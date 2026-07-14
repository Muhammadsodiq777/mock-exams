package com.mockexams.backend.service.payments;

public record PaymentProviderCreateResult(
		String providerPaymentId,
		String checkoutUrl
) {
}
