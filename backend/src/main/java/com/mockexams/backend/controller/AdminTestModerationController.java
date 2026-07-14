package com.mockexams.backend.controller;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.mockexams.backend.dto.AdminModerationQueueMetaRDO;
import com.mockexams.backend.dto.AdminModerationTestDetailRDO;
import com.mockexams.backend.dto.AdminModerationTestRDO;
import com.mockexams.backend.dto.TestModerationRejectCDO;
import com.mockexams.backend.service.AdminModerationSearchCriteria;
import com.mockexams.backend.service.TestModerationService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/admin/moderation/tests")
class AdminTestModerationController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "updatedAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.ASC;

	private final AuthorizationService authorizationService;
	private final TestModerationService testModerationService;

	AdminTestModerationController(AuthorizationService authorizationService, TestModerationService testModerationService) {
		this.authorizationService = authorizationService;
		this.testModerationService = testModerationService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<AdminModerationTestRDO>>> queue(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) @Min(1) Long teacherProfileId,
			@RequestParam(required = false) String difficulty,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime submittedFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime submittedTo,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		AdminModerationSearchCriteria criteria = new AdminModerationSearchCriteria(
				type, teacherProfileId, difficulty, submittedFrom, submittedTo, search);
		Page<AdminModerationTestRDO> queue = testModerationService.queue(
				criteria,
				PageRequest.of(page, size, Sort.by(Sort.Order.asc(SORT_FIELD), Sort.Order.asc("id"))));
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(queue),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				filters(criteria));
		return ResponseEntity.ok(ApiEnvelope.of(queue.getContent(), meta));
	}

	@GetMapping("/meta")
	ResponseEntity<ApiEnvelope<AdminModerationQueueMetaRDO>> queueMeta(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(testModerationService.queueMeta()));
	}

	@GetMapping("/{testId}")
	ResponseEntity<ApiEnvelope<AdminModerationTestDetailRDO>> preview(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long testId
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(testModerationService.preview(testId)));
	}

	@PostMapping("/{testId}/approve")
	ResponseEntity<ApiEnvelope<AdminModerationTestDetailRDO>> approve(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long testId
	) {
		User admin = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(testModerationService.approve(admin, testId)));
	}

	@PostMapping("/{testId}/reject")
	ResponseEntity<ApiEnvelope<AdminModerationTestDetailRDO>> reject(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long testId,
			@Valid @RequestBody TestModerationRejectCDO command
	) {
		User admin = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(testModerationService.reject(admin, testId, command.reason())));
	}

	private Map<String, Object> filters(AdminModerationSearchCriteria criteria) {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("visibility", "submitted");
		putIfPresent(filters, "type", criteria.type());
		putIfPresent(filters, "teacherProfileId", criteria.teacherProfileId());
		putIfPresent(filters, "difficulty", criteria.difficulty());
		putIfPresent(filters, "submittedFrom", criteria.submittedFrom());
		putIfPresent(filters, "submittedTo", criteria.submittedTo());
		putIfPresent(filters, "search", criteria.search());
		return filters;
	}

	private void putIfPresent(Map<String, Object> filters, String key, Object value) {
		if (value != null) {
			filters.put(key, value);
		}
	}
}
