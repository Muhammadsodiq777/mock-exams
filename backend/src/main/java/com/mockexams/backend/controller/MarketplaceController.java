package com.mockexams.backend.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.api.ApiPageMeta;
import com.mockexams.backend.api.ApiSortMeta;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.MarketplaceListingActivationRDO;
import com.mockexams.backend.dto.MarketplaceListingCDO;
import com.mockexams.backend.dto.MarketplaceListingDetailRDO;
import com.mockexams.backend.dto.MarketplaceListingItemCDO;
import com.mockexams.backend.dto.MarketplaceListingItemUDO;
import com.mockexams.backend.dto.MarketplaceListingSummaryRDO;
import com.mockexams.backend.dto.MarketplaceListingUDO;
import com.mockexams.backend.entity.MarketplaceListingTypes;
import com.mockexams.backend.entity.TestPrices;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.service.MarketplaceListingService;
import com.mockexams.backend.service.MarketplaceSearchCriteria;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1")
class MarketplaceController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String DEFAULT_SORT_FIELD = "createdAt";
	private static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;
	private static final Set<String> ALLOWED_PUBLIC_QUERY_PARAMS = Set.of(
			"page",
			"size",
			"sort",
			"type",
			"teacherId",
			"price",
			"minPrice",
			"maxPrice",
			"tag",
			"tags",
			"category",
			"categories",
			"language",
			"q");
	private static final Map<String, String> SORT_FIELDS = Map.of(
			"createdAt", "createdAt",
			"title", "title",
			"priceAmount", "priceAmount");

	private final AuthorizationService authorizationService;
	private final MarketplaceListingService marketplaceListingService;

	MarketplaceController(
			AuthorizationService authorizationService,
			MarketplaceListingService marketplaceListingService
	) {
		this.authorizationService = authorizationService;
		this.marketplaceListingService = marketplaceListingService;
	}

	@GetMapping("/public/marketplace")
	ResponseEntity<ApiEnvelope<List<MarketplaceListingSummaryRDO>>> listPublic(
			@RequestParam MultiValueMap<String, String> queryParams,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@RequestParam(required = false, name = "type") String listingType,
			@RequestParam(required = false) @Min(1) Long teacherId,
			@RequestParam(required = false) String price,
			@RequestParam(required = false) @DecimalMin("0.00") BigDecimal minPrice,
			@RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxPrice,
			@RequestParam(required = false) String language,
			@RequestParam(required = false, name = "q") String query,
			@RequestParam(required = false, name = "sort") List<String> sortParams
	) {
		validateAllowedQueryParams(queryParams);
		String normalizedListingType = normalizeListingType(listingType);
		String normalizedPrice = normalizePrice(price);
		validatePriceRange(minPrice, maxPrice);
		SortSpec sortSpec = parseSort(sortParams);
		MarketplaceSearchCriteria criteria = new MarketplaceSearchCriteria(
				normalizedListingType,
				teacherId,
				normalizedPrice,
				minPrice,
				maxPrice,
				normalizeSlugs(queryParams, "tag", "tags"),
				normalizeSlugs(queryParams, "category", "categories"),
				trimToNull(language),
				trimToNull(query));
		Page<MarketplaceListingSummaryRDO> listings = marketplaceListingService.listPublic(
				criteria,
				PageRequest.of(page, size, sortSpec.sort()));
		ApiMeta meta = ApiMeta.create(ApiPageMeta.from(listings), sortSpec.meta(), filters(criteria, true));
		return ResponseEntity.ok(ApiEnvelope.of(listings.getContent(), meta));
	}

	@GetMapping("/public/marketplace/{listingId}")
	ResponseEntity<ApiEnvelope<MarketplaceListingDetailRDO>> publicDetail(@PathVariable Long listingId) {
		return ResponseEntity.ok(ApiEnvelope.of(marketplaceListingService.publicDetail(listingId)));
	}

	@GetMapping("/teacher/marketplace/listings")
	ResponseEntity<ApiEnvelope<List<MarketplaceListingSummaryRDO>>> teacherListings(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@RequestParam(required = false, name = "sort") List<String> sortParams
	) {
		User teacher = requireTeacher(authorizationHeader);
		SortSpec sortSpec = parseSort(sortParams);
		Page<MarketplaceListingSummaryRDO> listings = marketplaceListingService.listTeacherListings(
				teacher,
				PageRequest.of(page, size, sortSpec.sort()));
		ApiMeta meta = ApiMeta.create(ApiPageMeta.from(listings), sortSpec.meta(), Map.of());
		return ResponseEntity.ok(ApiEnvelope.of(listings.getContent(), meta));
	}

	@PostMapping("/teacher/marketplace/listings")
	ResponseEntity<ApiEnvelope<MarketplaceListingDetailRDO>> createListing(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@Valid @RequestBody MarketplaceListingCDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(marketplaceListingService.create(teacher, command)));
	}

	@GetMapping("/teacher/marketplace/listings/{listingId}")
	ResponseEntity<ApiEnvelope<MarketplaceListingDetailRDO>> teacherDetail(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long listingId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(marketplaceListingService.preview(teacher, listingId)));
	}

	@PatchMapping("/teacher/marketplace/listings/{listingId}")
	ResponseEntity<ApiEnvelope<MarketplaceListingDetailRDO>> updateListing(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long listingId,
			@Valid @RequestBody MarketplaceListingUDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(marketplaceListingService.update(teacher, listingId, command)));
	}

	@DeleteMapping("/teacher/marketplace/listings/{listingId}")
	ResponseEntity<ApiEnvelope<Void>> deleteListing(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long listingId
	) {
		User teacher = requireTeacher(authorizationHeader);
		marketplaceListingService.delete(teacher, listingId);
		return ResponseEntity.ok(ApiEnvelope.of(null));
	}

	@PostMapping("/teacher/marketplace/listings/{listingId}/items")
	ResponseEntity<ApiEnvelope<MarketplaceListingDetailRDO>> addItem(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long listingId,
			@Valid @RequestBody MarketplaceListingItemCDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(marketplaceListingService.addItem(teacher, listingId, command)));
	}

	@PatchMapping("/teacher/marketplace/listings/{listingId}/items/{itemId}")
	ResponseEntity<ApiEnvelope<MarketplaceListingDetailRDO>> updateItem(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long listingId,
			@PathVariable Long itemId,
			@Valid @RequestBody MarketplaceListingItemUDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(
				marketplaceListingService.updateItem(teacher, listingId, itemId, command)));
	}

	@DeleteMapping("/teacher/marketplace/listings/{listingId}/items/{itemId}")
	ResponseEntity<ApiEnvelope<MarketplaceListingDetailRDO>> deleteItem(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long listingId,
			@PathVariable Long itemId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(marketplaceListingService.deleteItem(teacher, listingId, itemId)));
	}

	@GetMapping("/teacher/marketplace/listings/{listingId}/activation")
	ResponseEntity<ApiEnvelope<MarketplaceListingActivationRDO>> activation(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long listingId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(marketplaceListingService.activationStatus(teacher, listingId)));
	}

	@PostMapping("/teacher/marketplace/listings/{listingId}/activate")
	ResponseEntity<ApiEnvelope<MarketplaceListingDetailRDO>> activate(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long listingId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(marketplaceListingService.activate(teacher, listingId)));
	}

	@PatchMapping("/teacher/marketplace/listings/{listingId}/deactivate")
	ResponseEntity<ApiEnvelope<MarketplaceListingDetailRDO>> deactivate(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long listingId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(marketplaceListingService.deactivate(teacher, listingId)));
	}

	@GetMapping("/teacher/marketplace/listings/{listingId}/preview")
	ResponseEntity<ApiEnvelope<MarketplaceListingDetailRDO>> preview(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long listingId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(marketplaceListingService.preview(teacher, listingId)));
	}

	private User requireTeacher(String authorizationHeader) {
		return authorizationService.requireRole(authorizationHeader, RoleCodes.TEACHER);
	}

	private void validateAllowedQueryParams(MultiValueMap<String, String> queryParams) {
		for (String paramName : queryParams.keySet()) {
			if (!ALLOWED_PUBLIC_QUERY_PARAMS.contains(paramName)) {
				throw validationFailed("Unsupported marketplace filter: " + paramName + ".");
			}
		}
	}

	private String normalizeListingType(String listingType) {
		String normalized = trimToNull(listingType);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.toLowerCase();
		if (!MarketplaceListingTypes.isSupported(normalized)) {
			throw validationFailed("Unsupported marketplace listing type.");
		}
		return normalized;
	}

	private String normalizePrice(String price) {
		String normalized = trimToNull(price);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.toLowerCase();
		if (!TestPrices.isSupported(normalized)) {
			throw validationFailed("Unsupported price filter.");
		}
		return normalized;
	}

	private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
		if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
			throw validationFailed("minPrice must be less than or equal to maxPrice.");
		}
	}

	private SortSpec parseSort(List<String> sortParams) {
		if (sortParams == null || sortParams.isEmpty()) {
			Sort sort = Sort.by(DEFAULT_SORT_DIRECTION, DEFAULT_SORT_FIELD);
			return new SortSpec(sort, List.of(new ApiSortMeta(DEFAULT_SORT_FIELD, "desc")));
		}
		List<Sort.Order> orders = new ArrayList<>();
		List<ApiSortMeta> meta = new ArrayList<>();
		for (String sortParam : sortParams) {
			String[] parts = sortParam.split(",", -1);
			String requestedField = trimToNull(parts[0]);
			if (requestedField == null || !SORT_FIELDS.containsKey(requestedField)) {
				throw validationFailed("Unsupported sort field.");
			}
			Sort.Direction direction = Sort.Direction.ASC;
			if (parts.length > 1) {
				direction = parseSortDirection(parts[1]);
			}
			if (parts.length > 2) {
				throw validationFailed("Unsupported sort value.");
			}
			String entityField = SORT_FIELDS.get(requestedField);
			orders.add(new Sort.Order(direction, entityField));
			meta.add(new ApiSortMeta(requestedField, direction.name().toLowerCase()));
		}
		return new SortSpec(Sort.by(orders), meta);
	}

	private Sort.Direction parseSortDirection(String rawDirection) {
		String direction = trimToNull(rawDirection);
		if (direction == null) {
			return Sort.Direction.ASC;
		}
		if ("asc".equalsIgnoreCase(direction)) {
			return Sort.Direction.ASC;
		}
		if ("desc".equalsIgnoreCase(direction)) {
			return Sort.Direction.DESC;
		}
		throw validationFailed("Unsupported sort direction.");
	}

	private Map<String, Object> filters(MarketplaceSearchCriteria criteria, boolean publicOnly) {
		Map<String, Object> filters = new LinkedHashMap<>();
		if (publicOnly) {
			filters.put("status", "active");
		}
		putIfPresent(filters, "type", criteria.listingType());
		putIfPresent(filters, "teacherId", criteria.teacherId());
		putIfPresent(filters, "price", criteria.price());
		putIfPresent(filters, "minPrice", criteria.minPrice());
		putIfPresent(filters, "maxPrice", criteria.maxPrice());
		if (!criteria.tags().isEmpty()) {
			filters.put("tag", criteria.tags());
		}
		if (!criteria.categories().isEmpty()) {
			filters.put("category", criteria.categories());
		}
		putIfPresent(filters, "language", criteria.language());
		putIfPresent(filters, "q", criteria.query());
		return filters;
	}

	private List<String> normalizeSlugs(
			MultiValueMap<String, String> queryParams,
			String singularParam,
			String pluralParam
	) {
		List<String> values = new ArrayList<>();
		values.addAll(queryParams.getOrDefault(singularParam, List.of()));
		values.addAll(queryParams.getOrDefault(pluralParam, List.of()));
		return values.stream()
				.flatMap(value -> List.of(value.split(",")).stream())
				.map(this::trimToNull)
				.filter(value -> value != null)
				.distinct()
				.toList();
	}

	private void putIfPresent(Map<String, Object> filters, String key, Object value) {
		if (value != null) {
			filters.put(key, value);
		}
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private record SortSpec(Sort sort, List<ApiSortMeta> meta) {
	}
}
