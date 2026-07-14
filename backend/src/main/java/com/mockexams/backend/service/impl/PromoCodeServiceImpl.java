package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.AdminPromoCodeCDO;
import com.mockexams.backend.dto.PromoCodeCDO;
import com.mockexams.backend.dto.PromoCodeRDO;
import com.mockexams.backend.dto.PromoCodeUDO;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.entity.PromoCode;
import com.mockexams.backend.entity.PromoCodeRedemption;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.PromoCodeMapper;
import com.mockexams.backend.repository.PromoCodeRedemptionRepository;
import com.mockexams.backend.repository.PromoCodeRepository;
import com.mockexams.backend.service.PromoApplication;
import com.mockexams.backend.service.PromoCodeService;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.users.entity.User;

@Service
class PromoCodeServiceImpl implements PromoCodeService {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
	private static final String DEFAULT_CURRENCY = "UZS";

	private final PromoCodeRepository promoCodeRepository;
	private final PromoCodeRedemptionRepository redemptionRepository;
	private final TeacherProfileRepository teacherProfileRepository;
	private final PromoCodeMapper mapper;

	PromoCodeServiceImpl(
			PromoCodeRepository promoCodeRepository,
			PromoCodeRedemptionRepository redemptionRepository,
			TeacherProfileRepository teacherProfileRepository,
			PromoCodeMapper mapper
	) {
		this.promoCodeRepository = promoCodeRepository;
		this.redemptionRepository = redemptionRepository;
		this.teacherProfileRepository = teacherProfileRepository;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public PromoCodeRDO create(User teacher, PromoCodeCDO command) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		return mapper.toRDO(createCode(
				profile,
				command.code(),
				command.discountPercent(),
				command.discountAmount(),
				command.currency(),
				command.startsAt(),
				command.expiresAt(),
				command.usageLimit(),
				command.isActive()));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PromoCodeRDO> myCodes(User teacher, Boolean active, Pageable pageable) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		Page<PromoCode> codes = active == null
				? promoCodeRepository.findByTeacherProfile_Id(profile.getId(), pageable)
				: promoCodeRepository.findByTeacherProfile_IdAndActive(profile.getId(), active, pageable);
		return codes.map(mapper::toRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public PromoCodeRDO myCode(User teacher, Long promoCodeId) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		return mapper.toRDO(requireOwnCode(profile, promoCodeId));
	}

	@Override
	@Transactional
	public PromoCodeRDO update(User teacher, Long promoCodeId, PromoCodeUDO command) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		return mapper.toRDO(applyUpdate(requireOwnCode(profile, promoCodeId), command));
	}

	@Override
	@Transactional
	public PromoCodeRDO adminCreate(AdminPromoCodeCDO command) {
		TeacherProfile profile = command.teacherProfileId() == null
				? null
				: teacherProfileRepository.findById(command.teacherProfileId())
						.orElseThrow(this::teacherProfileMissing);
		return mapper.toRDO(createCode(
				profile,
				command.code(),
				command.discountPercent(),
				command.discountAmount(),
				command.currency(),
				command.startsAt(),
				command.expiresAt(),
				command.usageLimit(),
				command.isActive()));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PromoCodeRDO> adminCodes(Long teacherProfileId, Boolean active, Pageable pageable) {
		return promoCodeRepository.adminList(teacherProfileId, active, pageable).map(mapper::toRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public PromoCodeRDO adminCode(Long promoCodeId) {
		return mapper.toRDO(requireCode(promoCodeId));
	}

	@Override
	@Transactional
	public PromoCodeRDO adminUpdate(Long promoCodeId, PromoCodeUDO command) {
		return mapper.toRDO(applyUpdate(requireCode(promoCodeId), command));
	}

	@Override
	public PromoApplication validateForListing(MarketplaceListing listing, String rawCode) {
		String normalized = PromoCode.normalize(rawCode);
		if (normalized == null || normalized.isBlank()) {
			throw promoInvalid();
		}
		PromoCode promoCode = promoCodeRepository.findByCodeNormalized(normalized).orElseThrow(this::promoInvalid);
		OffsetDateTime now = now();
		if (!promoCode.isActive() || !promoCode.hasStartedAt(now)) {
			throw new BusinessException(
					"PROMO_CODE_INACTIVE",
					"Promo code is not currently active.",
					HttpStatus.CONFLICT);
		}
		if (promoCode.isExpiredAt(now)) {
			throw new BusinessException("PROMO_CODE_EXPIRED", "Promo code has expired.", HttpStatus.CONFLICT);
		}
		if (promoCode.isExhausted()) {
			throw promoExhausted();
		}
		if (promoCode.getTeacherProfile() != null
				&& (listing.getTeacherProfile() == null
						|| !promoCode.getTeacherProfile().getId().equals(listing.getTeacherProfile().getId()))) {
			throw new BusinessException(
					"PROMO_CODE_WRONG_OWNER",
					"Promo code does not apply to this teacher's listings.",
					HttpStatus.CONFLICT);
		}
		if (promoCode.getCurrency() != null && !promoCode.getCurrency().equalsIgnoreCase(listing.getCurrency())) {
			throw new BusinessException(
					"PROMO_CODE_CURRENCY_MISMATCH",
					"Promo code currency does not match the listing currency.",
					HttpStatus.CONFLICT);
		}
		BigDecimal originalAmount = money(listing.getPriceAmount());
		BigDecimal discountAmount = discountFor(promoCode, originalAmount);
		return new PromoApplication(
				promoCode.getId(),
				promoCode.getCode(),
				originalAmount,
				discountAmount,
				originalAmount.subtract(discountAmount));
	}

	@Override
	@Transactional
	public void redeemForPayment(Long promoCodeId, User student, Payment payment) {
		if (redemptionRepository.findByPromoCode_IdAndPaymentId(promoCodeId, payment.getId()).isPresent()) {
			return;
		}
		if (promoCodeRepository.consumeUsage(promoCodeId, now()) == 0) {
			throw promoExhausted();
		}
		redemptionRepository.save(PromoCodeRedemption.forPayment(
				promoCodeRepository.getReferenceById(promoCodeId),
				student.getId(),
				payment.getId(),
				payment.getPromoDiscountAmount(),
				payment.getCurrency(),
				now()));
	}

	@Override
	@Transactional
	public void releaseForPayment(Long promoCodeId, Payment payment) {
		if (promoCodeId == null || payment == null || payment.getId() == null) {
			return;
		}
		if (redemptionRepository.deleteByPromoCode_IdAndPaymentId(promoCodeId, payment.getId()) == 0) {
			// Already released (e.g. a duplicate failed/cancelled webhook); usage was not re-consumed.
			return;
		}
		promoCodeRepository.releaseUsage(promoCodeId, now());
	}

	private PromoCode createCode(
			TeacherProfile profile,
			String rawCode,
			BigDecimal discountPercent,
			BigDecimal discountAmount,
			String currency,
			OffsetDateTime startsAt,
			OffsetDateTime expiresAt,
			Integer usageLimit,
			Boolean isActive
	) {
		String displayCode = rawCode.trim();
		requireDiscountShape(discountPercent, discountAmount, currency);
		requireWindow(startsAt, expiresAt);
		if (promoCodeRepository.existsByCodeNormalized(PromoCode.normalize(displayCode))) {
			throw promoDuplicate();
		}
		PromoCode promoCode = PromoCode.create(
				profile,
				displayCode,
				scaledPercent(discountPercent),
				discountAmount == null ? null : money(discountAmount),
				discountAmount == null ? null : normalizedCurrencyOrDefault(currency),
				startsAt,
				expiresAt,
				usageLimit,
				isActive == null || isActive);
		try {
			return promoCodeRepository.saveAndFlush(promoCode);
		}
		catch (DataIntegrityViolationException ex) {
			throw promoDuplicate();
		}
	}

	private PromoCode applyUpdate(PromoCode promoCode, PromoCodeUDO command) {
		if (command.discountPercent() != null || command.discountAmount() != null) {
			requireDiscountShape(command.discountPercent(), command.discountAmount(), command.currency());
			promoCode.updateDiscount(
					scaledPercent(command.discountPercent()),
					command.discountAmount() == null ? null : money(command.discountAmount()),
					command.discountAmount() == null ? null : normalizedCurrencyOrDefault(command.currency()));
		}
		else if (command.currency() != null) {
			if (!promoCode.isPercentDiscount()) {
				promoCode.updateDiscount(null, promoCode.getDiscountAmount(), normalizedCurrencyOrDefault(command.currency()));
			}
			else {
				throw validationFailed("Currency applies to fixed-amount discounts only.");
			}
		}
		if (command.startsAt() != null || command.expiresAt() != null) {
			OffsetDateTime startsAt = command.startsAt() == null ? promoCode.getStartsAt() : command.startsAt();
			OffsetDateTime expiresAt = command.expiresAt() == null ? promoCode.getExpiresAt() : command.expiresAt();
			requireWindow(startsAt, expiresAt);
			promoCode.updateWindow(startsAt, expiresAt);
		}
		if (command.usageLimit() != null) {
			if (command.usageLimit() < promoCode.getUsedCount()) {
				throw validationFailed("Usage limit cannot be below the current used count.");
			}
			promoCode.updateUsageLimit(command.usageLimit());
		}
		if (command.isActive() != null) {
			promoCode.updateActive(command.isActive());
		}
		return promoCode;
	}

	private void requireDiscountShape(BigDecimal discountPercent, BigDecimal discountAmount, String currency) {
		if ((discountPercent == null) == (discountAmount == null)) {
			throw validationFailed("Exactly one of discountPercent or discountAmount is required.");
		}
		if (discountPercent != null && currency != null) {
			throw validationFailed("Currency applies to fixed-amount discounts only.");
		}
	}

	private void requireWindow(OffsetDateTime startsAt, OffsetDateTime expiresAt) {
		if (startsAt != null && expiresAt != null && !expiresAt.isAfter(startsAt)) {
			throw validationFailed("Promo code expiration must be after its start.");
		}
	}

	private BigDecimal discountFor(PromoCode promoCode, BigDecimal originalAmount) {
		if (promoCode.isPercentDiscount()) {
			return originalAmount.multiply(promoCode.getDiscountPercent())
					.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
		}
		return promoCode.getDiscountAmount().min(originalAmount);
	}

	private TeacherProfile requireTeacherProfile(User teacher) {
		if (teacher == null || teacher.getId() == null) {
			throw teacherProfileMissing();
		}
		return teacherProfileRepository.findByUserId(teacher.getId()).orElseThrow(this::teacherProfileMissing);
	}

	private PromoCode requireOwnCode(TeacherProfile profile, Long promoCodeId) {
		return promoCodeRepository.findByIdAndTeacherProfile_Id(promoCodeId, profile.getId())
				.orElseThrow(this::promoNotFound);
	}

	private PromoCode requireCode(Long promoCodeId) {
		return promoCodeRepository.findById(promoCodeId).orElseThrow(this::promoNotFound);
	}

	private BigDecimal scaledPercent(BigDecimal percent) {
		return percent == null ? null : percent.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private String normalizedCurrencyOrDefault(String currency) {
		if (currency == null || currency.isBlank()) {
			return DEFAULT_CURRENCY;
		}
		String normalized = currency.trim().toUpperCase(Locale.ROOT);
		if (normalized.length() != 3) {
			throw validationFailed("Currency is invalid.");
		}
		return normalized;
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private BusinessException promoInvalid() {
		return new BusinessException("PROMO_CODE_INVALID", "Promo code is invalid.", HttpStatus.NOT_FOUND);
	}

	private BusinessException promoNotFound() {
		return new BusinessException("PROMO_CODE_NOT_FOUND", "Promo code was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException promoDuplicate() {
		return new BusinessException("PROMO_CODE_DUPLICATE", "Promo code already exists.", HttpStatus.CONFLICT);
	}

	private BusinessException promoExhausted() {
		return new BusinessException(
				"PROMO_CODE_EXHAUSTED",
				"Promo code usage limit has been reached.",
				HttpStatus.CONFLICT);
	}

	private BusinessException teacherProfileMissing() {
		return new BusinessException("TEACHER_PROFILE_NOT_FOUND", "Teacher profile was not found.", HttpStatus.NOT_FOUND);
	}
}
