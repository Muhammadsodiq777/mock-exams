package com.mockexams.backend.controller;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.api.ApiPageMeta;
import com.mockexams.backend.api.ApiSortMeta;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.AdminAiUsageReportRDO;
import com.mockexams.backend.dto.AdminAiUsageReportTotalsRDO;
import com.mockexams.backend.dto.AiUsageRemainingRDO;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.service.AiUsageReportCriteria;
import com.mockexams.backend.service.AiUsageService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
class AiUsageController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "createdAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final AuthorizationService authorizationService;
	private final AiUsageService aiUsageService;

	AiUsageController(AuthorizationService authorizationService, AiUsageService aiUsageService) {
		this.authorizationService = authorizationService;
		this.aiUsageService = aiUsageService;
	}

	@GetMapping("/api/v1/student/ai-usage")
	ResponseEntity<ApiEnvelope<AiUsageRemainingRDO>> remainingUsage(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		User user = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(aiUsageService.remainingUsage(user.getId())));
	}

	@GetMapping("/api/v1/admin/ai-usage/report")
	ResponseEntity<ApiEnvelope<List<AdminAiUsageReportRDO>>> report(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) Long studentUserId,
			@RequestParam(required = false) String feedbackType,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String provider,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		AiUsageReportCriteria criteria = criteria(studentUserId, feedbackType, status, provider, from, to);
		Page<AdminAiUsageReportRDO> report = aiUsageService.listReport(criteria,
				PageRequest.of(page, size, Sort.by(SORT_DIRECTION, SORT_FIELD)));
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(report),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				filters(criteria));
		return ResponseEntity.ok(ApiEnvelope.of(report.getContent(), meta));
	}

	@GetMapping("/api/v1/admin/ai-usage/report/totals")
	ResponseEntity<ApiEnvelope<AdminAiUsageReportTotalsRDO>> reportTotals(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) Long studentUserId,
			@RequestParam(required = false) String feedbackType,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String provider,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(aiUsageService.summarizeReport(
				criteria(studentUserId, feedbackType, status, provider, from, to))));
	}

	private AiUsageReportCriteria criteria(
			Long studentUserId,
			String feedbackType,
			String status,
			String provider,
			OffsetDateTime from,
			OffsetDateTime to
	) {
		if (from != null && to != null && to.isBefore(from)) {
			throw new BusinessException("VALIDATION_FAILED", "The to filter must be after from.",
					org.springframework.http.HttpStatus.BAD_REQUEST);
		}
		if (feedbackType != null && !aiUsageService.supportedFeedbackTypes().contains(feedbackType)) {
			throw new BusinessException("VALIDATION_FAILED", "Unsupported AI feedback type.",
					org.springframework.http.HttpStatus.BAD_REQUEST);
		}
		return new AiUsageReportCriteria(studentUserId, feedbackType, status, provider, from, to);
	}

	private Map<String, Object> filters(AiUsageReportCriteria criteria) {
		Map<String, Object> filters = new LinkedHashMap<>();
		putIfPresent(filters, "studentUserId", criteria.studentUserId());
		putIfPresent(filters, "feedbackType", criteria.feedbackType());
		putIfPresent(filters, "status", criteria.status());
		putIfPresent(filters, "provider", criteria.provider());
		putIfPresent(filters, "from", criteria.from());
		putIfPresent(filters, "to", criteria.to());
		return filters;
	}

	private void putIfPresent(Map<String, Object> filters, String key, Object value) {
		if (value != null) {
			filters.put(key, value);
		}
	}
}
