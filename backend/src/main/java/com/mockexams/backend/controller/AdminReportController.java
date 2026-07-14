package com.mockexams.backend.controller;

import java.time.OffsetDateTime;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.AdminAiUsageAggregateReportRDO;
import com.mockexams.backend.dto.AdminProviderFailureReportRDO;
import com.mockexams.backend.dto.AdminRevenueReportRDO;
import com.mockexams.backend.dto.AdminSalesReportRDO;
import com.mockexams.backend.dto.AdminTestActivityReportRDO;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.service.AdminReportCriteria;
import com.mockexams.backend.service.AdminReportService;
import com.mockexams.backend.users.RoleCodes;

@RestController
@Validated
@RequestMapping("/api/v1/admin/reports")
class AdminReportController {

	private static final Set<String> AI_USAGE_PARAMETERS = Set.of(
			"from", "to", "studentUserId", "feedbackType", "status", "provider", "model");
	private static final Set<String> SALES_PARAMETERS = Set.of(
			"from", "to", "teacherProfileId", "listingId", "provider", "paymentStatus");
	private static final Set<String> REVENUE_PARAMETERS = Set.of(
			"from", "to", "teacherProfileId", "listingId");
	private static final Set<String> TEST_ACTIVITY_PARAMETERS = Set.of(
			"from", "to", "teacherProfileId", "testType");
	private static final Set<String> PROVIDER_FAILURE_PARAMETERS = Set.of(
			"from", "to", "provider", "paymentStatus", "status", "model");

	private final AuthorizationService authorizationService;
	private final AdminReportService adminReportService;

	AdminReportController(AuthorizationService authorizationService, AdminReportService adminReportService) {
		this.authorizationService = authorizationService;
		this.adminReportService = adminReportService;
	}

	@GetMapping("/ai-usage")
	ResponseEntity<ApiEnvelope<AdminAiUsageAggregateReportRDO>> aiUsage(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(required = false) @Min(1) Long studentUserId,
			@RequestParam(required = false) String feedbackType,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String provider,
			@RequestParam(required = false) String model,
			HttpServletRequest request
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		requireSupportedParameters(request, AI_USAGE_PARAMETERS);
		return ResponseEntity.ok(ApiEnvelope.of(adminReportService.aiUsage(new AdminReportCriteria(
				from, to, studentUserId, null, null, null, feedbackType, status, model, provider, null))));
	}

	@GetMapping("/sales")
	ResponseEntity<ApiEnvelope<AdminSalesReportRDO>> sales(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(required = false) @Min(1) Long teacherProfileId,
			@RequestParam(required = false) @Min(1) Long listingId,
			@RequestParam(required = false) String provider,
			@RequestParam(required = false) String paymentStatus,
			HttpServletRequest request
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		requireSupportedParameters(request, SALES_PARAMETERS);
		return ResponseEntity.ok(ApiEnvelope.of(adminReportService.sales(new AdminReportCriteria(
				from, to, null, teacherProfileId, listingId, null, null, null, null, provider, paymentStatus))));
	}

	@GetMapping("/revenue")
	ResponseEntity<ApiEnvelope<AdminRevenueReportRDO>> revenue(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(required = false) @Min(1) Long teacherProfileId,
			@RequestParam(required = false) @Min(1) Long listingId,
			HttpServletRequest request
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		requireSupportedParameters(request, REVENUE_PARAMETERS);
		return ResponseEntity.ok(ApiEnvelope.of(adminReportService.revenue(new AdminReportCriteria(
				from, to, null, teacherProfileId, listingId, null, null, null, null, null, null))));
	}

	@GetMapping("/test-activity")
	ResponseEntity<ApiEnvelope<AdminTestActivityReportRDO>> testActivity(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(required = false) @Min(1) Long teacherProfileId,
			@RequestParam(required = false) String testType,
			HttpServletRequest request
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		requireSupportedParameters(request, TEST_ACTIVITY_PARAMETERS);
		return ResponseEntity.ok(ApiEnvelope.of(adminReportService.testActivity(new AdminReportCriteria(
				from, to, null, teacherProfileId, null, testType, null, null, null, null, null))));
	}

	@GetMapping("/provider-failures")
	ResponseEntity<ApiEnvelope<AdminProviderFailureReportRDO>> providerFailures(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(required = false) String provider,
			@RequestParam(required = false) String paymentStatus,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String model,
			HttpServletRequest request
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		requireSupportedParameters(request, PROVIDER_FAILURE_PARAMETERS);
		return ResponseEntity.ok(ApiEnvelope.of(adminReportService.providerFailures(new AdminReportCriteria(
				from, to, null, null, null, null, null, status, model, provider, paymentStatus))));
	}

	private void requireSupportedParameters(HttpServletRequest request, Set<String> supportedParameters) {
		for (String parameter : request.getParameterMap().keySet()) {
			if (!supportedParameters.contains(parameter)) {
				throw new BusinessException(
						"VALIDATION_FAILED",
						"Unsupported report filter or sort parameter.",
						HttpStatus.BAD_REQUEST);
			}
		}
	}
}
