package com.mockexams.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void registerStudentCreatesStudentRoleAndSession() throws Exception {
		String email = uniqueEmail("student");

		mockMvc.perform(post("/api/v1/auth/register/student")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Student Example",
								  "preferredLanguage": "en"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
				.andExpect(jsonPath("$.data.user.email").value(email))
				.andExpect(jsonPath("$.data.user.roles", contains("student")))
				.andExpect(jsonPath("$.data.user.teacherProfile").doesNotExist())
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());

		assertThat(countUserRole(email, "student")).isEqualTo(1);
		assertThat(passwordHashFor(email))
				.startsWith("pbkdf2_sha256$")
				.isNotEqualTo(PASSWORD);
	}

	@Test
	void registerTeacherCreatesTeacherRoleAndProfile() throws Exception {
		String email = uniqueEmail("teacher");

		mockMvc.perform(post("/api/v1/auth/register/teacher")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Teacher Example",
								  "preferredLanguage": "en",
								  "displayName": "Teacher Example",
								  "headline": "IELTS reading coach",
								  "bio": "I help students improve reading scores."
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.user.roles", contains("teacher")))
				.andExpect(jsonPath("$.data.user.teacherProfile.displayName").value("Teacher Example"))
				.andExpect(jsonPath("$.data.user.teacherProfile.status").value("pending_approval"));

		assertThat(countUserRole(email, "teacher")).isEqualTo(1);
		assertThat(countTeacherProfiles(email, "pending_approval")).isEqualTo(1);
	}

	@Test
	void duplicateEmailReturnsStableBusinessError() throws Exception {
		String email = uniqueEmail("duplicate");
		registerStudent(email, PASSWORD);

		mockMvc.perform(post("/api/v1/auth/register/student")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Duplicate Student"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"))
				.andExpect(jsonPath("$.error.message").isNotEmpty())
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	@Test
	void loginWithCorrectPasswordIssuesToken() throws Exception {
		String email = uniqueEmail("login");
		registerStudent(email, PASSWORD);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.user.email").value(email))
				.andExpect(jsonPath("$.data.user.roles", contains("student")));
	}

	@Test
	void loginWithWrongPasswordDoesNotIssueToken() throws Exception {
		String email = uniqueEmail("wrong-password");
		registerStudent(email, PASSWORD);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "bad-password"
								}
								""".formatted(email)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.data").doesNotExist())
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "suspended", "banned", "deleted" })
	void inactiveUsersCannotLogin(String status) throws Exception {
		String email = uniqueEmail(status);
		registerStudent(email, PASSWORD);
		jdbcTemplate.update("update users set status = ? where email = ?", status, email);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.data").doesNotExist())
				.andExpect(jsonPath("$.error.code").value("ACCOUNT_INACTIVE"));
	}

	@Test
	void meReturnsCurrentUserFromBearerToken() throws Exception {
		String email = uniqueEmail("me");
		String registrationResponse = mockMvc.perform(post("/api/v1/auth/register/teacher")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Current Teacher",
								  "displayName": "Current Teacher"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode root = objectMapper.readTree(registrationResponse);
		String accessToken = root.at("/data/accessToken").asText();

		mockMvc.perform(get("/api/v1/auth/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(root.at("/data/user/id").asLong()))
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.fullName").value("Current Teacher"))
				.andExpect(jsonPath("$.data.phone").doesNotExist())
				.andExpect(jsonPath("$.data.status").value("active"))
				.andExpect(jsonPath("$.data.preferredLanguage").value("en"))
				.andExpect(jsonPath("$.data.roles", hasItem("teacher")))
				.andExpect(jsonPath("$.data.teacherProfile.displayName").value("Current Teacher"))
				.andExpect(jsonPath("$.data.teacherProfile.status").value("pending_approval"))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	@Test
	void meCompatibilityRouteReturnsCurrentUserFromBearerToken() throws Exception {
		String email = uniqueEmail("me-compat");
		String registrationResponse = mockMvc.perform(post("/api/v1/auth/register/student")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Compat Student"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode root = objectMapper.readTree(registrationResponse);
		String accessToken = root.at("/data/accessToken").asText();

		mockMvc.perform(get("/api/auth/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.roles", contains("student")));
	}

	@Test
	void meWithMalformedBearerTokenReturnsTokenInvalid() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me")
						.header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"))
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	private void registerStudent(String email, String password) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register/student")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Student Example"
								}
								""".formatted(email, password)))
				.andExpect(status().isCreated());
	}

	private int countUserRole(String email, String roleCode) {
		return jdbcTemplate.queryForObject("""
				select count(*)
				from users u
				join user_roles ur on ur.user_id = u.id
				join roles r on r.id = ur.role_id
				where u.email = ? and r.code = ?
				""", Integer.class, email, roleCode);
	}

	private int countTeacherProfiles(String email, String status) {
		return jdbcTemplate.queryForObject("""
				select count(*)
				from users u
				join teacher_profiles tp on tp.user_id = u.id
				where u.email = ? and tp.status = ?
				""", Integer.class, email, status);
	}

	private String passwordHashFor(String email) {
		return jdbcTemplate.queryForObject("select password_hash from users where email = ?", String.class, email);
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}
}
