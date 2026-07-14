package com.mockexams.backend.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.api.ApiPageMeta;
import com.mockexams.backend.api.ApiSortMeta;
import com.mockexams.backend.dto.TestCardRDO;
import com.mockexams.backend.dto.TestCategoryRDO;
import com.mockexams.backend.dto.TestDetailRDO;
import com.mockexams.backend.dto.TestTagRDO;
import com.mockexams.backend.entity.TestPrices;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.service.TestCatalogSearchCriteria;
import com.mockexams.backend.service.TestCatalogService;

@RestController
@Validated
@RequestMapping("/api/v1/public")
class TestCatalogController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String DEFAULT_SORT_FIELD = "publishedAt";
	private static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;
	private static final Set<String> ALLOWED_TEST_QUERY_PARAMS = Set.of(
			"page",
			"size",
			"sort",
			"type",
			"difficulty",
			"bandMin",
			"bandMax",
			"tag",
			"tags",
			"category",
			"categories",
			"language",
			"price",
			"teacherId");
	private static final Map<String, String> SORT_FIELDS = Map.of(
			"publishedAt", "publishedAt",
			"title", "title",
			"priceAmount", "priceAmount",
			"durationMinutes", "durationMinutes",
			"difficulty", "difficulty");

	private final TestCatalogService testCatalogService;

	TestCatalogController(TestCatalogService testCatalogService) {
		this.testCatalogService = testCatalogService;
	}

	@GetMapping("/tests")
	ResponseEntity<ApiEnvelope<List<TestCardRDO>>> listTests(
			@RequestParam MultiValueMap<String, String> queryParams,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String difficulty,
			@RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal bandMin,
			@RequestParam(required = false) @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal bandMax,
			@RequestParam(required = false) String language,
			@RequestParam(required = false) String price,
			@RequestParam(required = false) @Min(1) Long teacherId,
			@RequestParam(required = false, name = "sort") List<String> sortParams
	) {
		validateAllowedQueryParams(queryParams);
		String normalizedType = normalizeType(type);
		String normalizedPrice = normalizePrice(price);
		String normalizedDifficulty = trimToNull(difficulty);
		String normalizedLanguage = trimToNull(language);
		validateBandRange(bandMin, bandMax);
		SortSpec sortSpec = parseSort(sortParams);
		TestCatalogSearchCriteria criteria = new TestCatalogSearchCriteria(
				normalizedType,
				normalizedDifficulty,
				bandMin,
				bandMax,
				normalizeSlugs(queryParams, "tag", "tags"),
				normalizeSlugs(queryParams, "category", "categories"),
				normalizedLanguage,
				normalizedPrice,
				teacherId);
		Page<TestCardRDO> tests = testCatalogService.listTests(
				criteria,
				PageRequest.of(page, size, sortSpec.sort()));
		ApiMeta meta = ApiMeta.create(ApiPageMeta.from(tests), sortSpec.meta(), filters(criteria));
		return ResponseEntity.ok(ApiEnvelope.of(tests.getContent(), meta));
	}

	@GetMapping("/tests/{testId}")
	ResponseEntity<ApiEnvelope<TestDetailRDO>> testDetail(@PathVariable Long testId) {
		return ResponseEntity.ok(ApiEnvelope.of(testCatalogService.testDetail(testId)));
	}

	@GetMapping("/test-categories")
	ResponseEntity<ApiEnvelope<List<TestCategoryRDO>>> categories() {
		return ResponseEntity.ok(ApiEnvelope.of(testCatalogService.listCategories()));
	}

	@GetMapping("/test-tags")
	ResponseEntity<ApiEnvelope<List<TestTagRDO>>> tags() {
		return ResponseEntity.ok(ApiEnvelope.of(testCatalogService.listTags()));
	}

	private void validateAllowedQueryParams(MultiValueMap<String, String> queryParams) {
		for (String paramName : queryParams.keySet()) {
			if (!ALLOWED_TEST_QUERY_PARAMS.contains(paramName)) {
				throw validationFailed("Unsupported catalog filter: " + paramName + ".");
			}
		}
	}

	private String normalizeType(String type) {
		String normalized = trimToNull(type);
		if (normalized == null) {
			return null;
		}
		if (!TestTypes.isSupported(normalized)) {
			throw validationFailed("Unsupported test type.");
		}
		return normalized;
	}

	private String normalizePrice(String price) {
		String normalized = trimToNull(price);
		if (normalized == null) {
			return null;
		}
		if (!TestPrices.isSupported(normalized)) {
			throw validationFailed("Unsupported price filter.");
		}
		return normalized;
	}

	private void validateBandRange(BigDecimal bandMin, BigDecimal bandMax) {
		if (bandMin != null && bandMax != null && bandMin.compareTo(bandMax) > 0) {
			throw validationFailed("bandMin must be less than or equal to bandMax.");
		}
	}

	private SortSpec parseSort(List<String> sortParams) {
		if (sortParams == null || sortParams.isEmpty()) {
			Sort sort = Sort.by(DEFAULT_SORT_DIRECTION, DEFAULT_SORT_FIELD);
			return new SortSpec(sort, List.of(new ApiSortMeta(DEFAULT_SORT_FIELD, "desc")));
		}
		List<Sort.Order> orders = new ArrayList<>();
		List<ApiSortMeta> meta = new ArrayList<>();
		for (String sortParam : combineSplitSortParams(sortParams)) {
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

	private List<String> combineSplitSortParams(List<String> sortParams) {
		List<String> combined = new ArrayList<>();
		for (int index = 0; index < sortParams.size(); index++) {
			String current = sortParams.get(index);
			if (!current.contains(",") && index + 1 < sortParams.size() && isSortDirection(sortParams.get(index + 1))) {
				combined.add(current + "," + sortParams.get(index + 1));
				index++;
			}
			else {
				combined.add(current);
			}
		}
		return combined;
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

	private boolean isSortDirection(String value) {
		String direction = trimToNull(value);
		return "asc".equalsIgnoreCase(direction) || "desc".equalsIgnoreCase(direction);
	}

	private Map<String, Object> filters(TestCatalogSearchCriteria criteria) {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("visibility", "published");
		putIfPresent(filters, "type", criteria.type());
		putIfPresent(filters, "difficulty", criteria.difficulty());
		putIfPresent(filters, "bandMin", criteria.bandMin());
		putIfPresent(filters, "bandMax", criteria.bandMax());
		if (!criteria.tags().isEmpty()) {
			filters.put("tag", criteria.tags());
		}
		if (!criteria.categories().isEmpty()) {
			filters.put("category", criteria.categories());
		}
		putIfPresent(filters, "language", criteria.language());
		putIfPresent(filters, "price", criteria.price());
		putIfPresent(filters, "teacherId", criteria.teacherId());
		return filters;
	}

	private void putIfPresent(Map<String, Object> filters, String key, Object value) {
		if (value != null) {
			filters.put(key, value);
		}
	}

	private List<String> normalizeSlugs(MultiValueMap<String, String> queryParams, String singular, String plural) {
		List<String> values = new ArrayList<>();
		addSlugValues(values, queryParams.get(singular));
		addSlugValues(values, queryParams.get(plural));
		return List.copyOf(values);
	}

	private void addSlugValues(List<String> values, List<String> rawValues) {
		if (rawValues == null) {
			return;
		}
		for (String rawValue : rawValues) {
			for (String part : rawValue.split(",")) {
				String value = trimToNull(part);
				if (value != null) {
					values.add(value);
				}
			}
		}
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private record SortSpec(Sort sort, List<ApiSortMeta> meta) {
	}
}
