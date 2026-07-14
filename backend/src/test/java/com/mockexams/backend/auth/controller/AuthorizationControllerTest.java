package com.mockexams.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
class AuthorizationControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void studentCannotCallTeacherOnlyEndpoint() throws Exception {
		String accessToken = registerStudent(uniqueEmail("student-teacher-boundary"));

		mockMvc.perform(get("/api/v1/teacher/session")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	@Test
	void studentCanCallStudentOnlyEndpoint() throws Exception {
		String email = uniqueEmail("student-session");
		String accessToken = registerStudent(email);

		mockMvc.perform(get("/api/v1/student/session")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.roles", contains("student")))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	@Test
	void protectedEndpointWithoutBearerTokenReturnsAuthenticationRequired() throws Exception {
		mockMvc.perform(get("/api/v1/teacher/session"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	@Test
	void protectedEndpointWithMalformedBearerTokenReturnsTokenInvalid() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users")
						.header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"))
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	@Test
	void teacherCannotCallAdminOnlyEndpoint() throws Exception {
		String accessToken = registerTeacher(uniqueEmail("teacher-admin-boundary"));

		mockMvc.perform(get("/api/v1/admin/users")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	@Test
	void bannedUserTokenIsRejectedOnProtectedEndpoint() throws Exception {
		String email = uniqueEmail("banned-token");
		String accessToken = registerTeacher(email);
		jdbcTemplate.update("update users set status = 'banned' where email = ?", email);

		mockMvc.perform(get("/api/v1/teacher/session")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("ACCOUNT_INACTIVE"))
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	@Test
	void adminCanSuspendAndReactivateUser() throws Exception {
		String adminAccessToken = registerAdmin(uniqueEmail("admin-status"));
		String targetEmail = uniqueEmail("target-status");
		registerStudent(targetEmail);
		Long targetUserId = userIdFor(targetEmail);

		mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", targetUserId)
						.header("Authorization", "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "suspended"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(targetUserId))
				.andExpect(jsonPath("$.data.email").value(targetEmail))
				.andExpect(jsonPath("$.data.status").value("suspended"))
				.andExpect(jsonPath("$.data.roles", contains("student")))
				.andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());

		mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", targetUserId)
						.header("Authorization", "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "active"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(targetUserId))
				.andExpect(jsonPath("$.data.status").value("active"))
				.andExpect(jsonPath("$.data.roles", contains("student")));
	}

	@Test
	void adminStatusUpdateReturnsPersistedUpdatedAt() throws Exception {
		String adminAccessToken = registerAdmin(uniqueEmail("admin-updated-at"));
		String targetEmail = uniqueEmail("target-updated-at");
		registerStudent(targetEmail);
		Long targetUserId = userIdFor(targetEmail);

		String response = mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", targetUserId)
						.header("Authorization", "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "suspended"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		OffsetDateTime responseUpdatedAt = OffsetDateTime.parse(objectMapper.readTree(response)
				.at("/data/updatedAt")
				.asText());
		OffsetDateTime persistedUpdatedAt = jdbcTemplate.queryForObject(
				"select updated_at from users where id = ?",
				OffsetDateTime.class,
				targetUserId);
		assertThat(responseUpdatedAt).isEqualTo(persistedUpdatedAt);
	}

	@Test
	void adminCanDeleteAndReactivateUser() throws Exception {
		String adminAccessToken = registerAdmin(uniqueEmail("admin-delete"));
		String targetEmail = uniqueEmail("target-delete");
		registerStudent(targetEmail);
		Long targetUserId = userIdFor(targetEmail);

		mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", targetUserId)
						.header("Authorization", "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "deleted"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("deleted"))
				.andExpect(jsonPath("$.data.deletedAt").isNotEmpty());

		assertThat(deletedAtFor(targetUserId)).isNotNull();

		mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", targetUserId)
						.header("Authorization", "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "active"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("active"))
				.andExpect(jsonPath("$.data.deletedAt").doesNotExist());

		assertThat(deletedAtFor(targetUserId)).isNull();
	}

	@Test
	void adminStatusUpdateRejectsInvalidStatusWithValidationEnvelope() throws Exception {
		String adminAccessToken = registerAdmin(uniqueEmail("admin-invalid-status"));
		String targetEmail = uniqueEmail("target-invalid-status");
		registerStudent(targetEmail);
		Long targetUserId = userIdFor(targetEmail);

		mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", targetUserId)
						.header("Authorization", "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "archived"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fieldErrors[0].field").value("status"))
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	@Test
	void adminCanListUsersWithPaginationMetadata() throws Exception {
		String adminAccessToken = registerAdmin(uniqueEmail("admin-list"));
		String studentEmail = uniqueEmail("listed-student");
		registerStudent(studentEmail);

		mockMvc.perform(get("/api/v1/admin/users")
						.param("page", "0")
						.param("size", "1")
						.header("Authorization", "Bearer " + adminAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.meta.page.number").value(0))
				.andExpect(jsonPath("$.meta.page.size").value(1))
				.andExpect(jsonPath("$.meta.page.totalElements").value(greaterThanOrEqualTo(2)))
				.andExpect(jsonPath("$.meta.page.totalPages").value(greaterThanOrEqualTo(2)))
				.andExpect(jsonPath("$.meta.sort[0].field").value("createdAt"))
				.andExpect(jsonPath("$.meta.sort[0].direction").value("desc"))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	@Test
	void adminCanFindUserOnDefaultFirstPage() throws Exception {
		String adminAccessToken = registerAdmin(uniqueEmail("admin-list-default"));
		String studentEmail = uniqueEmail("listed-student-default");
		registerStudent(studentEmail);

		mockMvc.perform(get("/api/v1/admin/users")
						.header("Authorization", "Bearer " + adminAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].email", hasItem(studentEmail)))
				.andExpect(jsonPath("$.meta.page.size").value(20))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	@Test
	void adminListRejectsOversizedPageWithValidationEnvelope() throws Exception {
		String adminAccessToken = registerAdmin(uniqueEmail("admin-list-oversized"));

		mockMvc.perform(get("/api/v1/admin/users")
						.param("size", "101")
						.header("Authorization", "Bearer " + adminAccessToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fieldErrors[0].field").value("size"))
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	private String registerAdmin(String email) throws Exception {
		String accessToken = registerStudent(email);
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select u.id, r.id
				from users u
				join roles r on r.code = 'admin'
				where u.email = ?
				""", email);
		return accessToken;
	}

	private String registerStudent(String email) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/register/student")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Student Example"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return accessTokenFrom(response);
	}

	private String registerTeacher(String email) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/register/teacher")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Teacher Example",
								  "displayName": "Teacher Example"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return accessTokenFrom(response);
	}

	private String accessTokenFrom(String response) throws Exception {
		JsonNode root = objectMapper.readTree(response);
		return root.at("/data/accessToken").asText();
	}

	private Long userIdFor(String email) {
		return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
	}

	private OffsetDateTime deletedAtFor(Long userId) {
		return jdbcTemplate.queryForObject("select deleted_at from users where id = ?", OffsetDateTime.class, userId);
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}
}
