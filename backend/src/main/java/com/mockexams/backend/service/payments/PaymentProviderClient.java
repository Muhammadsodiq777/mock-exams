package com.mockexams.backend.service.payments;

public interface PaymentProviderClient {

	String provider();

	PaymentProviderCreateResult createPayment(PaymentProviderCreateRequest request);

	boolean verifyWebhookSignature(String rawBody, String signature);
}
