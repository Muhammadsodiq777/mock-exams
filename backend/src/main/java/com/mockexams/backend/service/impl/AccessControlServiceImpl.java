package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.AccessStatusRDO;
import com.mockexams.backend.dto.MarketplaceListingItemRDO;
import com.mockexams.backend.dto.PurchaseRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.FileMetadata;
import com.mockexams.backend.entity.FilePurpose;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.MarketplaceListingItem;
import com.mockexams.backend.entity.MarketplaceListingTypes;
import com.mockexams.backend.entity.Purchase;
import com.mockexams.backend.entity.PurchaseSources;
import com.mockexams.backend.entity.PurchaseStatuses;
import com.mockexams.backend.entity.Subscription;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.MarketplaceListingMapper;
import com.mockexams.backend.mapper.PurchaseMapper;
import com.mockexams.backend.repository.MarketplaceListingItemRepository;
import com.mockexams.backend.repository.MarketplaceListingRepository;
import com.mockexams.backend.repository.PurchaseRepository;
import com.mockexams.backend.repository.SubscriptionRepository;
import com.mockexams.backend.service.AccessControlService;
import com.mockexams.backend.service.AccessGrantRequest;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

@Service
class AccessControlServiceImpl implements AccessControlService {

	private final MarketplaceListingRepository listingRepository;
	private final MarketplaceListingItemRepository itemRepository;
	private final PurchaseRepository purchaseRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final UserRepository userRepository;
	private final JdbcTemplate jdbcTemplate;
	private final MarketplaceListingMapper listingMapper;
	private final PurchaseMapper purchaseMapper;

	AccessControlServiceImpl(
			MarketplaceListingRepository listingRepository,
			MarketplaceListingItemRepository itemRepository,
			PurchaseRepository purchaseRepository,
			SubscriptionRepository subscriptionRepository,
			UserRepository userRepository,
			JdbcTemplate jdbcTemplate,
			MarketplaceListingMapper listingMapper,
			PurchaseMapper purchaseMapper
	) {
		this.listingRepository = listingRepository;
		this.itemRepository = itemRepository;
		this.purchaseRepository = purchaseRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.userRepository = userRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.listingMapper = listingMapper;
		this.purchaseMapper = purchaseMapper;
	}

	@Override
	@Transactional
	public PurchaseRDO grantFreeListingAccess(User student, Long listingId) {
		MarketplaceListing listing = requireListing(listingId);
		requireCurrentlyAccessibleListing(listing);
		if (!isFree(listing)) {
			throw paymentRequired();
		}
		return grantAccess(
				student.getId(),
				listing,
				new AccessGrantRequest(PurchaseSources.FREE_LISTING, "listing:" + listingId, null, null));
	}

	@Override
	@Transactional
	public PurchaseRDO grantInternalAccess(Long studentUserId, Long listingId, AccessGrantRequest request) {
		MarketplaceListing listing = requireListing(listingId);
		requireCurrentlyAccessibleListing(listing);
		return grantAccess(studentUserId, listing, normalizeGrantRequest(request));
	}

	@Override
	@Transactional(readOnly = true)
	public AccessStatusRDO listingAccessStatus(User student, Long listingId) {
		MarketplaceListing listing = requireListing(listingId);
		return accessStatus(student, listing, now());
	}

	@Override
	@Transactional
	public void requireListingAccess(User student, Long listingId) {
		MarketplaceListing listing = requireListing(listingId);
		requireAccess(student, listing, now());
	}

	@Override
	@Transactional
	public List<MarketplaceListingItemRDO> accessibleListingItems(User student, Long listingId) {
		MarketplaceListing listing = requireListing(listingId);
		requireAccess(student, listing, now());
		List<MarketplaceListingItem> items = itemRepository.findByListingIdOrderByItemOrderAscIdAsc(listing.getId());
		return listingMapper.toItemRDOs(items);
	}

	@Override
	@Transactional
	public void requireTestAccess(User student, CatalogTest test) {
		if (Boolean.TRUE.equals(test.isFree())) {
			grantFirstActiveFreeListingIfPresent(student, test.getId());
			return;
		}
		List<MarketplaceListing> listings = listingRepository.findByReferencedTestId(test.getId());
		if (listings.isEmpty()) {
			throw paymentRequired();
		}
		AccessFailure strongestFailure = null;
		OffsetDateTime checkedAt = now();
		for (MarketplaceListing listing : listings) {
			try {
				requireAccess(student, listing, checkedAt);
				return;
			}
			catch (BusinessException ex) {
				strongestFailure = AccessFailure.strongest(strongestFailure, ex);
			}
		}
		throw strongestFailure == null ? paymentRequired() : strongestFailure.exception();
	}

	@Override
	@Transactional
	public void requireFileDownloadAccess(User requester, FileMetadata file) {
		if (isOwner(requester, file) || !FilePurpose.COURSE_MATERIAL.equals(file.getPurpose())) {
			return;
		}
		List<MarketplaceListingItem> items = itemRepository.findByFile_IdOrderByItemOrderAscIdAsc(file.getId());
		if (items.isEmpty()) {
			return;
		}
		AccessFailure strongestFailure = null;
		OffsetDateTime checkedAt = now();
		for (MarketplaceListingItem item : items) {
			try {
				requireAccess(requester, item.getListing(), checkedAt);
				return;
			}
			catch (BusinessException ex) {
				strongestFailure = AccessFailure.strongest(strongestFailure, ex);
			}
		}
		throw strongestFailure == null ? paymentRequired() : strongestFailure.exception();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PurchaseRDO> purchaseHistory(User student) {
		return purchaseRepository.findByStudent_IdOrderByPurchasedAtDescIdDesc(student.getId()).stream()
				.map(purchase -> purchaseMapper.toRDO(
						purchase,
						paymentStatus(purchase.getPaymentId()),
						accessStatus(student, purchase.getListing(), now())))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PurchaseRDO purchaseDetail(User student, Long purchaseId) {
		Purchase purchase = purchaseRepository.findByIdAndStudent_Id(purchaseId, student.getId())
				.orElseThrow(this::purchaseNotFound);
		return purchaseMapper.toRDO(
				purchase,
				paymentStatus(purchase.getPaymentId()),
				accessStatus(student, purchase.getListing(), now()));
	}

	private PurchaseRDO grantAccess(Long studentUserId, MarketplaceListing listing, AccessGrantRequest request) {
		User student = userRepository.findById(studentUserId).orElseThrow(this::studentNotFound);
		Purchase purchase = purchaseRepository.findByStudent_IdAndListing_Id(student.getId(), listing.getId())
				.orElse(null);
		if (purchase == null) {
			purchase = purchaseRepository.save(Purchase.grant(
					student,
					listing,
					request.sourceType(),
					request.sourceReference(),
					request.sourceMetadataJson(),
					request.accessExpiresAt(),
					request.paymentId(),
					request.amountPaid(),
					now()));
		}
		return purchaseMapper.toRDO(
				purchase,
				paymentStatus(purchase.getPaymentId()),
				accessStatus(student, listing, now()));
	}

	private void grantFirstActiveFreeListingIfPresent(User student, Long testId) {
		listingRepository.findByReferencedTestId(testId).stream()
				.filter(this::isCurrentlyAccessibleListing)
				.filter(this::isFree)
				.findFirst()
				.ifPresent(listing -> grantAccess(
						student.getId(),
						listing,
						new AccessGrantRequest(PurchaseSources.FREE_LISTING, "listing:" + listing.getId(), null, null)));
	}

	private void requireAccess(User student, MarketplaceListing listing, OffsetDateTime checkedAt) {
		requireCurrentlyAccessibleListing(listing);
		if (isFree(listing)) {
			grantAccess(
					student.getId(),
					listing,
					new AccessGrantRequest(PurchaseSources.FREE_LISTING, "listing:" + listing.getId(), null, null));
			return;
		}
		Purchase purchase = purchaseRepository.findGrantForAccessCheck(student.getId(), listing.getId()).orElse(null);
		if (purchase != null) {
			if (purchase.grantsAccessAt(checkedAt)) {
				return;
			}
			if (purchase.isExpiredAt(checkedAt)) {
				throw accessExpired();
			}
			if (PurchaseStatuses.REVOKED.equals(purchase.getStatus())) {
				throw listingNotAccessible();
			}
		}
		List<Subscription> subscriptions = subscriptionRepository.findByStudent_IdAndListing_Id(
				student.getId(),
				listing.getId());
		if (subscriptions.stream().anyMatch(subscription -> subscription.grantsAccessAt(checkedAt))) {
			return;
		}
		if (hasExpiredAccess(student.getId(), listing.getId(), checkedAt, purchase, subscriptions)) {
			throw accessExpired();
		}
		throw paymentRequired();
	}

	private AccessStatusRDO accessStatus(User student, MarketplaceListing listing, OffsetDateTime checkedAt) {
		if (!isCurrentlyAccessibleListing(listing)) {
			return new AccessStatusRDO(listing.getId(), false, "not_accessible", "LISTING_NOT_ACCESSIBLE", null, null);
		}
		if (student == null || student.getId() == null) {
			return new AccessStatusRDO(
					listing.getId(),
					isFree(listing),
					isFree(listing) ? "accessible" : "payment_required",
					isFree(listing) ? null : "PAYMENT_REQUIRED",
					null,
					null);
		}
		if (isFree(listing)) {
			return new AccessStatusRDO(listing.getId(), true, "accessible", null, null, null);
		}
		Purchase purchase = purchaseRepository.findGrantForAccessCheck(student.getId(), listing.getId()).orElse(null);
		if (purchase != null) {
			if (purchase.grantsAccessAt(checkedAt)) {
				return new AccessStatusRDO(
						listing.getId(),
						true,
						"accessible",
						null,
						purchase.getId(),
						purchase.getAccessExpiresAt());
			}
			if (purchase.isExpiredAt(checkedAt)) {
				return new AccessStatusRDO(
						listing.getId(),
						false,
						"expired",
						"ACCESS_EXPIRED",
						purchase.getId(),
						purchase.getAccessExpiresAt());
			}
			if (PurchaseStatuses.REVOKED.equals(purchase.getStatus())) {
				return new AccessStatusRDO(
						listing.getId(),
						false,
						"not_accessible",
						"LISTING_NOT_ACCESSIBLE",
						purchase.getId(),
						purchase.getAccessExpiresAt());
			}
		}
		List<Subscription> subscriptions = subscriptionRepository.findByStudent_IdAndListing_Id(
				student.getId(),
				listing.getId());
		return subscriptions.stream()
				.filter(subscription -> subscription.grantsAccessAt(checkedAt))
				.findFirst()
				.map(subscription -> new AccessStatusRDO(
						listing.getId(),
						true,
						"accessible",
						null,
						null,
						subscription.getEndsAt()))
				.orElseGet(() -> {
					if (hasExpiredAccess(student.getId(), listing.getId(), checkedAt, purchase, subscriptions)) {
						return new AccessStatusRDO(
								listing.getId(),
								false,
								"expired",
								"ACCESS_EXPIRED",
								purchase == null ? null : purchase.getId(),
								purchase == null ? null : purchase.getAccessExpiresAt());
					}
					return new AccessStatusRDO(
							listing.getId(),
							false,
							"payment_required",
							"PAYMENT_REQUIRED",
							null,
							null);
				});
	}

	private boolean hasExpiredAccess(
			Long studentId,
			Long listingId,
			OffsetDateTime checkedAt,
			Purchase purchase,
			List<Subscription> subscriptions
	) {
		if (purchase != null && purchase.isExpiredAt(checkedAt)) {
			return true;
		}
		if (subscriptions.stream().anyMatch(subscription -> subscription.isExpiredAt(checkedAt))) {
			return true;
		}
		return purchaseRepository.hasExpiredGrant(studentId, listingId, checkedAt)
				|| subscriptionRepository.hasExpiredActiveSubscription(studentId, listingId, checkedAt);
	}

	private AccessGrantRequest normalizeGrantRequest(AccessGrantRequest request) {
		if (request == null || isBlank(request.sourceType())) {
			throw validationFailed("Access grant source type is required.");
		}
		String sourceType = request.sourceType().trim().toLowerCase();
		if (!PurchaseSources.isSupported(sourceType)) {
			throw validationFailed("Access grant source type is unsupported.");
		}
		String sourceReference = trimToNull(request.sourceReference());
		String sourceMetadataJson = trimToNull(request.sourceMetadataJson());
		if (request.accessExpiresAt() != null && !request.accessExpiresAt().isAfter(now())) {
			throw validationFailed("Access expiration must be in the future.");
		}
		if (request.paymentId() != null && request.paymentId() < 1) {
			throw validationFailed("Payment id must be positive.");
		}
		if (request.amountPaid() != null && request.amountPaid().signum() < 0) {
			throw validationFailed("Paid amount cannot be negative.");
		}
		return new AccessGrantRequest(
				sourceType,
				sourceReference,
				sourceMetadataJson,
				request.accessExpiresAt(),
				request.paymentId(),
				request.amountPaid());
	}

	private MarketplaceListing requireListing(Long listingId) {
		return listingRepository.findById(listingId).orElseThrow(this::listingNotFound);
	}

	private void requireCurrentlyAccessibleListing(MarketplaceListing listing) {
		if (!isCurrentlyAccessibleListing(listing)) {
			throw listingNotAccessible();
		}
	}

	private boolean isCurrentlyAccessibleListing(MarketplaceListing listing) {
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
			return isPublished(listing.getTest());
		}
		List<MarketplaceListingItem> items = itemRepository.findByListingIdOrderByItemOrderAscIdAsc(listing.getId());
		if (items.isEmpty()) {
			return false;
		}
		return items.stream().allMatch(this::isValidListingItem);
	}

	private boolean isValidListingItem(MarketplaceListingItem item) {
		if (item.getTest() != null) {
			return isPublished(item.getTest());
		}
		return item.getFile() != null;
	}

	private boolean isPublished(CatalogTest test) {
		return test != null
				&& TestVisibilities.PUBLISHED.equals(test.getVisibility())
				&& test.getDeletedAt() == null;
	}

	private String paymentStatus(Long paymentId) {
		if (paymentId == null) {
			return null;
		}
		try {
			return jdbcTemplate.queryForObject("select status from payments where id = ?", String.class, paymentId);
		}
		catch (DataAccessException ex) {
			return null;
		}
	}

	private boolean isOwner(User requester, FileMetadata file) {
		return requester != null
				&& requester.getId() != null
				&& file.getOwner() != null
				&& Objects.equals(requester.getId(), file.getOwner().getId());
	}

	private boolean isFree(MarketplaceListing listing) {
		return listing.getPriceAmount() == null || BigDecimal.ZERO.compareTo(listing.getPriceAmount()) == 0;
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
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

	private BusinessException studentNotFound() {
		return new BusinessException("USER_NOT_FOUND", "Student was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException listingNotFound() {
		return new BusinessException("LISTING_NOT_FOUND", "Marketplace listing was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException purchaseNotFound() {
		return new BusinessException("PURCHASE_NOT_FOUND", "Purchase was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException paymentRequired() {
		return new BusinessException("PAYMENT_REQUIRED", "Marketplace purchase is required.", HttpStatus.PAYMENT_REQUIRED);
	}

	private BusinessException accessExpired() {
		return new BusinessException("ACCESS_EXPIRED", "Marketplace access has expired.", HttpStatus.FORBIDDEN);
	}

	private BusinessException listingNotAccessible() {
		return new BusinessException(
				"LISTING_NOT_ACCESSIBLE",
				"Marketplace listing is not currently accessible.",
				HttpStatus.FORBIDDEN);
	}

	private record AccessFailure(BusinessException exception, int priority) {

		private static AccessFailure strongest(AccessFailure current, BusinessException exception) {
			AccessFailure candidate = new AccessFailure(exception, priority(exception));
			if (current == null || candidate.priority() > current.priority()) {
				return candidate;
			}
			return current;
		}

		private static int priority(BusinessException exception) {
			return switch (exception.getCode()) {
				case "LISTING_NOT_ACCESSIBLE" -> 3;
				case "ACCESS_EXPIRED" -> 2;
				case "PAYMENT_REQUIRED" -> 1;
				default -> 0;
			};
		}
	}
}
