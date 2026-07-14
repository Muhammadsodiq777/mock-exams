package com.mockexams.backend.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.api.ApiPageMeta;
import com.mockexams.backend.api.ApiSortMeta;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.PaymentRDO;
import com.mockexams.backend.service.PaymentService;
import com.mockexams.backend.users.RoleCodes;

@RestController
@Validated
@RequestMapping("/api/v1/admin/payments")
class AdminPaymentController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "createdAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final AuthorizationService authorizationService;
	private final PaymentService paymentService;

	AdminPaymentController(AuthorizationService authorizationService, PaymentService paymentService) {
		this.authorizationService = authorizationService;
		this.paymentService = paymentService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<PaymentRDO>>> payments(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		Page<PaymentRDO> payments = paymentService.adminPayments(
				status,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc(SORT_FIELD), Sort.Order.desc("id"))));
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(payments),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				filters(status));
		return ResponseEntity.ok(ApiEnvelope.of(payments.getContent(), meta));
	}

	@GetMapping("/{paymentId}")
	ResponseEntity<ApiEnvelope<PaymentRDO>> payment(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long paymentId
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(paymentService.adminPayment(paymentId)));
	}

	private Map<String, Object> filters(String status) {
		Map<String, Object> filters = new LinkedHashMap<>();
		if (status != null && !status.isBlank()) {
			filters.put("status", status.trim().toLowerCase(Locale.ROOT));
		}
		return filters;
	}
}
