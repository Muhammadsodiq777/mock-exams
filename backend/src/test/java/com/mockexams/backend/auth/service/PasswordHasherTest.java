package com.mockexams.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

	private final PasswordHasher passwordHasher = new PasswordHasher();

	@Test
	void matchesReturnsTrueForHashCreatedByHasher() {
		String storedHash = passwordHasher.hash("correct-password");

		assertThat(passwordHasher.matches("correct-password", storedHash)).isTrue();
		assertThat(passwordHasher.matches("wrong-password", storedHash)).isFalse();
	}

	@Test
	void matchesReturnsFalseForMalformedStoredHashes() {
		String salt = Base64.getEncoder().encodeToString(new byte[] { 1, 2, 3 });
		String hash = Base64.getEncoder().encodeToString(new byte[] { 4, 5, 6 });

		assertThat(passwordHasher.matches("password", null)).isFalse();
		assertThat(passwordHasher.matches("password", "plain-text-password")).isFalse();
		assertThat(passwordHasher.matches("password", "pbkdf2_sha256$0$" + salt + "$" + hash)).isFalse();
		assertThat(passwordHasher.matches("password", "pbkdf2_sha256$120000$$" + hash)).isFalse();
		assertThat(passwordHasher.matches("password", "pbkdf2_sha256$120000$" + salt + "$")).isFalse();
		assertThat(passwordHasher.matches("password", "pbkdf2_sha256$bad$" + salt + "$" + hash)).isFalse();
		assertThat(passwordHasher.matches("password", "pbkdf2_sha256$120000$not-base64$" + hash)).isFalse();
	}
}
