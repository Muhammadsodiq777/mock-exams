package com.mockexams.backend.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTeacherProfileControllerTest {

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
		jdbcTemplate.update("delete from marketplace_listing_items");
		jdbcTemplate.update("delete from purchases");
		jdbcTemplate.update("delete from subscriptions");
		jdbcTemplate.update("delete from marketplace_listings");
		jdbcTemplate.update("delete from question_answers");
		jdbcTemplate.update("delete from question_options");
		jdbcTemplate.update("delete from questions");
		jdbcTemplate.update("delete from listening_segments");
		jdbcTemplate.update("delete from reading_paragraphs");
		jdbcTemplate.update("delete from test_sections");
		jdbcTemplate.update("delete from tests");
		jdbcTemplate.update("delete from teacher_wallets");
		jdbcTemplate.update("delete from teacher_profiles");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
		jdbcTemplate.update("delete from files");
	}

	@Test
	void anonymousRequestIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/admin/teacher-profiles/1/suspend"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void nonAdminIsForbidden() throws Exception {
		TeacherRegistration teacher = approvedTeacher("forbidden");

		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/suspend", teacher.profileId())
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/reactivate", teacher.profileId())
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void suspendOnlyValidFromApprovedStatus() throws Exception {
		TeacherRegistration pending = registerTeacher("suspend-pending");
		String adminToken = registerAdmin(uniqueEmail("suspend-pending-admin"));

		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/suspend", pending.profileId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TEACHER_PROFILE_STATUS_CONFLICT"));
	}

	@Test
	void reactivateOnlyValidFromSuspendedStatus() throws Exception {
		TeacherRegistration teacher = approvedTeacher("reactivate-invalid");
		String adminToken = registerAdmin(uniqueEmail("reactivate-invalid-admin"));

		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/reactivate", teacher.profileId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TEACHER_PROFILE_STATUS_CONFLICT"));
	}

	@Test
	void listProfilesSupportsStatusFilterAndPagingMeta() throws Exception {
		TeacherRegistration pending = registerTeacher("list-pending");
		TeacherRegistration approved = approvedTeacher("list-approved");
		String adminToken = registerAdmin(uniqueEmail("list-profiles-admin"));

		mockMvc.perform(get("/api/v1/admin/teacher-profiles")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "pending_approval")
						.param("page", "0")
						.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(pending.profileId().intValue())))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(approved.profileId().intValue()))))
				.andExpect(jsonPath("$.meta.filters.status").value("pending_approval"))
				.andExpect(jsonPath("$.meta.page.number").value(0))
				.andExpect(jsonPath("$.meta.page.size").value(1))
				.andExpect(jsonPath("$.meta.page.totalElements").value(1))
				.andExpect(jsonPath("$.meta.sort[0].field").value("createdAt"))
				.andExpect(jsonPath("$.meta.sort[0].direction").value("desc"));
	}

	@Test
	void suspendHidesContentAndNotifiesThenReactivateRestoresVisibility() throws Exception {
		TeacherRegistration teacher = approvedTeacher("lifecycle");
		String adminToken = registerAdmin(uniqueEmail("lifecycle-admin"));
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Lifecycle Reading Test", "reading");
		Long listingId = createActiveListing(teacher.profileId(), testId, "Lifecycle Listing");

		mockMvc.perform(get("/api/v1/public/tests/{testId}", testId))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/public/marketplace"))
				.andExpect(jsonPath("$.data[*].id", hasItem(listingId.intValue())));

		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/suspend", teacher.profileId())
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "reason": "Repeated policy violations." }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("suspended"));

		mockMvc.perform(get("/api/v1/public/tests/{testId}", testId))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/public/marketplace"))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(listingId.intValue()))));

		Integer notificationCount = jdbcTemplate.queryForObject(
				"select count(*) from notifications where user_id = ? and type = 'account_status' and body like ?",
				Integer.class,
				teacher.userId(),
				"%suspended%");
		assertThat(notificationCount).isEqualTo(1);

		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/reactivate", teacher.profileId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("approved"));

		mockMvc.perform(get("/api/v1/public/tests/{testId}", testId))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/public/marketplace"))
				.andExpect(jsonPath("$.data[*].id", hasItem(listingId.intValue())));
	}

	@Test
	void adminDetailIncludesContentAndWalletSummaryWithoutSecrets() throws Exception {
		TeacherRegistration teacher = approvedTeacher("detail");
		String adminToken = registerAdmin(uniqueEmail("detail-admin"));
		createPublishedTest(teacher.userId(), teacher.profileId(), "Detail Published Test", "reading");
		createDraftTest(teacher.userId(), teacher.profileId(), "Detail Draft Test", "reading");

		String response = mockMvc.perform(get("/api/v1/admin/teacher-profiles/{profileId}", teacher.profileId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.profile.id").value(teacher.profileId()))
				.andExpect(jsonPath("$.data.totalTestCount").value(2))
				.andExpect(jsonPath("$.data.publishedTestCount").value(1))
				.andExpect(jsonPath("$.data.recentTests.length()").value(2))
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(response).doesNotContain("passwordHash");
		assertThat(response).doesNotContain("accessToken");
	}

	private Long createPublishedTest(Long userId, Long teacherProfileId, String title, String type) {
		return createTest(userId, teacherProfileId, title, type, "published");
	}

	private Long createDraftTest(Long userId, Long teacherProfileId, String title, String type) {
		return createTest(userId, teacherProfileId, title, type, "draft");
	}

	private Long createTest(Long userId, Long teacherProfileId, String title, String type, String visibility) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
					insert into tests (
					  teacher_profile_id, title, description, type, difficulty, estimated_band_min, estimated_band_max,
					  duration_minutes, price_amount, currency, is_free, language, visibility, published_at, created_by_user_id
					)
					values (?, ?, 'Admin fixture test.', ?, 'intermediate', 5.5, 7.0, 60, 0, 'UZS', true, 'en', ?, ?, ?)
					""", new String[] { "id" });
			statement.setObject(1, teacherProfileId);
			statement.setString(2, title);
			statement.setString(3, type);
			statement.setString(4, visibility);
			statement.setObject(5, "published".equals(visibility) ? OffsetDateTime.now(ZoneOffset.UTC) : null);
			statement.setLong(6, userId);
			return statement;
		}, keyHolder);
		return keyHolder.getKey().longValue();
	}

	private Long createActiveListing(Long teacherProfileId, Long testId, String title) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
					insert into marketplace_listings (
					  teacher_profile_id, test_id, title, description, listing_type, price_amount, currency, is_active, language
					)
					values (?, ?, ?, 'Listing fixture.', 'single_test', 0, 'UZS', true, 'en')
					""", new String[] { "id" });
			statement.setLong(1, teacherProfileId);
			statement.setLong(2, testId);
			statement.setString(3, title);
			return statement;
		}, keyHolder);
		return keyHolder.getKey().longValue();
	}

	private TeacherRegistration approvedTeacher(String prefix) throws Exception {
		TeacherRegistration teacher = registerTeacher(prefix);
		String adminToken = registerAdmin(uniqueEmail(prefix + "-approver"));
		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/approve", teacher.profileId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
		return teacher;
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
		return objectMapper.readTree(response).at("/data/accessToken").asText();
	}

	private String uniqueEmail(String prefix) {
		String compactPrefix = prefix.length() > 18 ? prefix.substring(0, 18) : prefix;
		String compactUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		return compactPrefix + "-" + compactUuid + "@example.test";
	}

	private record TeacherRegistration(String accessToken, Long userId, Long profileId) {
	}
}
