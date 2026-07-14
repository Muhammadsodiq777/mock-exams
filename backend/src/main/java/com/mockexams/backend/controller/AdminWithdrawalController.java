package com.mockexams.backend.controller;

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
import com.mockexams.backend.dto.AdminWithdrawalRequestRDO;
import com.mockexams.backend.dto.WithdrawalRejectionCDO;
import com.mockexams.backend.service.WithdrawalService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/admin/withdrawals")
class AdminWithdrawalController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "createdAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final AuthorizationService authorizationService;
	private final WithdrawalService withdrawalService;

	AdminWithdrawalController(AuthorizationService authorizationService, WithdrawalService withdrawalService) {
		this.authorizationService = authorizationService;
		this.withdrawalService = withdrawalService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<AdminWithdrawalRequestRDO>>> withdrawals(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) @Min(1) Long teacherProfileId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		Page<AdminWithdrawalRequestRDO> withdrawals = withdrawalService.adminQueue(
				status,
				teacherProfileId,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc(SORT_FIELD), Sort.Order.desc("id"))));
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(withdrawals),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				filters(status, teacherProfileId));
		return ResponseEntity.ok(ApiEnvelope.of(withdrawals.getContent(), meta));
	}

	@GetMapping("/{withdrawalRequestId}")
	ResponseEntity<ApiEnvelope<AdminWithdrawalRequestRDO>> withdrawal(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long withdrawalRequestId
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(withdrawalService.adminWithdrawal(withdrawalRequestId)));
	}

	@PostMapping("/{withdrawalRequestId}/approve")
	ResponseEntity<ApiEnvelope<AdminWithdrawalRequestRDO>> approve(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long withdrawalRequestId
	) {
		User admin = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(withdrawalService.approve(admin, withdrawalRequestId)));
	}

	@PostMapping("/{withdrawalRequestId}/reject")
	ResponseEntity<ApiEnvelope<AdminWithdrawalRequestRDO>> reject(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long withdrawalRequestId,
			@Valid @RequestBody WithdrawalRejectionCDO command
	) {
		User admin = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(withdrawalService.reject(admin, withdrawalRequestId, command.reason())));
	}

	@PostMapping("/{withdrawalRequestId}/mark-paid")
	ResponseEntity<ApiEnvelope<AdminWithdrawalRequestRDO>> markPaid(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long withdrawalRequestId
	) {
		User admin = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(withdrawalService.markPaid(admin, withdrawalRequestId)));
	}

	private Map<String, Object> filters(String status, Long teacherProfileId) {
		Map<String, Object> filters = new LinkedHashMap<>();
		if (status != null && !status.isBlank()) {
			filters.put("status", status.trim().toLowerCase(Locale.ROOT));
		}
		if (teacherProfileId != null) {
			filters.put("teacherProfileId", teacherProfileId);
		}
		return filters;
	}
}
