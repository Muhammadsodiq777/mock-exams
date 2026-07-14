package com.mockexams.backend.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
class BackendEnvironmentValidator implements InitializingBean {

	private static final String DEV_ONLY_PREFIX = "dev-only-";

	private final BackendEnvironmentProperties properties;

	BackendEnvironmentValidator(BackendEnvironmentProperties properties) {
		this.properties = properties;
	}

	@Override
	public void afterPropertiesSet() {
		List<String> errors = new ArrayList<>();

		requirePositiveDuration("app.jwt.access-token-ttl", properties.jwt().accessTokenTtl(), errors);
		requirePositiveDuration("app.jwt.refresh-token-ttl", properties.jwt().refreshTokenTtl(), errors);
		requireHttpUrl("app.minio.endpoint", properties.minio().endpoint(), errors);
		requireHttpUrl("app.openai.base-url", properties.openai().baseUrl(), errors);
		requireRedisUrl("app.redis.url", properties.redis().url(), errors);
		requireCorsOrigins(errors);
		requireTranscriptionSettings(errors);
		requireAiUsageSettings(errors);
		requireProviderSecretWhenEnabled(
				"OpenAI",
				"app.openai.enabled",
				properties.openai().enabled(),
				"app.openai.api-key",
				properties.openai().apiKey(),
				errors
		);
		requireProviderSecretWhenEnabled(
				"payment provider",
				"app.payments.enabled",
				properties.payments().enabled(),
				"app.payments.api-key",
				properties.payments().apiKey(),
				errors
		);
		requireProviderSecretWhenEnabled(
				"payment provider",
				"app.payments.enabled",
				properties.payments().enabled(),
				"app.payments.webhook-secret",
				properties.payments().webhookSecret(),
				errors
		);

		if (properties.payments().enabled() && isMockProvider(properties.payments().provider())) {
			errors.add("app.payments.provider must name a real provider when app.payments.enabled=true");
		}
		requireWritingAiProvider(errors);
		requireSpeakingAiProvider(errors);
		requireStudyPlanAiProvider(errors);

		if (!errors.isEmpty()) {
			throw new IllegalStateException(
					"Invalid backend environment configuration: " + String.join("; ", errors));
		}
	}

	private void requirePositiveDuration(String propertyName, Duration value, List<String> errors) {
		if (value.isZero() || value.isNegative()) {
			errors.add(propertyName + " must be a positive duration");
		}
	}

	private void requireCorsOrigins(List<String> errors) {
		for (String origin : properties.cors().allowedOrigins()) {
			requireHttpUrl("app.cors.allowed-origins", origin, errors);
		}
	}

	private void requireHttpUrl(String propertyName, String value, List<String> errors) {
		URI uri = parseUri(propertyName, value, errors);
		if (uri == null) {
			return;
		}
		String scheme = uri.getScheme();
		if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
			errors.add(propertyName + " must use http or https");
		}
		if (uri.getHost() == null) {
			errors.add(propertyName + " must include a host");
		}
	}

	private void requireRedisUrl(String propertyName, String value, List<String> errors) {
		URI uri = parseUri(propertyName, value, errors);
		if (uri == null) {
			return;
		}
		String scheme = uri.getScheme();
		if (!"redis".equalsIgnoreCase(scheme) && !"rediss".equalsIgnoreCase(scheme)) {
			errors.add(propertyName + " must use redis or rediss");
		}
		if (uri.getHost() == null) {
			errors.add(propertyName + " must include a host");
		}
	}

	private URI parseUri(String propertyName, String value, List<String> errors) {
		try {
			return new URI(value);
		}
		catch (URISyntaxException ex) {
			errors.add(propertyName + " must be a valid URI");
			return null;
		}
	}

	private void requireProviderSecretWhenEnabled(
			String providerName,
			String enabledPropertyName,
			boolean enabled,
			String secretPropertyName,
			String secretValue,
			List<String> errors
	) {
		if (enabled && secretValue.toLowerCase(Locale.ROOT).startsWith(DEV_ONLY_PREFIX)) {
			errors.add(secretPropertyName + " must be set to a real " + providerName
					+ " value when " + enabledPropertyName + "=true");
		}
	}

	private boolean isMockProvider(String provider) {
		return "mock".equalsIgnoreCase(provider) || provider.toLowerCase(Locale.ROOT).startsWith("dev-only");
	}

	private void requireAiUsageSettings(List<String> errors) {
		BackendEnvironmentProperties.Usage usage = properties.ai().usage();
		if (!"monthly".equalsIgnoreCase(usage.period())) {
			errors.add("app.ai.usage.period must be monthly");
		}
		if (usage.hardLimitRequests() != null && usage.hardLimitRequests() < usage.freeRequestsLimit()) {
			errors.add("app.ai.usage.hard-limit-requests must be greater than or equal to app.ai.usage.free-requests-limit");
		}
	}

	private void requireTranscriptionSettings(List<String> errors) {
		BackendEnvironmentProperties.Transcription transcription = properties.transcription();
		String provider = transcription.provider().toLowerCase(Locale.ROOT);
		if (!"mock".equals(provider) && !"openai".equals(provider)) {
			errors.add("app.transcription.provider must be mock or openai");
		}
		if (transcription.maxAudioDuration().isZero() || transcription.maxAudioDuration().isNegative()) {
			errors.add("app.transcription.max-audio-duration must be a positive duration");
		}
		if ("openai".equals(provider) && !properties.openai().enabled()) {
			errors.add("app.openai.enabled must be true when app.transcription.provider=openai");
		}
	}

	private void requireWritingAiProvider(List<String> errors) {
		String provider = properties.ai().writing().provider().toLowerCase(Locale.ROOT);
		if (!"mock".equals(provider) && !"openai".equals(provider)) {
			errors.add("app.ai.writing.provider must be mock or openai");
		}
		if ("openai".equals(provider) && !properties.openai().enabled()) {
			errors.add("app.openai.enabled must be true when app.ai.writing.provider=openai");
		}
	}

	private void requireSpeakingAiProvider(List<String> errors) {
		String provider = properties.ai().speaking().provider().toLowerCase(Locale.ROOT);
		if (!"mock".equals(provider) && !"openai".equals(provider)) {
			errors.add("app.ai.speaking.provider must be mock or openai");
		}
		if ("openai".equals(provider) && !properties.openai().enabled()) {
			errors.add("app.openai.enabled must be true when app.ai.speaking.provider=openai");
		}
	}

	private void requireStudyPlanAiProvider(List<String> errors) {
		String provider = properties.ai().studyPlan().provider().toLowerCase(Locale.ROOT);
		if (!"mock".equals(provider) && !"openai".equals(provider)) {
			errors.add("app.ai.study-plan.provider must be mock or openai");
		}
		if ("openai".equals(provider) && !properties.openai().enabled()) {
			errors.add("app.openai.enabled must be true when app.ai.study-plan.provider=openai");
		}
	}
}
