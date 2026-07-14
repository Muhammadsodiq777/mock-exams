package com.mockexams.backend.service.payments;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.mockexams.backend.config.BackendEnvironmentProperties;
import com.mockexams.backend.error.BusinessException;

@Component
class MockPaymentProviderClient implements PaymentProviderClient {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private final BackendEnvironmentProperties properties;

	MockPaymentProviderClient(BackendEnvironmentProperties properties) {
		this.properties = properties;
	}

	@Override
	public String provider() {
		return properties.payments().provider().trim().toLowerCase(Locale.ROOT);
	}

	@Override
	public PaymentProviderCreateResult createPayment(PaymentProviderCreateRequest request) {
		if (!"mock".equals(provider())) {
			if (!properties.payments().enabled()) {
				throw new BusinessException(
						"PAYMENT_PROVIDER_DISABLED",
						"Payment provider is disabled.",
						HttpStatus.SERVICE_UNAVAILABLE);
			}
			throw new BusinessException(
					"SERVICE_UNAVAILABLE",
					"Configured payment provider adapter is not available.",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
		String providerPaymentId = "mock-" + UUID.randomUUID();
		return new PaymentProviderCreateResult(
				providerPaymentId,
				"mock://payments/" + providerPaymentId);
	}

	@Override
	public boolean verifyWebhookSignature(String rawBody, String signature) {
		if (signature == null || signature.isBlank()) {
			return false;
		}
		String expected = hmac(rawBody == null ? "" : rawBody, properties.payments().webhookSecret());
		return MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.UTF_8),
				signature.trim().getBytes(StandardCharsets.UTF_8));
	}

	private String hmac(String payload, String secret) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
			return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			throw new IllegalStateException("Payment webhook signature verification is unavailable.", ex);
		}
	}
}
