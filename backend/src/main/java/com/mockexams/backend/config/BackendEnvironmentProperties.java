package com.mockexams.backend.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record BackendEnvironmentProperties(
		@Valid @NotNull Cors cors,
		@Valid @NotNull Jwt jwt,
		@Valid @NotNull Minio minio,
		@Valid @NotNull Redis redis,
		@Valid @NotNull OpenAi openai,
		@Valid @NotNull Transcription transcription,
		@Valid @NotNull Ai ai,
		@Valid @NotNull Payments payments,
		@Valid @NotNull Google google
) {

	public record Cors(
			@NotEmpty List<@NotBlank String> allowedOrigins
	) {

		public Cors {
			allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
		}
	}

	public record Jwt(
			@NotBlank String issuer,
			@NotBlank @Size(min = 32) String secret,
			@NotNull Duration accessTokenTtl,
			@NotNull Duration refreshTokenTtl
	) {
	}

	public record Minio(
			@NotBlank String endpoint,
			@NotBlank String bucket,
			@NotBlank String accessKey,
			@NotBlank String secretKey
	) {
	}

	public record Redis(
			@NotBlank String url
	) {
	}

	public record OpenAi(
			boolean enabled,
			@NotBlank String apiKey,
			@NotBlank String model,
			@NotBlank String baseUrl
	) {
	}

	public record Transcription(
			@NotBlank String provider,
			@NotBlank String model,
			@NotNull Duration maxAudioDuration,
			@NotNull @DecimalMin("0.000000") BigDecimal costPerMinute
	) {
	}

	public record Ai(
			@Valid @NotNull Usage usage,
			@Valid @NotNull Writing writing,
			@Valid @NotNull Speaking speaking,
			@Valid @NotNull StudyPlan studyPlan
	) {
	}

	public record Writing(
			@NotBlank String provider
	) {
	}

	public record Speaking(
			@NotBlank String provider
	) {
	}

	public record StudyPlan(
			@NotBlank String provider
	) {
	}

	public record Usage(
			@NotBlank String period,
			@Min(0) int freeRequestsLimit,
			@Min(0) Integer hardLimitRequests,
			boolean countProviderFailures,
			@NotNull @DecimalMin("0.000000") BigDecimal promptTokenCostPer1k,
			@NotNull @DecimalMin("0.000000") BigDecimal completionTokenCostPer1k
	) {
	}

	public record Payments(
			boolean enabled,
			@NotBlank String provider,
			@NotBlank String apiKey,
			@NotBlank String webhookSecret
	) {
	}

	public record Google(
			@NotBlank String clientId,
			@NotBlank String clientSecret,
			@NotBlank String platformCalendarId
	) {
	}
}
