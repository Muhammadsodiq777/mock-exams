package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.mockexams.backend.auth.service.PasswordHasher;
import com.mockexams.backend.service.FileStorageService;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PromoCodeControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordHasher passwordHasher;

	@MockBean
	private FileStorageService fileStorageService;

	@BeforeEach
	void setUp() {
		cleanPromoFixtures();
		when(fileStorageService.createDownloadUrl(any()))
				.thenReturn("http://localhost:9000/mock-exams-test/signed-url");
	}

	@AfterEach
	void tearDown() {
		cleanPromoFixtures();
	}

	private void cleanPromoFixtures() {
		jdbcTemplate.update("delete from teacher_transactions");
		jdbcTemplate.update("delete from promo_code_redemptions");
		jdbcTemplate.update("delete from withdrawal_requests");
		jdbcTemplate.update("delete from teacher_wallets");
		jdbcTemplate.update("delete from subscriptions");
		jdbcTemplate.update("delete from purchases");
		jdbcTemplate.update("delete from payment_disputes");
		jdbcTemplate.update("delete from payments");
		jdbcTemplate.update("delete from promo_codes");
		jdbcTemplate.update("delete from marketplace_listing_items");
		jdbcTemplate.update("delete from marketplace_listings");
		jdbcTemplate.update("delete from study_plan_tasks");
		jdbcTemplate.update("delete from study_plans");
		jdbcTemplate.update("delete from test_results");
		jdbcTemplate.update("delete from student_answers");
		jdbcTemplate.update("delete from test_attempts");
		jdbcTemplate.update("delete from question_answers");
		jdbcTemplate.update("delete from question_options");
		jdbcTemplate.update("delete from questions");
		jdbcTemplate.update("delete from listening_segments");
		jdbcTemplate.update("delete from reading_paragraphs");
		jdbcTemplate.update("delete from test_sections");
		jdbcTemplate.update("delete from tests");
		jdbcTemplate.update("delete from files");
		jdbcTemplate.update("delete from teacher_profiles");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
	}

	@Test
	void teacherCreatesReadsAndUpdatesOwnPromoCode() throws Exception {
		TeacherFixture teacher = approvedTeacher("promo-own");

		JsonNode created = objectMapper.readTree(mockMvc.perform(post("/api/v1/teacher/promo-codes")
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "SumMer10",
								  "discountPercent": 10.00,
								  "usageLimit": 5
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.code").value("SumMer10"))
				.andExpect(jsonPath("$.data.teacherProfileId").value(teacher.profileId()))
				.andExpect(jsonPath("$.data.discountPercent").value(10.00))
				.andExpect(jsonPath("$.data.usageLimit").value(5))
				.andExpect(jsonPath("$.data.usedCount").value(0))
				.andExpect(jsonPath("$.data.isActive").value(true))
				.andReturn().getResponse().getContentAsString()).get("data");
		long promoCodeId = created.get("id").asLong();

		mockMvc.perform(get("/api/v1/teacher/promo-codes")
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(promoCodeId))
				.andExpect(jsonPath("$.meta.page.totalElements").value(1));

		mockMvc.perform(get("/api/v1/teacher/promo-codes/" + promoCodeId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.code").value("SumMer10"));

		mockMvc.perform(patch("/api/v1/teacher/promo-codes/" + promoCodeId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "usageLimit": 9,
								  "isActive": false
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.usageLimit").value(9))
				.andExpect(jsonPath("$.data.isActive").value(false))
				.andExpect(jsonPath("$.data.code").value("SumMer10"));

		mockMvc.perform(get("/api/v1/teacher/promo-codes")
						.header("Authorization", "Bearer " + teacher.accessToken())
						.param("active", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(0))
				.andExpect(jsonPath("$.meta.filters.active").value(true));
	}

	@Test
	void duplicateCodeIsRejectedCaseInsensitively() throws Exception {
		TeacherFixture first = approvedTeacher("promo-dup-a");
		TeacherFixture second = approvedTeacher("promo-dup-b");

		createTeacherPercentCode(first.accessToken(), "WINTER25", "25.00")
				.andExpect(status().isCreated());
		createTeacherPercentCode(second.accessToken(), "winter25", "10.00")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PROMO_CODE_DUPLICATE"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from promo_codes", Integer.class)).isOne();
	}

	@Test
	void promoCodeValidationRejectsInvalidShapes() throws Exception {
		TeacherFixture teacher = approvedTeacher("promo-shape");

		mockMvc.perform(post("/api/v1/teacher/promo-codes")
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "BOTH10",
								  "discountPercent": 10.00,
								  "discountAmount": 1000.00
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/teacher/promo-codes")
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "NONE10"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/teacher/promo-codes")
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "PCTCUR10",
								  "discountPercent": 10.00,
								  "currency": "UZS"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/teacher/promo-codes")
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "WINDOW10",
								  "discountPercent": 10.00,
								  "startsAt": "2026-08-01T00:00:00Z",
								  "expiresAt": "2026-07-01T00:00:00Z"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/teacher/promo-codes")
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "TOO-BIG",
								  "discountPercent": 150.00
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from promo_codes", Integer.class)).isZero();
	}

	@Test
	void usageLimitCannotGoBelowUsedCount() throws Exception {
		TeacherFixture teacher = approvedTeacher("promo-limit-floor");
		JsonNode created = objectMapper.readTree(createTeacherPercentCode(teacher.accessToken(), "FLOOR10", "10.00")
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString()).get("data");
		jdbcTemplate.update("update promo_codes set used_count = 2 where id = ?", created.get("id").asLong());

		mockMvc.perform(patch("/api/v1/teacher/promo-codes/" + created.get("id").asLong())
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "usageLimit": 1
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void teacherCannotAccessAnotherTeachersCode() throws Exception {
		TeacherFixture owner = approvedTeacher("promo-owner");
		TeacherFixture intruder = approvedTeacher("promo-intruder");
		JsonNode created = objectMapper.readTree(createTeacherPercentCode(owner.accessToken(), "MINE10", "10.00")
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString()).get("data");
		long promoCodeId = created.get("id").asLong();

		mockMvc.perform(get("/api/v1/teacher/promo-codes/" + promoCodeId)
						.header("Authorization", "Bearer " + intruder.accessToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("PROMO_CODE_NOT_FOUND"));

		mockMvc.perform(patch("/api/v1/teacher/promo-codes/" + promoCodeId)
						.header("Authorization", "Bearer " + intruder.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "isActive": false
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("PROMO_CODE_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/teacher/promo-codes")
						.header("Authorization", "Bearer " + intruder.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	void adminManagesPlatformAndTeacherScopedCodes() throws Exception {
		TeacherFixture teacher = approvedTeacher("promo-admin-scope");
		String adminToken = tokenForUser(createUserWithRole("promo-admin", "admin"));

		JsonNode platformCode = objectMapper.readTree(mockMvc.perform(post("/api/v1/admin/promo-codes")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "PLATFORM20",
								  "discountPercent": 20.00
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.teacherProfileId").doesNotExist())
				.andReturn().getResponse().getContentAsString()).get("data");

		mockMvc.perform(post("/api/v1/admin/promo-codes")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "teacherProfileId": %d,
								  "code": "SCOPED15",
								  "discountAmount": 1500.00,
								  "currency": "UZS"
								}
								""".formatted(teacher.profileId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.teacherProfileId").value(teacher.profileId()))
				.andExpect(jsonPath("$.data.discountAmount").value(1500.00))
				.andExpect(jsonPath("$.data.currency").value("UZS"));

		mockMvc.perform(get("/api/v1/admin/promo-codes")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2));

		mockMvc.perform(get("/api/v1/admin/promo-codes")
						.header("Authorization", "Bearer " + adminToken)
						.param("teacherProfileId", teacher.profileId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].code").value("SCOPED15"))
				.andExpect(jsonPath("$.meta.filters.teacherProfileId").value(teacher.profileId()));

		mockMvc.perform(patch("/api/v1/admin/promo-codes/" + platformCode.get("id").asLong())
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "isActive": false
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isActive").value(false));

		mockMvc.perform(get("/api/v1/admin/promo-codes")
						.header("Authorization", "Bearer " + adminToken)
						.param("active", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(platformCode.get("id").asLong()));

		mockMvc.perform(post("/api/v1/admin/promo-codes")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "teacherProfileId": 999999,
								  "code": "GHOST10",
								  "discountPercent": 10.00
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEACHER_PROFILE_NOT_FOUND"));
	}

	@Test
	void promoCodeEndpointsRequireProperRoles() throws Exception {
		TeacherFixture teacher = approvedTeacher("promo-roles");
		String studentToken = registerStudent(uniqueEmail("promo-role-student"));

		mockMvc.perform(post("/api/v1/teacher/promo-codes")
						.header("Authorization", "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "NOPE10",
								  "discountPercent": 10.00
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(get("/api/v1/admin/promo-codes")
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(get("/api/v1/teacher/promo-codes"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
	}

	private org.springframework.test.web.servlet.ResultActions createTeacherPercentCode(
			String accessToken,
			String code,
			String percent
	) throws Exception {
		return mockMvc.perform(post("/api/v1/teacher/promo-codes")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "code": "%s",
						  "discountPercent": %s
						}
						""".formatted(code, percent)));
	}

	private TeacherFixture approvedTeacher(String prefix) throws Exception {
		Long userId = createUserWithRole(prefix, "teacher");
		Long profileId = createTeacherProfile(userId);
		return new TeacherFixture(userId, profileId, tokenForUser(userId));
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

	private String tokenForUser(Long userId) throws Exception {
		String email = jdbcTemplate.queryForObject("select email from users where id = ?", String.class, userId);
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return accessTokenFrom(response);
	}

	private String accessTokenFrom(String response) throws Exception {
		JsonNode root = objectMapper.readTree(response);
		return root.at("/data/accessToken").asText();
	}

	private Long createUserWithRole(String prefix, String roleCode) {
		Long userId = insertAndReturnId("""
				insert into users (email, password_hash, full_name, status, preferred_language)
				values (?, ?, ?, 'active', 'en')
				""",
				uniqueEmail(prefix),
				passwordHasher.hash(PASSWORD),
				"Fixture " + prefix);
		Long roleId = jdbcTemplate.queryForObject("select id from roles where code = ?", Long.class, roleCode);
		jdbcTemplate.update("insert into user_roles (user_id, role_id) values (?, ?)", userId, roleId);
		return userId;
	}

	private Long createTeacherProfile(Long userId) {
		return insertAndReturnId("""
				insert into teacher_profiles (
				  user_id, display_name, headline, bio, status, approved_by_user_id, approved_at
				)
				values (?, ?, 'IELTS teacher', 'Experienced teacher.', 'approved', ?, ?)
				""",
				userId,
				"Teacher " + userId,
				userId,
				OffsetDateTime.now(ZoneOffset.UTC));
	}

	private Long insertAndReturnId(String sql, Object... args) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql, new String[] { "id" });
			for (int i = 0; i < args.length; i++) {
				statement.setObject(i + 1, args[i]);
			}
			return statement;
		}, keyHolder);
		return keyHolder.getKey().longValue();
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private record TeacherFixture(Long userId, Long profileId, String accessToken) {
	}
}
