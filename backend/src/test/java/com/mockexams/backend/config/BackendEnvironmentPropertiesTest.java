package com.mockexams.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class BackendEnvironmentPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
			.withUserConfiguration(TestConfig.class)
			.withPropertyValues(validProperties());

	@Test
	void bindsValidLocalConfiguration() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(BackendEnvironmentProperties.class).cors().allowedOrigins())
					.containsExactly("http://localhost:5173", "http://127.0.0.1:5173");
		});
	}

	@Test
	void failsWhenJwtSecretIsTooShort() {
		contextRunner
				.withPropertyValues("app.jwt.secret=short")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void failsWhenCorsOriginIsNotHttpUrl() {
		contextRunner
				.withPropertyValues("app.cors.allowed-origins=localhost:5173")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasMessageContaining("app.cors.allowed-origins must use http or https");
				});
	}

	@Test
	void failsWhenOpenAiEnabledWithDevOnlyKey() {
		contextRunner
				.withPropertyValues("app.openai.enabled=true")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasMessageContaining("app.openai.api-key must be set to a real OpenAI value");
				});
	}

	@Test
	void failsWhenAiHardLimitIsBelowFreeLimit() {
		contextRunner
				.withPropertyValues("app.ai.usage.hard-limit-requests=4")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasMessageContaining("app.ai.usage.hard-limit-requests must be greater than or equal");
				});
	}

	@Test
	void failsWhenPaymentsEnabledWithMockProvider() {
		contextRunner
				.withPropertyValues(
						"app.payments.enabled=true",
						"app.payments.api-key=pay_real_local_test_key",
						"app.payments.webhook-secret=pay_real_local_test_webhook_secret"
				)
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasMessageContaining("app.payments.provider must name a real provider");
				});
	}

	private String[] validProperties() {
		return new String[] {
				"app.cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5173",
				"app.jwt.issuer=mock-exams-test",
				"app.jwt.secret=test-only-change-me-local-jwt-secret-32-chars",
				"app.jwt.access-token-ttl=PT1H",
				"app.jwt.refresh-token-ttl=P7D",
				"app.minio.endpoint=http://localhost:9000",
				"app.minio.bucket=mock-exams-test",
				"app.minio.access-key=minioadmin",
				"app.minio.secret-key=minioadmin",
				"app.redis.url=redis://localhost:6379",
				"app.openai.enabled=false",
				"app.openai.api-key=dev-only-openai-api-key",
				"app.openai.model=gpt-4.1-mini",
				"app.openai.base-url=https://api.openai.com/v1",
				"app.transcription.provider=mock",
				"app.transcription.model=mock-transcriber",
				"app.transcription.max-audio-duration=PT5M",
				"app.transcription.cost-per-minute=0.000000",
				"app.ai.usage.period=monthly",
				"app.ai.usage.free-requests-limit=20",
				"app.ai.usage.hard-limit-requests=50",
				"app.ai.usage.count-provider-failures=false",
				"app.ai.usage.prompt-token-cost-per-1k=0.000000",
				"app.ai.usage.completion-token-cost-per-1k=0.000000",
				"app.ai.writing.provider=mock",
				"app.ai.speaking.provider=mock",
				"app.ai.study-plan.provider=mock",
				"app.payments.enabled=false",
				"app.payments.provider=mock",
				"app.payments.api-key=dev-only-payment-api-key",
				"app.payments.webhook-secret=dev-only-payment-webhook-secret",
				"app.google.client-id=dev-only-google-client-id",
				"app.google.client-secret=dev-only-google-client-secret",
				"app.google.platform-calendar-id=dev-only-google-calendar-id"
		};
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(BackendEnvironmentProperties.class)
	static class TestConfig {

		@Bean
		BackendEnvironmentValidator backendEnvironmentValidator(BackendEnvironmentProperties properties) {
			return new BackendEnvironmentValidator(properties);
		}
	}
}
