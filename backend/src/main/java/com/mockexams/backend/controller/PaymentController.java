package com.mockexams.backend.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.PaymentCDO;
import com.mockexams.backend.dto.PaymentRDO;
import com.mockexams.backend.dto.PromoQuoteRDO;
import com.mockexams.backend.service.PaymentService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/student/payments")
class PaymentController {

	private final AuthorizationService authorizationService;
	private final PaymentService paymentService;

	PaymentController(AuthorizationService authorizationService, PaymentService paymentService) {
		this.authorizationService = authorizationService;
		this.paymentService = paymentService;
	}

	@PostMapping
	ResponseEntity<ApiEnvelope<PaymentRDO>> createPayment(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@Valid @RequestBody PaymentCDO request
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.status(201).body(ApiEnvelope.of(paymentService.createPayment(student, request)));
	}

	@GetMapping("/promo-quote")
	ResponseEntity<ApiEnvelope<PromoQuoteRDO>> promoQuote(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam @Min(1) Long listingId,
			@RequestParam @NotBlank @Size(max = 60) String promoCode
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(paymentService.promoQuote(student, listingId, promoCode)));
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<PaymentRDO>>> payments(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(paymentService.studentPayments(student)));
	}

	@GetMapping("/{paymentId}")
	ResponseEntity<ApiEnvelope<PaymentRDO>> payment(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long paymentId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(paymentService.studentPayment(student, paymentId)));
	}
}
