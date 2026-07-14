package com.mockexams.backend.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
class AdminUserControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanFixtures() {
		jdbcTemplate.update("delete from notifications");
		jdbcTemplate.update("delete from teacher_profiles");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
	}

	@Test
	void anonymousRequestIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void nonAdminIsForbidden() throws Exception {
		String studentToken = registerStudent(uniqueEmail("admin-forbidden"));

		mockMvc.perform(get("/api/v1/admin/users")
						.header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(patch("/api/v1/admin/users/1/status")
						.header("Authorization", "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "suspended" }
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void listSupportsStatusRoleAndEmailFiltersWithPagingMeta() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("list-admin"));
		Long targetStudentId = registerStudentAndReturnId(uniqueEmail("list-target-student"));
		Long otherStudentId = registerStudentAndReturnId(uniqueEmail("list-other-student"));
		jdbcTemplate.update("update users set status = 'suspended' where id = ?", targetStudentId);

		mockMvc.perform(get("/api/v1/admin/users")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "suspended")
						.param("role", "student"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", org.hamcrest.Matchers.hasItem(targetStudentId.intValue())))
				.andExpect(jsonPath("$.data[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(otherStudentId.intValue()))))
				.andExpect(jsonPath("$.meta.filters.status").value("suspended"))
				.andExpect(jsonPath("$.meta.filters.role").value("student"))
				.andExpect(jsonPath("$.meta.page.size").value(20))
				.andExpect(jsonPath("$.meta.page.number").value(0))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());

		mockMvc.perform(get("/api/v1/admin/users")
						.header("Authorization", "Bearer " + adminToken)
						.param("q", "list-target"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", org.hamcrest.Matchers.hasItem(targetStudentId.intValue())))
				.andExpect(jsonPath("$.data[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(otherStudentId.intValue()))));
	}

	@Test
	void listAndDetailExposeTeacherStatusAndSupportTeacherStatusFilter() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("teacher-status-admin"));
		TeacherRegistration approvedTeacher = registerTeacher("teacher-approved");
		TeacherRegistration pendingTeacher = registerTeacher("teacher-pending");
		Long studentId = registerStudentAndReturnId(uniqueEmail("teacher-status-student"));
		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/approve", approvedTeacher.profileId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/admin/users")
						.header("Authorization", "Bearer " + adminToken)
						.param("role", "teacher")
						.param("teacherStatus", "approved"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", org.hamcrest.Matchers.hasItem(approvedTeacher.userId().intValue())))
				.andExpect(jsonPath("$.data[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(pendingTeacher.userId().intValue()))))
				.andExpect(jsonPath("$.data[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(studentId.intValue()))))
				.andExpect(jsonPath("$.data[0].teacherStatus").value("approved"))
				.andExpect(jsonPath("$.meta.filters.role").value("teacher"))
				.andExpect(jsonPath("$.meta.filters.teacherStatus").value("approved"));

		mockMvc.perform(get("/api/v1/admin/users/{userId}", approvedTeacher.userId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.teacherStatus").value("approved"));

		mockMvc.perform(get("/api/v1/admin/users/{userId}", studentId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.teacherStatus").doesNotExist());
	}

	@Test
	void unsupportedStatusOrRoleFilterIsValidationFailed() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("filter-admin"));

		mockMvc.perform(get("/api/v1/admin/users")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "bogus"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(get("/api/v1/admin/users")
						.header("Authorization", "Bearer " + adminToken)
						.param("role", "bogus"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(get("/api/v1/admin/users")
						.header("Authorization", "Bearer " + adminToken)
						.param("teacherStatus", "bogus"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void detailReturnsSafeUserFieldsAndFailsWhenMissing() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("detail-admin"));
		Long studentId = registerStudentAndReturnId(uniqueEmail("detail-student"));

		String response = mockMvc.perform(get("/api/v1/admin/users/{userId}", studentId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(studentId))
				.andExpect(jsonPath("$.data.email").exists())
				.andExpect(jsonPath("$.data.roles[0]").value("student"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(response).doesNotContain("passwordHash");
		assertThat(response).doesNotContain("accessToken");
		assertThat(response).doesNotContain("refreshToken");

		mockMvc.perform(get("/api/v1/admin/users/{userId}", 999999L)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
	}

	@Test
	void suspendingUserNotifiesAndBlocksLoginThenReactivateRestoresLogin() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("suspend-admin"));
		String targetEmail = uniqueEmail("suspend-target");
		Long targetId = registerStudentAndReturnId(targetEmail);

		mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", targetId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "suspended", "reason": "policy violation" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("suspended"));

		Integer notificationCount = jdbcTemplate.queryForObject(
				"select count(*) from notifications where user_id = ? and type = 'account_status' and body like ?",
				Integer.class,
				targetId,
				"%suspended%");
		assertThat(notificationCount).isEqualTo(1);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "password": "%s" }
								""".formatted(targetEmail, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("ACCOUNT_INACTIVE"));

		mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", targetId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "active" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("active"));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "password": "%s" }
								""".formatted(targetEmail, PASSWORD)))
				.andExpect(status().isOk());
	}

	@Test
	void lastActiveAdminCannotBeBannedButSecondAdminCan() throws Exception {
		String firstAdminToken = registerAdmin(uniqueEmail("last-admin-one"));
		String secondAdminEmail = uniqueEmail("last-admin-two");
		Long secondAdminId = registerAdminAndReturnId(secondAdminEmail);

		mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", secondAdminId)
						.header("Authorization", "Bearer " + firstAdminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "banned" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("banned"));

		Long firstAdminId = userIdByEmail(firstAdminToken);
		mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", firstAdminId)
						.header("Authorization", "Bearer " + firstAdminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "banned" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("LAST_ADMIN_PROTECTED"));
	}

	private Long userIdByEmail(String accessToken) throws Exception {
		String response = mockMvc.perform(get("/api/v1/auth/me")
						.header("Authorization", "Bearer " + accessToken))
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).at("/data/id").asLong();
	}

	private String registerAdmin(String email) throws Exception {
		String accessToken = registerStudent(email);
		promoteToAdmin(email);
		return accessToken;
	}

	private Long registerAdminAndReturnId(String email) throws Exception {
		registerStudent(email);
		promoteToAdmin(email);
		return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
	}

	private void promoteToAdmin(String email) {
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select u.id, r.id
				from users u
				join roles r on r.code = 'admin'
				where u.email = ?
				""", email);
	}

	private Long registerStudentAndReturnId(String email) throws Exception {
		registerStudent(email);
		return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
	}

	private TeacherRegistration registerTeacher(String prefix) throws Exception {
		String email = uniqueEmail(prefix);
		String response = mockMvc.perform(post("/api/v1/auth/register/teacher")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Teacher Example",
								  "displayName": "Teacher Example",
								  "headline": "IELTS mentor",
								  "bio": "I help students prepare for IELTS."
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode root = objectMapper.readTree(response);
		return new TeacherRegistration(
				root.at("/data/accessToken").asText(),
				root.at("/data/user/id").asLong(),
				root.at("/data/user/teacherProfile/id").asLong());
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

	private String accessTokenFrom(String response) throws Exception {
		JsonNode root = objectMapper.readTree(response);
		return root.at("/data/accessToken").asText();
	}

	private String uniqueEmail(String prefix) {
		String compactPrefix = prefix.length() > 18 ? prefix.substring(0, 18) : prefix;
		String compactUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		return compactPrefix + "-" + compactUuid + "@example.test";
	}

	private record TeacherRegistration(String accessToken, Long userId, Long profileId) {
	}
}
