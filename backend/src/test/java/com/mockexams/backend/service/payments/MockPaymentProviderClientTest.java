package com.mockexams.backend.service.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.mockexams.backend.config.BackendEnvironmentProperties;
import com.mockexams.backend.error.BusinessException;

class MockPaymentProviderClientTest {

	@Test
	void disabledRealProviderCannotCreatePayment() {
		MockPaymentProviderClient client = new MockPaymentProviderClient(properties(false, "payme"));

		assertThatThrownBy(() -> client.createPayment(new PaymentProviderCreateRequest(
				1L,
				2L,
				new BigDecimal("25000.00"),
				"UZS",
				"Listing",
				null)))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("PAYMENT_PROVIDER_DISABLED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
				});
	}

	private BackendEnvironmentProperties properties(boolean paymentEnabled, String paymentProvider) {
		return new BackendEnvironmentProperties(
				new BackendEnvironmentProperties.Cors(List.of("http://localhost:5173")),
				new BackendEnvironmentProperties.Jwt(
						"mock-exams-test",
						"dev-only-change-me-local-jwt-secret-32-chars",
						Duration.ofHours(1),
						Duration.ofDays(7)),
				new BackendEnvironmentProperties.Minio(
						"http://localhost:9000",
						"mock-exams-local",
						"minioadmin",
						"minioadmin"),
				new BackendEnvironmentProperties.Redis("redis://localhost:6379"),
				new BackendEnvironmentProperties.OpenAi(
						false,
						"dev-only-openai-api-key",
						"gpt-4.1-mini",
						"https://api.openai.com/v1"),
				new BackendEnvironmentProperties.Transcription(
						"mock",
						"gpt-4o-mini-transcribe",
						Duration.ofMinutes(5),
						BigDecimal.ZERO),
				new BackendEnvironmentProperties.Ai(
						new BackendEnvironmentProperties.Usage(
								"monthly",
								20,
								50,
								false,
								BigDecimal.ZERO,
								BigDecimal.ZERO),
						new BackendEnvironmentProperties.Writing("mock"),
						new BackendEnvironmentProperties.Speaking("mock"),
						new BackendEnvironmentProperties.StudyPlan("mock")),
				new BackendEnvironmentProperties.Payments(
						paymentEnabled,
						paymentProvider,
						"dev-only-payment-api-key",
						"dev-only-payment-webhook-secret"),
				new BackendEnvironmentProperties.Google(
						"dev-only-google-client-id",
						"dev-only-google-client-secret",
						"dev-only-google-calendar-id"));
	}
}
