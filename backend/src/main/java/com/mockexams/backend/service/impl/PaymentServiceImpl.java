package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.PaymentCDO;
import com.mockexams.backend.dto.PaymentRDO;
import com.mockexams.backend.dto.PaymentWebhookCDO;
import com.mockexams.backend.dto.PromoQuoteRDO;
import com.mockexams.backend.dto.PurchaseRDO;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.MarketplaceListingItem;
import com.mockexams.backend.entity.MarketplaceListingTypes;
import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.entity.PaymentStatuses;
import com.mockexams.backend.entity.PaymentTargetTypes;
import com.mockexams.backend.entity.PurchaseSources;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.PaymentMapper;
import com.mockexams.backend.repository.MarketplaceListingItemRepository;
import com.mockexams.backend.repository.MarketplaceListingRepository;
import com.mockexams.backend.repository.PaymentRepository;
import com.mockexams.backend.service.AccessControlService;
import com.mockexams.backend.service.AccessGrantRequest;
import com.mockexams.backend.service.PaymentService;
import com.mockexams.backend.service.PromoApplication;
import com.mockexams.backend.service.PromoCodeService;
import com.mockexams.backend.service.TeacherWalletService;
import com.mockexams.backend.service.payments.PaymentProviderClient;
import com.mockexams.backend.service.payments.PaymentProviderCreateRequest;
import com.mockexams.backend.service.payments.PaymentProviderCreateResult;
import com.mockexams.backend.service.schedules.LiveSpeakingPaymentTarget;
import com.mockexams.backend.service.schedules.SpeakingScheduleService;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.users.entity.User;

@Service
class PaymentServiceImpl implements PaymentService {

	private final PaymentRepository paymentRepository;
	private final MarketplaceListingRepository listingRepository;
	private final MarketplaceListingItemRepository itemRepository;
	private final PaymentProviderClient providerClient;
	private final AccessControlService accessControlService;
	private final TeacherWalletService teacherWalletService;
	private final PromoCodeService promoCodeService;
	private final SpeakingScheduleService speakingScheduleService;
	private final PaymentMapper paymentMapper;
	private final ObjectMapper objectMapper;

	PaymentServiceImpl(
			PaymentRepository paymentRepository,
			MarketplaceListingRepository listingRepository,
			MarketplaceListingItemRepository itemRepository,
			PaymentProviderClient providerClient,
			AccessControlService accessControlService,
			TeacherWalletService teacherWalletService,
			PromoCodeService promoCodeService,
			SpeakingScheduleService speakingScheduleService,
			PaymentMapper paymentMapper,
			ObjectMapper objectMapper
	) {
		this.paymentRepository = paymentRepository;
		this.listingRepository = listingRepository;
		this.itemRepository = itemRepository;
		this.providerClient = providerClient;
		this.accessControlService = accessControlService;
		this.teacherWalletService = teacherWalletService;
		this.promoCodeService = promoCodeService;
		this.speakingScheduleService = speakingScheduleService;
		this.paymentMapper = paymentMapper;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public PaymentRDO createPayment(User student, PaymentCDO request) {
		String targetType = normalizeTargetType(request.targetType());
		if (PaymentTargetTypes.LIVE_SPEAKING_BOOKING.equals(targetType)) {
			return createLiveSpeakingPayment(student, request);
		}
		if (PaymentTargetTypes.SUBSCRIPTION.equals(targetType)) {
			throw targetUnsupported("Subscription payments are not implemented yet.");
		}
		if (!PaymentTargetTypes.MARKETPLACE_LISTING.equals(targetType)) {
			throw validationFailed("Unsupported payment target type.");
		}

		MarketplaceListing listing = requirePayableListing(request.listingId());
		requireNotAlreadyPurchased(student, listing);
		String promoCode = trimToNull(request.promoCode());
		PromoApplication promo = promoCode == null ? null : promoCodeService.validateForListing(listing, promoCode);
		Payment payment = Payment.pendingMarketplaceListing(
				student,
				listing,
				providerClient.provider(),
				metadataFor(student, listing),
				now());
		if (promo != null) {
			payment.applyPromoDiscount(promo.promoCodeId(), promo.displayCode(), promo.discountAmount());
		}
		payment = paymentRepository.saveAndFlush(payment);
		if (promo != null) {
			promoCodeService.redeemForPayment(promo.promoCodeId(), student, payment);
		}
		if (payment.getAmount().compareTo(BigDecimal.ZERO) == 0) {
			payment.markPaidWithoutProvider(now());
			dispatchPaidConfirmation(payment);
			payment.markConfirmationDispatched(now());
			return paymentMapper.toRDO(payment);
		}

		PaymentProviderCreateResult providerResult = providerClient.createPayment(new PaymentProviderCreateRequest(
				payment.getId(),
				student.getId(),
				payment.getAmount(),
				payment.getCurrency(),
				listing.getTitle(),
				trimToNull(request.returnUrl())));
		payment.attachProviderPayment(providerResult.providerPaymentId(), providerResult.checkoutUrl());
		return paymentMapper.toRDO(payment);
	}

	private PaymentRDO createLiveSpeakingPayment(User student, PaymentCDO request) {
		LiveSpeakingPaymentTarget target = speakingScheduleService.requirePaymentTarget(student, request.liveSpeakingBookingId());
		Payment existing = paymentRepository.findFirstByTargetLiveSpeakingBookingIdAndStatusInOrderByCreatedAtDescIdDesc(
						target.bookingId(),
						List.of(PaymentStatuses.PENDING, PaymentStatuses.PAID))
				.orElse(null);
		if (existing != null) {
			return paymentMapper.toRDO(existing);
		}

		Payment payment = Payment.pendingLiveSpeakingBooking(
				student,
				target.bookingId(),
				target.amount(),
				target.currency(),
				providerClient.provider(),
				metadataFor(student, target),
				now());
		payment = paymentRepository.saveAndFlush(payment);
		speakingScheduleService.attachPayment(student, target.bookingId(), payment);
		if (payment.getAmount().compareTo(BigDecimal.ZERO) == 0) {
			payment.markPaidWithoutProvider(now());
			dispatchPaidConfirmation(payment);
			payment.markConfirmationDispatched(now());
			return paymentMapper.toRDO(payment);
		}

		PaymentProviderCreateResult providerResult = providerClient.createPayment(new PaymentProviderCreateRequest(
				payment.getId(),
				student.getId(),
				payment.getAmount(),
				payment.getCurrency(),
				target.description(),
				trimToNull(request.returnUrl())));
		payment.attachProviderPayment(providerResult.providerPaymentId(), providerResult.checkoutUrl());
		return paymentMapper.toRDO(payment);
	}

	@Override
	@Transactional(readOnly = true)
	public PromoQuoteRDO promoQuote(User student, Long listingId, String promoCode) {
		String normalizedCode = trimToNull(promoCode);
		if (normalizedCode == null) {
			throw validationFailed("Promo code is required.");
		}
		MarketplaceListing listing;
		try {
			listing = requirePayableListing(listingId);
			requireNotAlreadyPurchased(student, listing);
		}
		catch (BusinessException ex) {
			return new PromoQuoteRDO(listingId, normalizedCode, false, ex.getCode(), null, null, null, null);
		}
		try {
			PromoApplication promo = promoCodeService.validateForListing(listing, normalizedCode);
			return new PromoQuoteRDO(
					listing.getId(),
					promo.displayCode(),
					true,
					null,
					promo.originalAmount(),
					promo.discountAmount(),
					promo.finalAmount(),
					listing.getCurrency());
		}
		catch (BusinessException ex) {
			return new PromoQuoteRDO(
					listing.getId(),
					normalizedCode,
					false,
					ex.getCode(),
					listing.getPriceAmount(),
					null,
					listing.getPriceAmount(),
					listing.getCurrency());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public PaymentRDO studentPayment(User student, Long paymentId) {
		return paymentMapper.toRDO(paymentRepository.findByIdAndStudent_Id(paymentId, student.getId())
				.orElseThrow(this::paymentNotFound));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PaymentRDO> studentPayments(User student) {
		return paymentRepository.findByStudent_IdOrderByCreatedAtDescIdDesc(student.getId()).stream()
				.map(paymentMapper::toRDO)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PaymentRDO adminPayment(Long paymentId) {
		return paymentMapper.toRDO(paymentRepository.findById(paymentId).orElseThrow(this::paymentNotFound));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PaymentRDO> adminPayments(String status, Pageable pageable) {
		String normalizedStatus = trimToNull(status);
		if (normalizedStatus == null) {
			return paymentRepository.findAll(pageable).map(paymentMapper::toRDO);
		}
		normalizedStatus = normalizedStatus.toLowerCase(Locale.ROOT);
		if (!PaymentStatuses.isSupported(normalizedStatus)) {
			throw validationFailed("Unsupported payment status filter.");
		}
		return paymentRepository.findByStatus(normalizedStatus, pageable).map(paymentMapper::toRDO);
	}

	@Override
	@Transactional
	public PaymentRDO handleProviderWebhook(String rawBody, String signature) {
		if (!providerClient.verifyWebhookSignature(rawBody, signature)) {
			throw new BusinessException(
					"PAYMENT_SIGNATURE_INVALID",
					"Payment webhook signature is invalid.",
					HttpStatus.UNAUTHORIZED);
		}

		PaymentWebhookCDO event = readWebhook(rawBody);
		String status = normalizeStatus(event.status());
		Payment payment = paymentRepository.findByProviderAndProviderPaymentId(
						providerClient.provider(),
						event.providerPaymentId())
				.orElseThrow(this::paymentNotFound);

		if (PaymentStatuses.PAID.equals(status) && payment.hasAmountMismatch(event.amount(), event.currency())) {
			payment.flagAmountMismatch(event.amount(), normalizeCurrency(event.currency()));
			return paymentMapper.toRDO(payment);
		}

		boolean wasPending = PaymentStatuses.PENDING.equals(payment.getStatus());
		payment.applyProviderEvent(
				trimToNull(event.eventId()),
				status,
				event.amount(),
				normalizeCurrency(event.currency()),
				effectiveOccurredAt(payment, event.occurredAt()),
				trimToNull(event.failureCode()),
				trimToNull(event.failureMessage()));

		if (PaymentStatuses.PAID.equals(payment.getStatus()) && payment.getConfirmationDispatchedAt() == null) {
			dispatchPaidConfirmation(payment);
			payment.markConfirmationDispatched(now());
		}
		if (wasPending && payment.getPromoCodeId() != null
				&& (PaymentStatuses.FAILED.equals(payment.getStatus()) || PaymentStatuses.CANCELLED.equals(payment.getStatus()))) {
			promoCodeService.releaseForPayment(payment.getPromoCodeId(), payment);
		}
		return paymentMapper.toRDO(payment);
	}

	private void dispatchPaidConfirmation(Payment payment) {
		if (PaymentTargetTypes.MARKETPLACE_LISTING.equals(payment.getTargetType())) {
			MarketplaceListing listing = payment.getTargetListing();
			PurchaseRDO purchase = accessControlService.grantInternalAccess(
					payment.getStudent().getId(),
					listing.getId(),
					new AccessGrantRequest(
							PurchaseSources.PAYMENT,
							"payment:" + payment.getId(),
							accessGrantMetadata(payment),
							null,
							payment.getId(),
							payment.getAmount()));
			teacherWalletService.recordMarketplacePurchaseEarning(purchase.id());
			return;
		}
		if (PaymentTargetTypes.LIVE_SPEAKING_BOOKING.equals(payment.getTargetType())) {
			if (speakingScheduleService.confirmPaidBooking(payment)) {
				teacherWalletService.recordLiveSpeakingBookingEarning(
						payment.getTargetLiveSpeakingBookingId(),
						payment.getId());
			}
			return;
		}
		throw targetUnsupported("Payment target is not implemented yet.");
	}

	private PaymentWebhookCDO readWebhook(String rawBody) {
		try {
			PaymentWebhookCDO event = objectMapper.readValue(rawBody, PaymentWebhookCDO.class);
			if (isBlank(event.providerPaymentId()) || isBlank(event.status()) || event.amount() == null
					|| isBlank(event.currency())) {
				throw validationFailed("Payment webhook payload is invalid.");
			}
			return event;
		}
		catch (JsonProcessingException ex) {
			throw validationFailed("Payment webhook payload is invalid.");
		}
	}

	private MarketplaceListing requirePayableListing(Long listingId) {
		if (listingId == null) {
			throw validationFailed("Marketplace listing id is required.");
		}
		MarketplaceListing listing = listingRepository.findById(listingId).orElseThrow(this::listingNotFound);
		if (!isCurrentlyPayable(listing)) {
			throw new BusinessException(
					"LISTING_NOT_ACCESSIBLE",
					"Marketplace listing is not currently payable.",
					HttpStatus.FORBIDDEN);
		}
		if (listing.getPriceAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw validationFailed("Free listings do not require a payment.");
		}
		return listing;
	}

	private void requireNotAlreadyPurchased(User student, MarketplaceListing listing) {
		if (accessControlService.listingAccessStatus(student, listing.getId()).accessible()) {
			throw alreadyPurchased();
		}
	}

	private boolean isCurrentlyPayable(MarketplaceListing listing) {
		if (!listing.isActive()
				|| listing.getTeacherProfile() == null
				|| !TeacherProfileStatuses.APPROVED.equals(listing.getTeacherProfile().getStatus())
				|| listing.getPriceAmount() == null
				|| listing.getPriceAmount().compareTo(BigDecimal.ZERO) < 0
				|| isBlank(listing.getTitle())
				|| !MarketplaceListingTypes.isSupported(listing.getListingType())) {
			return false;
		}
		if (MarketplaceListingTypes.isDirectTestListing(listing.getListingType())) {
			return listing.getTest() != null
					&& TestVisibilities.PUBLISHED.equals(listing.getTest().getVisibility())
					&& listing.getTest().getDeletedAt() == null;
		}
		List<MarketplaceListingItem> items = itemRepository.findByListingIdOrderByItemOrderAscIdAsc(listing.getId());
		return !items.isEmpty() && items.stream().allMatch(this::isValidListingItem);
	}

	private boolean isValidListingItem(MarketplaceListingItem item) {
		if (item.getTest() != null) {
			return TestVisibilities.PUBLISHED.equals(item.getTest().getVisibility())
					&& item.getTest().getDeletedAt() == null;
		}
		return item.getFile() != null;
	}

	private String metadataFor(User student, MarketplaceListing listing) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("targetType", PaymentTargetTypes.MARKETPLACE_LISTING);
		metadata.put("listingId", listing.getId());
		metadata.put("listingTitle", listing.getTitle());
		metadata.put("studentUserId", student.getId());
		return toJson(metadata);
	}

	private String metadataFor(User student, LiveSpeakingPaymentTarget target) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("targetType", PaymentTargetTypes.LIVE_SPEAKING_BOOKING);
		metadata.put("bookingId", target.bookingId());
		metadata.put("studentUserId", student.getId());
		return toJson(metadata);
	}

	private String accessGrantMetadata(Payment payment) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("provider", payment.getProvider());
		metadata.put("providerPaymentId", payment.getProviderPaymentId());
		metadata.put("targetType", payment.getTargetType());
		return toJson(metadata);
	}

	private String toJson(Map<String, Object> metadata) {
		try {
			return objectMapper.writeValueAsString(metadata);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Payment metadata serialization failed.", ex);
		}
	}

	private String normalizeTargetType(String targetType) {
		String normalized = trimToNull(targetType);
		if (normalized == null) {
			throw validationFailed("Payment target type is required.");
		}
		return normalized.toLowerCase(Locale.ROOT);
	}

	private String normalizeStatus(String status) {
		String normalized = trimToNull(status);
		if (normalized == null) {
			throw validationFailed("Payment status is required.");
		}
		normalized = normalized.toLowerCase(Locale.ROOT);
		if (!PaymentStatuses.isSupported(normalized)) {
			throw validationFailed("Unsupported payment status.");
		}
		return normalized;
	}

	private String normalizeCurrency(String currency) {
		String normalized = trimToNull(currency);
		if (normalized == null || normalized.length() != 3) {
			throw validationFailed("Payment currency is invalid.");
		}
		return normalized.toUpperCase(Locale.ROOT);
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private OffsetDateTime effectiveOccurredAt(Payment payment, OffsetDateTime providerOccurredAt) {
		OffsetDateTime occurredAt = providerOccurredAt == null ? now() : providerOccurredAt;
		if (occurredAt.isBefore(payment.getCreatedAt())) {
			return payment.getCreatedAt();
		}
		return occurredAt;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private BusinessException targetUnsupported(String message) {
		return new BusinessException("PAYMENT_TARGET_UNSUPPORTED", message, HttpStatus.CONFLICT);
	}

	private BusinessException listingNotFound() {
		return new BusinessException("LISTING_NOT_FOUND", "Marketplace listing was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException paymentNotFound() {
		return new BusinessException("PAYMENT_NOT_FOUND", "Payment was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException alreadyPurchased() {
		return new BusinessException(
				"ALREADY_PURCHASED",
				"Student already has granted access to this marketplace listing.",
				HttpStatus.CONFLICT);
	}
}
