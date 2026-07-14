package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.MarketplaceListingActivationRDO;
import com.mockexams.backend.dto.MarketplaceListingCDO;
import com.mockexams.backend.dto.MarketplaceListingDetailRDO;
import com.mockexams.backend.dto.MarketplaceListingItemCDO;
import com.mockexams.backend.dto.MarketplaceListingItemUDO;
import com.mockexams.backend.dto.MarketplaceListingSummaryRDO;
import com.mockexams.backend.dto.MarketplaceListingUDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.FileMetadata;
import com.mockexams.backend.entity.FilePurpose;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.MarketplaceListingItem;
import com.mockexams.backend.entity.MarketplaceListingTypes;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.MarketplaceListingMapper;
import com.mockexams.backend.repository.FileMetadataRepository;
import com.mockexams.backend.repository.MarketplaceListingItemRepository;
import com.mockexams.backend.repository.MarketplaceListingRepository;
import com.mockexams.backend.repository.MarketplaceListingSpecifications;
import com.mockexams.backend.repository.TestCatalogRepository;
import com.mockexams.backend.service.MarketplaceListingService;
import com.mockexams.backend.service.MarketplaceSearchCriteria;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.users.entity.User;

@Service
class MarketplaceListingServiceImpl implements MarketplaceListingService {

	private static final List<FilePurpose> LISTING_FILE_PURPOSES = List.of(
			FilePurpose.COURSE_MATERIAL,
			FilePurpose.DOCUMENT);

	private final MarketplaceListingRepository listingRepository;
	private final MarketplaceListingItemRepository itemRepository;
	private final TestCatalogRepository testRepository;
	private final FileMetadataRepository fileMetadataRepository;
	private final TeacherProfileRepository teacherProfileRepository;
	private final MarketplaceListingMapper mapper;

	MarketplaceListingServiceImpl(
			MarketplaceListingRepository listingRepository,
			MarketplaceListingItemRepository itemRepository,
			TestCatalogRepository testRepository,
			FileMetadataRepository fileMetadataRepository,
			TeacherProfileRepository teacherProfileRepository,
			MarketplaceListingMapper mapper
	) {
		this.listingRepository = listingRepository;
		this.itemRepository = itemRepository;
		this.testRepository = testRepository;
		this.fileMetadataRepository = fileMetadataRepository;
		this.teacherProfileRepository = teacherProfileRepository;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<MarketplaceListingSummaryRDO> listPublic(MarketplaceSearchCriteria criteria, Pageable pageable) {
		Page<MarketplaceListing> listings = listingRepository.findAll(
				MarketplaceListingSpecifications.publicMarketplace(criteria),
				pageable);
		return toSummaryPage(listings, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public MarketplaceListingDetailRDO publicDetail(Long listingId) {
		MarketplaceListing listing = listingRepository.findActivePublicById(listingId)
				.filter(this::isPubliclyDiscoverable)
				.orElseThrow(this::notFound);
		if (!isFree(listing)) {
			return mapper.toDetailRDO(listing, List.of());
		}
		return toDetail(listing);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<MarketplaceListingSummaryRDO> listTeacherListings(User teacher, Pageable pageable) {
		TeacherProfile profile = requireApprovedTeacher(teacher);
		Page<MarketplaceListing> listings = listingRepository.findAll(
				MarketplaceListingSpecifications.teacherOwned(profile.getId()),
				pageable);
		return toSummaryPage(listings, pageable);
	}

	@Override
	@Transactional
	public MarketplaceListingDetailRDO create(User teacher, MarketplaceListingCDO command) {
		TeacherProfile profile = requireApprovedTeacher(teacher);
		String listingType = normalizeListingType(command.listingType());
		CatalogTest test = resolveOwnedTest(teacher, profile, command.testId());
		validateDirectTestShape(listingType, test);
		MarketplaceListing listing = MarketplaceListing.draft(
				profile,
				test,
				requiredText(command.title(), "Title is required."),
				trimToNull(command.description()),
				listingType,
				nonNegativePrice(command.priceAmount()),
				currency(command.currency()),
				language(command.language()));
		return toDetail(listingRepository.save(listing));
	}

	@Override
	@Transactional
	public MarketplaceListingDetailRDO update(User teacher, Long listingId, MarketplaceListingUDO command) {
		TeacherProfile profile = requireApprovedTeacher(teacher);
		MarketplaceListing listing = requireOwnedListing(listingId, profile);
		String listingType = command.listingType() == null
				? listing.getListingType()
				: normalizeListingType(command.listingType());
		CatalogTest test = command.testId() == null
				? listing.getTest()
				: resolveOwnedTest(teacher, profile, command.testId());
		validateDirectTestShape(listingType, test);
		listing.update(
				test,
				command.title() == null ? listing.getTitle() : requiredText(command.title(), "Title is required."),
				command.description() == null ? listing.getDescription() : trimToNull(command.description()),
				listingType,
				command.priceAmount() == null ? listing.getPriceAmount() : nonNegativePrice(command.priceAmount()),
				command.currency() == null ? listing.getCurrency() : currency(command.currency()),
				command.language() == null ? listing.getLanguage() : language(command.language()));
		if (listing.isActive() && !activationReport(listing).ready()) {
			listing.deactivate();
		}
		return toDetail(listing);
	}

	@Override
	@Transactional(readOnly = true)
	public MarketplaceListingDetailRDO preview(User teacher, Long listingId) {
		TeacherProfile profile = requireApprovedTeacher(teacher);
		return toDetail(requireOwnedListing(listingId, profile));
	}

	@Override
	@Transactional
	public MarketplaceListingDetailRDO addItem(User teacher, Long listingId, MarketplaceListingItemCDO command) {
		TeacherProfile profile = requireApprovedTeacher(teacher);
		MarketplaceListing listing = requireOwnedListing(listingId, profile);
		ResolvedItemReference reference = resolveItemReference(teacher, profile, command.testId(), command.fileId());
		MarketplaceListingItem item = new MarketplaceListingItem(
				listing,
				reference.test(),
				reference.file(),
				trimToNull(command.title()),
				trimToNull(command.description()),
				command.itemOrder());
		itemRepository.save(item);
		deactivateIfInvalid(listing);
		return toDetail(listing);
	}

	@Override
	@Transactional
	public MarketplaceListingDetailRDO updateItem(
			User teacher,
			Long listingId,
			Long itemId,
			MarketplaceListingItemUDO command
	) {
		TeacherProfile profile = requireApprovedTeacher(teacher);
		MarketplaceListing listing = requireOwnedListing(listingId, profile);
		MarketplaceListingItem item = itemRepository.findByIdAndListingId(itemId, listing.getId())
				.orElseThrow(this::itemInvalid);
		ResolvedItemReference reference = resolveUpdateItemReference(teacher, profile, item, command);
		item.update(
				reference.test(),
				reference.file(),
				command.title() == null ? item.getTitle() : trimToNull(command.title()),
				command.description() == null ? item.getDescription() : trimToNull(command.description()),
				command.itemOrder() == null ? item.getItemOrder() : command.itemOrder());
		deactivateIfInvalid(listing);
		return toDetail(listing);
	}

	@Override
	@Transactional
	public MarketplaceListingDetailRDO deleteItem(User teacher, Long listingId, Long itemId) {
		TeacherProfile profile = requireApprovedTeacher(teacher);
		MarketplaceListing listing = requireOwnedListing(listingId, profile);
		MarketplaceListingItem item = itemRepository.findByIdAndListingId(itemId, listing.getId())
				.orElseThrow(this::itemInvalid);
		itemRepository.delete(item);
		deactivateIfInvalid(listing);
		return toDetail(listing);
	}

	@Override
	@Transactional(readOnly = true)
	public MarketplaceListingActivationRDO activationStatus(User teacher, Long listingId) {
		TeacherProfile profile = requireApprovedTeacher(teacher);
		return activationReport(requireOwnedListing(listingId, profile));
	}

	@Override
	@Transactional
	public MarketplaceListingDetailRDO activate(User teacher, Long listingId) {
		TeacherProfile profile = requireApprovedTeacher(teacher);
		MarketplaceListing listing = requireOwnedListing(listingId, profile);
		if (listing.isActive()) {
			throw statusConflict("Listing is already active.");
		}
		MarketplaceListingActivationRDO report = activationReport(listing);
		if (!report.ready()) {
			throw statusConflict(String.join(" ", report.errors()));
		}
		listing.activate();
		return toDetail(listing);
	}

	@Override
	@Transactional
	public MarketplaceListingDetailRDO deactivate(User teacher, Long listingId) {
		TeacherProfile profile = requireApprovedTeacher(teacher);
		MarketplaceListing listing = requireOwnedListing(listingId, profile);
		listing.deactivate();
		return toDetail(listing);
	}

	@Override
	@Transactional
	public void delete(User teacher, Long listingId) {
		TeacherProfile profile = requireApprovedTeacher(teacher);
		MarketplaceListing listing = requireOwnedListing(listingId, profile);
		listingRepository.delete(listing);
	}

	private Page<MarketplaceListingSummaryRDO> toSummaryPage(Page<MarketplaceListing> listings, Pageable pageable) {
		if (listings.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, listings.getTotalElements());
		}
		List<Long> listingIds = listings.getContent().stream().map(MarketplaceListing::getId).toList();
		Map<Long, MarketplaceListing> readModels = listingRepository.findReadModelsByIdIn(listingIds).stream()
				.collect(Collectors.toMap(MarketplaceListing::getId, Function.identity()));
		Map<Long, Integer> itemCounts = itemRepository.findByListingIdInOrderByListingIdAscItemOrderAscIdAsc(listingIds)
				.stream()
				.collect(Collectors.groupingBy(
						item -> item.getListing().getId(),
						Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
		List<MarketplaceListingSummaryRDO> content = listingIds.stream()
				.map(readModels::get)
				.filter(Objects::nonNull)
				.map(listing -> mapper.toSummaryRDO(listing, itemCounts.getOrDefault(listing.getId(), 0)))
				.toList();
		return new PageImpl<>(content, pageable, listings.getTotalElements());
	}

	private MarketplaceListingDetailRDO toDetail(MarketplaceListing listing) {
		List<MarketplaceListingItem> items = itemRepository.findByListingIdOrderByItemOrderAscIdAsc(listing.getId());
		return mapper.toDetailRDO(listing, items);
	}

	private TeacherProfile requireApprovedTeacher(User teacher) {
		TeacherProfile profile = teacherProfileRepository.findByUserId(teacher.getId())
				.orElseThrow(this::teacherApprovalRequired);
		if (!TeacherProfileStatuses.APPROVED.equals(profile.getStatus())) {
			throw teacherApprovalRequired();
		}
		return profile;
	}

	private MarketplaceListing requireOwnedListing(Long listingId, TeacherProfile profile) {
		return listingRepository.findByIdAndTeacherProfile_Id(listingId, profile.getId())
				.orElseThrow(this::notFound);
	}

	private CatalogTest resolveOwnedTest(User teacher, TeacherProfile profile, Long testId) {
		if (testId == null) {
			return null;
		}
		CatalogTest test = testRepository.findByIdAndTeacherProfileIdAndDeletedAtIsNull(testId, profile.getId())
				.orElseThrow(this::itemInvalid);
		if (!teacher.getId().equals(test.getCreatedByUserId())) {
			throw itemInvalid();
		}
		return test;
	}

	private ResolvedItemReference resolveItemReference(User teacher, TeacherProfile profile, Long testId, Long fileId) {
		if ((testId == null && fileId == null) || (testId != null && fileId != null)) {
			throw itemInvalid();
		}
		if (testId != null) {
			return new ResolvedItemReference(resolveOwnedTest(teacher, profile, testId), null);
		}
		return new ResolvedItemReference(null, resolveOwnedFile(teacher, fileId));
	}

	private ResolvedItemReference resolveUpdateItemReference(
			User teacher,
			TeacherProfile profile,
			MarketplaceListingItem item,
			MarketplaceListingItemUDO command
	) {
		boolean hasTest = command.testId() != null;
		boolean hasFile = command.fileId() != null;
		if (hasTest || hasFile) {
			return resolveItemReference(teacher, profile, command.testId(), command.fileId());
		}
		return new ResolvedItemReference(item.getTest(), item.getFile());
	}

	private FileMetadata resolveOwnedFile(User teacher, Long fileId) {
		FileMetadata file = fileMetadataRepository.findByIdAndOwner_Id(fileId, teacher.getId())
				.orElseThrow(this::itemInvalid);
		if (!LISTING_FILE_PURPOSES.contains(file.getPurpose())) {
			throw itemInvalid();
		}
		return file;
	}

	private MarketplaceListingActivationRDO activationReport(MarketplaceListing listing) {
		List<String> errors = new java.util.ArrayList<>();
		if (listing.getPriceAmount() == null || listing.getPriceAmount().compareTo(BigDecimal.ZERO) < 0) {
			errors.add("Listing price must be non-negative.");
		}
		if (isBlank(listing.getTitle())) {
			errors.add("Listing title is required.");
		}
		if (!MarketplaceListingTypes.isSupported(listing.getListingType())) {
			errors.add("Listing type is unsupported.");
		}
		if (MarketplaceListingTypes.isDirectTestListing(listing.getListingType())) {
			if (listing.getTest() == null) {
				errors.add("Single test listings require a test.");
			}
			else if (!isPublished(listing.getTest())) {
				errors.add("Referenced test must be published.");
			}
		}
		else {
			List<MarketplaceListingItem> items = itemRepository.findByListingIdOrderByItemOrderAscIdAsc(listing.getId());
			if (items.isEmpty()) {
				errors.add("Pack listings require at least one item.");
			}
			for (MarketplaceListingItem item : items) {
				if (item.getTest() != null && !isPublished(item.getTest())) {
					errors.add("Listing item tests must be published.");
				}
				if (item.getTest() == null && item.getFile() == null) {
					errors.add("Listing items require a test or file.");
				}
			}
		}
		return new MarketplaceListingActivationRDO(listing.getId(), errors.isEmpty(), errors);
	}

	private boolean isPubliclyDiscoverable(MarketplaceListing listing) {
		if (!listing.isActive()
				|| listing.getTeacherProfile() == null
				|| !TeacherProfileStatuses.APPROVED.equals(listing.getTeacherProfile().getStatus())) {
			return false;
		}
		return activationReport(listing).ready();
	}

	private void deactivateIfInvalid(MarketplaceListing listing) {
		if (listing.isActive() && !activationReport(listing).ready()) {
			listing.deactivate();
		}
	}

	private void validateDirectTestShape(String listingType, CatalogTest test) {
		if (MarketplaceListingTypes.isDirectTestListing(listingType)) {
			return;
		}
		if (test != null) {
			throw itemInvalid();
		}
	}

	private boolean isPublished(CatalogTest test) {
		return TestVisibilities.PUBLISHED.equals(test.getVisibility()) && test.getDeletedAt() == null;
	}

	private boolean isFree(MarketplaceListing listing) {
		return listing.getPriceAmount() == null || BigDecimal.ZERO.compareTo(listing.getPriceAmount()) == 0;
	}

	private String normalizeListingType(String listingType) {
		String normalized = requiredText(listingType, "Listing type is required.").toLowerCase();
		if (!MarketplaceListingTypes.isSupported(normalized)) {
			throw new BusinessException("VALIDATION_FAILED", "Unsupported listing type.", HttpStatus.BAD_REQUEST);
		}
		return normalized;
	}

	private BigDecimal nonNegativePrice(BigDecimal priceAmount) {
		if (priceAmount == null || priceAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessException("VALIDATION_FAILED", "Listing price must be non-negative.", HttpStatus.BAD_REQUEST);
		}
		return priceAmount;
	}

	private String currency(String currency) {
		String normalized = requiredText(currency, "Currency is required.").toUpperCase();
		if (normalized.length() != 3) {
			throw new BusinessException("VALIDATION_FAILED", "Currency must be a 3-letter code.", HttpStatus.BAD_REQUEST);
		}
		return normalized;
	}

	private String language(String language) {
		return requiredText(language, "Language is required.").toLowerCase();
	}

	private String requiredText(String value, String message) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			throw new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
		}
		return normalized;
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

	private BusinessException notFound() {
		return new BusinessException("LISTING_NOT_FOUND", "Marketplace listing was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException itemInvalid() {
		return new BusinessException("LISTING_ITEM_INVALID", "Marketplace listing item is invalid.", HttpStatus.BAD_REQUEST);
	}

	private BusinessException teacherApprovalRequired() {
		return new BusinessException(
				"TEACHER_APPROVAL_REQUIRED",
				"Teacher approval is required to manage marketplace listings.",
				HttpStatus.FORBIDDEN);
	}

	private BusinessException statusConflict(String message) {
		return new BusinessException("LISTING_STATUS_CONFLICT", message, HttpStatus.CONFLICT);
	}

	private record ResolvedItemReference(CatalogTest test, FileMetadata file) {
	}
}
