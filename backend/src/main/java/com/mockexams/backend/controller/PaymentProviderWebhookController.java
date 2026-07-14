package com.mockexams.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.dto.PaymentRDO;
import com.mockexams.backend.service.PaymentService;

@RestController
@RequestMapping("/api/v1/provider/payments")
class PaymentProviderWebhookController {

	private static final String SIGNATURE_HEADER = "X-MockExams-Payment-Signature";

	private final PaymentService paymentService;

	PaymentProviderWebhookController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping("/webhook")
	ResponseEntity<ApiEnvelope<PaymentRDO>> webhook(
			@RequestHeader(name = SIGNATURE_HEADER, required = false) String signature,
			@RequestBody String rawBody
	) {
		return ResponseEntity.ok(ApiEnvelope.of(paymentService.handleProviderWebhook(rawBody, signature)));
	}
}
