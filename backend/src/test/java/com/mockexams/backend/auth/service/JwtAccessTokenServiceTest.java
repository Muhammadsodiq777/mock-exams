package com.mockexams.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import com.mockexams.backend.config.BackendEnvironmentProperties;
import com.mockexams.backend.error.BusinessException;

class JwtAccessTokenServiceTest {

	private static final String ISSUER = "mock-exams-test";
	private static final String SECRET = "test-only-change-me-local-jwt-secret-32-chars";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final JwtAccessTokenService jwtAccessTokenService = new JwtAccessTokenService(properties(), OBJECT_MAPPER);

	@Test
	void issuedTokenReturnsSubject() {
		JwtAccessTokenService.IssuedAccessToken token = jwtAccessTokenService.issue(42L, "student@example.test");

		assertThat(jwtAccessTokenService.requireSubject(token.token())).isEqualTo(42L);
	}

	@Test
	void requireSubjectRejectsUnsupportedHeader() {
		String token = signedToken(
				Map.of("alg", "none", "typ", "JWT"),
				accessClaims(Map.of()));

		assertTokenError(token, "TOKEN_INVALID");
	}

	@Test
	void requireSubjectRejectsWrongIssuer() {
		String token = signedToken(
				Map.of("alg", "HS256", "typ", "JWT"),
				accessClaims(Map.of("iss", "other-issuer")));

		assertTokenError(token, "TOKEN_INVALID");
	}

	@Test
	void requireSubjectRejectsWrongPayloadTokenType() {
		String token = signedToken(
				Map.of("alg", "HS256", "typ", "JWT"),
				accessClaims(Map.of("typ", "refresh")));

		assertTokenError(token, "TOKEN_INVALID");
	}

	@Test
	void requireSubjectRejectsExpiredToken() {
		String token = signedToken(
				Map.of("alg", "HS256", "typ", "JWT"),
				accessClaims(Map.of("exp", Instant.now().minusSeconds(60).getEpochSecond())));

		assertTokenError(token, "TOKEN_EXPIRED");
	}

	private void assertTokenError(String token, String code) {
		assertThatThrownBy(() -> jwtAccessTokenService.requireSubject(token))
				.isInstanceOfSatisfying(BusinessException.class,
						ex -> assertThat(ex.getCode()).isEqualTo(code));
	}

	private Map<String, Object> accessClaims(Map<String, Object> overrides) {
		Map<String, Object> claims = new java.util.HashMap<>();
		claims.put("iss", ISSUER);
		claims.put("sub", "42");
		claims.put("email", "student@example.test");
		claims.put("typ", "access");
		claims.put("iat", Instant.now().getEpochSecond());
		claims.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());
		claims.putAll(overrides);
		return claims;
	}

	private String signedToken(Map<String, Object> header, Map<String, Object> payload) {
		String unsignedToken = encode(header) + "." + encode(payload);
		return unsignedToken + "." + sign(unsignedToken);
	}

	private String encode(Map<String, Object> value) {
		try {
			return Base64.getUrlEncoder().withoutPadding()
					.encodeToString(OBJECT_MAPPER.writeValueAsBytes(value));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not encode token test value", ex);
		}
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return Base64.getUrlEncoder().withoutPadding()
					.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not sign token test value", ex);
		}
	}

	private BackendEnvironmentProperties properties() {
		return new BackendEnvironmentProperties(
				new BackendEnvironmentProperties.Cors(List.of("http://localhost:5173")),
				new BackendEnvironmentProperties.Jwt(ISSUER, SECRET, Duration.ofHours(1), Duration.ofDays(7)),
				new BackendEnvironmentProperties.Minio("http://localhost:9000", "mock-exams-test", "minioadmin",
						"minioadmin"),
				new BackendEnvironmentProperties.Redis("redis://localhost:6379"),
				new BackendEnvironmentProperties.OpenAi(false, "dev-only-openai-api-key", "gpt-4.1-mini",
						"https://api.openai.com/v1"),
				new BackendEnvironmentProperties.Transcription(
						"mock", "mock-transcriber", Duration.ofMinutes(5), BigDecimal.ZERO),
				new BackendEnvironmentProperties.Ai(
						new BackendEnvironmentProperties.Usage(
								"monthly", 20, 50, false, BigDecimal.ZERO, BigDecimal.ZERO),
						new BackendEnvironmentProperties.Writing("mock"),
						new BackendEnvironmentProperties.Speaking("mock"),
						new BackendEnvironmentProperties.StudyPlan("mock")),
				new BackendEnvironmentProperties.Payments(false, "mock", "dev-only-payment-api-key",
						"dev-only-payment-webhook-secret"),
				new BackendEnvironmentProperties.Google(
						"dev-only-google-client-id",
						"dev-only-google-client-secret",
						"dev-only-google-calendar-id"));
	}
}
