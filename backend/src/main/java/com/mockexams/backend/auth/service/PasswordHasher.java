package com.mockexams.backend.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 120_000;
	private static final int SALT_BYTES = 16;
	private static final int KEY_BITS = 256;
	private static final String FORMAT = "pbkdf2_sha256";

	private final SecureRandom secureRandom = new SecureRandom();

	public String hash(String rawPassword) {
		byte[] salt = new byte[SALT_BYTES];
		secureRandom.nextBytes(salt);
		byte[] hash = pbkdf2(rawPassword, salt, ITERATIONS);
		return String.join("$",
				FORMAT,
				Integer.toString(ITERATIONS),
				Base64.getEncoder().encodeToString(salt),
				Base64.getEncoder().encodeToString(hash));
	}

	public boolean matches(String rawPassword, String storedHash) {
		if (storedHash == null) {
			return false;
		}
		try {
			String[] parts = storedHash.split("\\$");
			if (parts.length != 4 || !FORMAT.equals(parts[0])) {
				return false;
			}
			int iterations = Integer.parseInt(parts[1]);
			byte[] salt = Base64.getDecoder().decode(parts[2]);
			byte[] expected = Base64.getDecoder().decode(parts[3]);
			if (iterations <= 0 || salt.length == 0 || expected.length == 0) {
				return false;
			}
			byte[] actual = pbkdf2(rawPassword, salt, iterations);
			return MessageDigest.isEqual(expected, actual);
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private byte[] pbkdf2(String password, byte[] salt, int iterations) {
		try {
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
			return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not hash password", ex);
		}
	}
}
