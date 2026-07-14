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
import org.springframework.http.HttpStatus;
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
import com.mockexams.backend.dto.PaymentDisputeCDO;
import com.mockexams.backend.dto.PaymentDisputeRDO;
import com.mockexams.backend.service.PaymentDisputeService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/student/payment-disputes")
class PaymentDisputeController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "createdAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final AuthorizationService authorizationService;
	private final PaymentDisputeService paymentDisputeService;

	PaymentDisputeController(AuthorizationService authorizationService, PaymentDisputeService paymentDisputeService) {
		this.authorizationService = authorizationService;
		this.paymentDisputeService = paymentDisputeService;
	}

	@PostMapping
	ResponseEntity<ApiEnvelope<PaymentDisputeRDO>> create(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@Valid @RequestBody PaymentDisputeCDO command
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(paymentDisputeService.create(student, command)));
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<PaymentDisputeRDO>>> myDisputes(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		Page<PaymentDisputeRDO> disputes = paymentDisputeService.myDisputes(
				student,
				status,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc(SORT_FIELD), Sort.Order.desc("id"))));
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(disputes),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				filters(status));
		return ResponseEntity.ok(ApiEnvelope.of(disputes.getContent(), meta));
	}

	@GetMapping("/{disputeId}")
	ResponseEntity<ApiEnvelope<PaymentDisputeRDO>> myDispute(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long disputeId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(paymentDisputeService.myDispute(student, disputeId)));
	}

	private Map<String, Object> filters(String status) {
		Map<String, Object> filters = new LinkedHashMap<>();
		if (status != null && !status.isBlank()) {
			filters.put("status", status.trim().toLowerCase(Locale.ROOT));
		}
		return filters;
	}
}
