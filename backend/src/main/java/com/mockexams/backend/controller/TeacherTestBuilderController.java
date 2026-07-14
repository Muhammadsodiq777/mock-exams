package com.mockexams.backend.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.validation.Valid;
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
import com.mockexams.backend.dto.TeacherListeningSegmentCDO;
import com.mockexams.backend.dto.TeacherListeningSegmentReorderUDO;
import com.mockexams.backend.dto.TeacherListeningSegmentUDO;
import com.mockexams.backend.dto.TeacherQuestionCDO;
import com.mockexams.backend.dto.TeacherQuestionUDO;
import com.mockexams.backend.dto.TeacherReadingParagraphCDO;
import com.mockexams.backend.dto.TeacherReadingParagraphReorderUDO;
import com.mockexams.backend.dto.TeacherReadingParagraphUDO;
import com.mockexams.backend.dto.TeacherTestCDO;
import com.mockexams.backend.dto.TeacherTestRDO;
import com.mockexams.backend.dto.TeacherTestSectionCDO;
import com.mockexams.backend.dto.TeacherTestSectionReorderUDO;
import com.mockexams.backend.dto.TeacherTestSectionUDO;
import com.mockexams.backend.dto.TeacherTestSummaryRDO;
import com.mockexams.backend.dto.TeacherTestUDO;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.service.TeacherTestBuilderService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/teacher/tests")
class TeacherTestBuilderController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String DEFAULT_SORT_FIELD = "updatedAt";
	private static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;
	private static final Set<String> ALLOWED_TEST_QUERY_PARAMS = Set.of("page", "size", "sort", "status", "q");
	private static final Set<String> TEST_VISIBILITIES = Set.of(
			TestVisibilities.DRAFT,
			TestVisibilities.SUBMITTED,
			TestVisibilities.PUBLISHED,
			TestVisibilities.REJECTED,
			TestVisibilities.ARCHIVED);
	private static final Map<String, String> SORT_FIELDS = Map.of(
			"updatedAt", "updatedAt",
			"createdAt", "createdAt",
			"publishedAt", "publishedAt",
			"title", "title",
			"visibility", "visibility",
			"type", "type");

	private final AuthorizationService authorizationService;
	private final TeacherTestBuilderService teacherTestBuilderService;

	TeacherTestBuilderController(
			AuthorizationService authorizationService,
			TeacherTestBuilderService teacherTestBuilderService
	) {
		this.authorizationService = authorizationService;
		this.teacherTestBuilderService = teacherTestBuilderService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<TeacherTestSummaryRDO>>> listOwnedTests(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam MultiValueMap<String, String> queryParams,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@RequestParam(required = false) String status,
			@RequestParam(required = false, name = "q") String search,
			@RequestParam(required = false, name = "sort") List<String> sortParams
	) {
		User teacher = requireTeacher(authorizationHeader);
		validateAllowedQueryParams(queryParams);
		String normalizedStatus = normalizeStatus(status);
		String normalizedSearch = trimToNull(search);
		SortSpec sortSpec = parseSort(sortParams);
		Page<TeacherTestSummaryRDO> tests = teacherTestBuilderService.listOwnedTests(
				teacher,
				normalizedStatus,
				normalizedSearch,
				PageRequest.of(page, size, sortSpec.sort()));
		ApiMeta meta = ApiMeta.create(ApiPageMeta.from(tests), sortSpec.meta(), filters(normalizedStatus, normalizedSearch));
		return ResponseEntity.ok(ApiEnvelope.of(tests.getContent(), meta));
	}

	@PostMapping
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> createDraft(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@Valid @RequestBody TeacherTestCDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(teacherTestBuilderService.createDraft(teacher, command)));
	}

	@PatchMapping("/{testId}")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> updateTest(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@Valid @RequestBody TeacherTestUDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(teacherTestBuilderService.updateTest(teacher, testId, command)));
	}

	@PostMapping("/{testId}/sections")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> addSection(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@Valid @RequestBody TeacherTestSectionCDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(teacherTestBuilderService.addSection(teacher, testId, command)));
	}

	@PatchMapping("/{testId}/sections/{sectionId}")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> updateSection(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@Valid @RequestBody TeacherTestSectionUDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(
				teacherTestBuilderService.updateSection(teacher, testId, sectionId, command)));
	}

	@PatchMapping("/{testId}/sections/reorder")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> reorderSections(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@Valid @RequestBody TeacherTestSectionReorderUDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(teacherTestBuilderService.reorderSections(teacher, testId, command)));
	}

	@DeleteMapping("/{testId}/sections/{sectionId}")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> deleteSection(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(teacherTestBuilderService.deleteSection(teacher, testId, sectionId)));
	}

	@PostMapping("/{testId}/sections/{sectionId}/paragraphs")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> addParagraph(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@Valid @RequestBody TeacherReadingParagraphCDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(teacherTestBuilderService.addParagraph(teacher, testId, sectionId, command)));
	}

	@PatchMapping("/{testId}/sections/{sectionId}/paragraphs/{paragraphId}")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> updateParagraph(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@PathVariable Long paragraphId,
			@Valid @RequestBody TeacherReadingParagraphUDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(
				teacherTestBuilderService.updateParagraph(teacher, testId, sectionId, paragraphId, command)));
	}

	@PatchMapping("/{testId}/sections/{sectionId}/paragraphs/reorder")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> reorderParagraphs(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@Valid @RequestBody TeacherReadingParagraphReorderUDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(
				teacherTestBuilderService.reorderParagraphs(teacher, testId, sectionId, command)));
	}

	@DeleteMapping("/{testId}/sections/{sectionId}/paragraphs/{paragraphId}")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> deleteParagraph(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@PathVariable Long paragraphId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(
				teacherTestBuilderService.deleteParagraph(teacher, testId, sectionId, paragraphId)));
	}

	@PostMapping("/{testId}/sections/{sectionId}/listening-segments")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> addListeningSegment(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@Valid @RequestBody TeacherListeningSegmentCDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(teacherTestBuilderService.addListeningSegment(teacher, testId, sectionId, command)));
	}

	@PatchMapping("/{testId}/sections/{sectionId}/listening-segments/{segmentId}")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> updateListeningSegment(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@PathVariable Long segmentId,
			@Valid @RequestBody TeacherListeningSegmentUDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(
				teacherTestBuilderService.updateListeningSegment(teacher, testId, sectionId, segmentId, command)));
	}

	@PatchMapping("/{testId}/sections/{sectionId}/listening-segments/reorder")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> reorderListeningSegments(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@Valid @RequestBody TeacherListeningSegmentReorderUDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(
				teacherTestBuilderService.reorderListeningSegments(teacher, testId, sectionId, command)));
	}

	@DeleteMapping("/{testId}/sections/{sectionId}/listening-segments/{segmentId}")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> deleteListeningSegment(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@PathVariable Long segmentId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(
				teacherTestBuilderService.deleteListeningSegment(teacher, testId, sectionId, segmentId)));
	}

	@PostMapping("/{testId}/sections/{sectionId}/questions")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> addQuestion(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@Valid @RequestBody TeacherQuestionCDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(teacherTestBuilderService.addQuestion(teacher, testId, sectionId, command)));
	}

	@PatchMapping("/{testId}/sections/{sectionId}/questions/{questionId}")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> updateQuestion(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@PathVariable Long questionId,
			@Valid @RequestBody TeacherQuestionUDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(
				teacherTestBuilderService.updateQuestion(teacher, testId, sectionId, questionId, command)));
	}

	@DeleteMapping("/{testId}/sections/{sectionId}/questions/{questionId}")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> deleteQuestion(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId,
			@PathVariable Long sectionId,
			@PathVariable Long questionId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(
				teacherTestBuilderService.deleteQuestion(teacher, testId, sectionId, questionId)));
	}

	@GetMapping("/{testId}/preview")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> preview(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(teacherTestBuilderService.preview(teacher, testId)));
	}

	@PostMapping("/{testId}/submit")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> submit(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(teacherTestBuilderService.submit(teacher, testId)));
	}

	@PostMapping("/{testId}/archive")
	ResponseEntity<ApiEnvelope<TeacherTestRDO>> archive(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long testId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(teacherTestBuilderService.archive(teacher, testId)));
	}

	private User requireTeacher(String authorizationHeader) {
		return authorizationService.requireRole(authorizationHeader, RoleCodes.TEACHER);
	}

	private void validateAllowedQueryParams(MultiValueMap<String, String> queryParams) {
		for (String paramName : queryParams.keySet()) {
			if (!ALLOWED_TEST_QUERY_PARAMS.contains(paramName)) {
				throw validationFailed("Unsupported teacher test filter: " + paramName + ".");
			}
		}
	}

	private String normalizeStatus(String status) {
		String normalized = trimToNull(status);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.toLowerCase(Locale.ROOT);
		if (!TEST_VISIBILITIES.contains(normalized)) {
			throw validationFailed("Unsupported teacher test status.");
		}
		return normalized;
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
			orders.add(new Sort.Order(direction, SORT_FIELDS.get(requestedField)));
			meta.add(new ApiSortMeta(requestedField, direction.name().toLowerCase(Locale.ROOT)));
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

	private Map<String, Object> filters(String status, String search) {
		Map<String, Object> filters = new LinkedHashMap<>();
		putIfPresent(filters, "status", status);
		putIfPresent(filters, "q", search);
		return filters;
	}

	private void putIfPresent(Map<String, Object> filters, String key, Object value) {
		if (value != null) {
			filters.put(key, value);
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
