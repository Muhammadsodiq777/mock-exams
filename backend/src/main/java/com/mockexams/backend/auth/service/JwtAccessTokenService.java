package com.mockexams.backend.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.mockexams.backend.config.BackendEnvironmentProperties;
import com.mockexams.backend.error.BusinessException;

@Component
public class JwtAccessTokenService {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	private final BackendEnvironmentProperties properties;
	private final ObjectMapper objectMapper;

	public JwtAccessTokenService(BackendEnvironmentProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public IssuedAccessToken issue(Long userId, String email) {
		OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plus(properties.jwt().accessTokenTtl());
		long issuedAtEpochSecond = Instant.now().getEpochSecond();
		long expiresAtEpochSecond = expiresAt.toEpochSecond();
		String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
		String payload = encodeJson(Map.of(
				"iss", properties.jwt().issuer(),
				"sub", userId.toString(),
				"email", email,
				"typ", "access",
				"iat", issuedAtEpochSecond,
				"exp", expiresAtEpochSecond));
		String unsignedToken = header + "." + payload;
		String signature = sign(unsignedToken);
		return new IssuedAccessToken(unsignedToken + "." + signature, expiresAt);
	}

	public Long requireSubject(String token) {
		String[] parts = token.split("\\.", -1);
		if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
			throw BusinessException.tokenInvalid("Invalid access token.");
		}
		String unsignedToken = parts[0] + "." + parts[1];
		if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
			throw BusinessException.tokenInvalid("Invalid access token.");
		}
		validateHeader(decodeJson(parts[0]));
		Map<String, Object> claims = decodeJson(parts[1]);
		if (!properties.jwt().issuer().equals(claims.get("iss")) || !"access".equals(claims.get("typ"))) {
			throw BusinessException.tokenInvalid("Invalid access token.");
		}
		Object expiresAtClaim = claims.get("exp");
		Object subjectClaim = claims.get("sub");
		if (!(expiresAtClaim instanceof Number) || !(subjectClaim instanceof String subject)) {
			throw BusinessException.tokenInvalid("Invalid access token.");
		}
		long expiresAt = ((Number) expiresAtClaim).longValue();
		if (expiresAt <= Instant.now().getEpochSecond()) {
			throw new BusinessException("TOKEN_EXPIRED", "Access token expired.", HttpStatus.UNAUTHORIZED);
		}
		try {
			return Long.valueOf(subject);
		}
		catch (NumberFormatException ex) {
			throw BusinessException.tokenInvalid("Invalid access token.");
		}
	}

	private void validateHeader(Map<String, Object> header) {
		if (!"HS256".equals(header.get("alg")) || !"JWT".equals(header.get("typ"))) {
			throw BusinessException.tokenInvalid("Invalid access token.");
		}
	}

	private String encodeJson(Map<String, Object> value) {
		try {
			return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not encode JWT", ex);
		}
	}

	private Map<String, Object> decodeJson(String encoded) {
		try {
			byte[] json = BASE64_URL_DECODER.decode(encoded);
			return objectMapper.readValue(json, new TypeReference<>() {
			});
		}
		catch (Exception ex) {
			throw BusinessException.tokenInvalid("Invalid access token.");
		}
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(properties.jwt().secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
			return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not sign JWT", ex);
		}
	}

	private boolean constantTimeEquals(String left, String right) {
		return MessageDigestIsEqual.bytes(left, right);
	}

	public record IssuedAccessToken(String token, OffsetDateTime expiresAt) {
	}

	private static final class MessageDigestIsEqual {

		private MessageDigestIsEqual() {
		}

		static boolean bytes(String left, String right) {
			return java.security.MessageDigest.isEqual(
					left.getBytes(StandardCharsets.UTF_8),
					right.getBytes(StandardCharsets.UTF_8));
		}
	}
}
