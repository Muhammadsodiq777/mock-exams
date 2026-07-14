package com.mockexams.backend.controller;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
import com.mockexams.backend.dto.AdminPaymentDisputeRDO;
import com.mockexams.backend.dto.PaymentDisputeDecisionCDO;
import com.mockexams.backend.service.AdminPaymentDisputeSearchCriteria;
import com.mockexams.backend.service.PaymentDisputeService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/admin/payment-disputes")
class AdminPaymentDisputeController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "createdAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final AuthorizationService authorizationService;
	private final PaymentDisputeService paymentDisputeService;

	AdminPaymentDisputeController(AuthorizationService authorizationService, PaymentDisputeService paymentDisputeService) {
		this.authorizationService = authorizationService;
		this.paymentDisputeService = paymentDisputeService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<AdminPaymentDisputeRDO>>> disputes(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String paymentStatus,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(required = false) @Min(1) Long teacherProfileId,
			@RequestParam(required = false) @Min(1) Long studentUserId,
			@RequestParam(required = false) String reason,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		AdminPaymentDisputeSearchCriteria criteria = new AdminPaymentDisputeSearchCriteria(
				status, paymentStatus, from, to, teacherProfileId, studentUserId, reason);
		Page<AdminPaymentDisputeRDO> disputes = paymentDisputeService.adminQueue(
				criteria,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc(SORT_FIELD), Sort.Order.desc("id"))));
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(disputes),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				filters(status, paymentStatus, from, to, teacherProfileId, studentUserId, reason));
		return ResponseEntity.ok(ApiEnvelope.of(disputes.getContent(), meta));
	}

	@GetMapping("/{disputeId}")
	ResponseEntity<ApiEnvelope<AdminPaymentDisputeRDO>> dispute(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long disputeId
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(paymentDisputeService.adminDispute(disputeId)));
	}

	@PostMapping("/{disputeId}/decision")
	ResponseEntity<ApiEnvelope<AdminPaymentDisputeRDO>> decide(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long disputeId,
			@Valid @RequestBody PaymentDisputeDecisionCDO command
	) {
		User admin = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(paymentDisputeService.decide(admin, disputeId, command)));
	}

	private Map<String, Object> filters(
			String status,
			String paymentStatus,
			OffsetDateTime from,
			OffsetDateTime to,
			Long teacherProfileId,
			Long studentUserId,
			String reason
	) {
		Map<String, Object> filters = new LinkedHashMap<>();
		if (status != null && !status.isBlank()) {
			filters.put("status", status.trim().toLowerCase(Locale.ROOT));
		}
		if (paymentStatus != null && !paymentStatus.isBlank()) {
			filters.put("paymentStatus", paymentStatus.trim().toLowerCase(Locale.ROOT));
		}
		if (from != null) {
			filters.put("from", from);
		}
		if (to != null) {
			filters.put("to", to);
		}
		if (teacherProfileId != null) {
			filters.put("teacherProfileId", teacherProfileId);
		}
		if (studentUserId != null) {
			filters.put("studentUserId", studentUserId);
		}
		if (reason != null && !reason.isBlank()) {
			filters.put("reason", reason.trim());
		}
		return filters;
	}
}
