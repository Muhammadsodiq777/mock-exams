package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
class StudentProgressControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanProgressFixtures() {
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
		jdbcTemplate.update("delete from teacher_profiles");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
		jdbcTemplate.update("delete from files");
	}

	@Test
	void emptyStudentGetsRenderableProgressState() throws Exception {
		String accessToken = registerStudent(uniqueEmail("progress-empty"));
		Long authorId = createAdminUser("progress-empty-author");
		Long recommendationId = createTest(authorId, "First Reading Practice", "reading", "published", true);

		mockMvc.perform(get("/api/v1/student/progress")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.state").value("empty"))
				.andExpect(jsonPath("$.data.summary.completedAttemptCount").value(0))
				.andExpect(jsonPath("$.data.summary.overallBandEstimate").doesNotExist())
				.andExpect(jsonPath("$.data.summary.missingSkills[0]").value("listening"))
				.andExpect(jsonPath("$.data.summary.missingSkills[1]").value("reading"))
				.andExpect(jsonPath("$.data.skillHistories.reading").isEmpty())
				.andExpect(jsonPath("$.data.skillHistories.listening").isEmpty())
				.andExpect(jsonPath("$.data.recommendations[0].testId").value(recommendationId))
				.andExpect(jsonPath("$.data.recommendations[0].directlyAvailable").value(true))
				.andExpect(jsonPath("$.meta.asOf").isNotEmpty())
				.andExpect(jsonPath("$.meta.window.days").value(180));
	}

	@Test
	void dashboardSeparatesHistoriesAndAggregatesStoredResults() throws Exception {
		String studentEmail = uniqueEmail("progress-history");
		String accessToken = registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		Long authorId = createAdminUser("progress-history-author");
		Long readingTestId = createTest(authorId, "Reading Result", "reading", "published", true);
		Long listeningTestId = createTest(authorId, "Listening Result", "listening", "published", true);
		Long paidTestId = createTest(authorId, "Paid Listening", "listening", "published", false);
		Long rejectedTestId = createTest(authorId, "Rejected Reading", "reading", "rejected", true);

		Long oldReadingAttemptId = createAttempt(readingTestId, studentId, "submitted", OffsetDateTime.now(ZoneOffset.UTC).minusDays(14));
		Long oldReadingResultId = createResult(oldReadingAttemptId, "28.00", "40.00", "6.5", 28, 12, "{\"gap_fill\": 3}");
		Long latestReadingAttemptId = createAttempt(readingTestId, studentId, "submitted", OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));
		Long latestReadingResultId = createResult(latestReadingAttemptId, "32.00", "40.00", "7.0", 32, 8, "{\"gap_fill\": 2, \"matching_headings\": 1}");
		Long listeningAttemptId = createAttempt(listeningTestId, studentId, "expired", OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
		createResult(listeningAttemptId, "30.00", "40.00", "7.0", 30, 10, "{\"multiple_choice\": 4}");
		createAttempt(readingTestId, studentId, "in_progress", OffsetDateTime.now(ZoneOffset.UTC).minusHours(2));

		mockMvc.perform(get("/api/v1/student/progress")
						.header("Authorization", "Bearer " + accessToken)
						.param("days", "30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.state").value("active"))
				.andExpect(jsonPath("$.data.summary.completedAttemptCount").value(3))
				.andExpect(jsonPath("$.data.summary.overallBandEstimate").value(7.0))
				.andExpect(jsonPath("$.data.summary.skillBands.reading").value(7.0))
				.andExpect(jsonPath("$.data.summary.skillBands.listening").value(7.0))
				.andExpect(jsonPath("$.data.summary.missingSkills[0]").value("writing"))
				.andExpect(jsonPath("$.data.summary.missingSkills[1]").value("speaking"))
				.andExpect(jsonPath("$.data.skillHistories.reading[0].resultId").value(oldReadingResultId))
				.andExpect(jsonPath("$.data.skillHistories.reading[1].resultId").value(latestReadingResultId))
				.andExpect(jsonPath("$.data.skillHistories.listening[0].attemptStatus").value("expired"))
				.andExpect(jsonPath("$.data.weakAreas[0].questionType").value("multiple_choice"))
				.andExpect(jsonPath("$.data.weakAreas[0].incorrectCount").value(4))
				.andExpect(jsonPath("$.data.recentAttempts[0].attemptId").value(listeningAttemptId))
				.andExpect(jsonPath("$.data.recommendations[0].testId").value(listeningTestId))
				.andExpect(jsonPath("$.data.aiUsage.hardLimitReached").value(false))
				.andExpect(jsonPath("$.meta.window.days").value(30));

		Integer scoredAnswerRows = jdbcTemplate.queryForObject(
				"select count(*) from student_answers where is_correct is not null",
				Integer.class);
		assertThat(scoredAnswerRows).isZero();
		assertThat(paidTestId).isPositive();
		assertThat(rejectedTestId).isPositive();
	}

	@Test
	void progressKeepsLatestAndHistoryPerSkillWhenOneSkillCrowdsGlobalHistoryLimit() throws Exception {
		String studentEmail = uniqueEmail("progress-crowded-history");
		String accessToken = registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		Long authorId = createAdminUser("progress-crowded-history-author");
		Long readingTestId = createTest(authorId, "Crowded Reading Result", "reading", "published", true);
		Long listeningTestId = createTest(authorId, "Older Listening Result", "listening", "published", true);
		OffsetDateTime baseTime = OffsetDateTime.now(ZoneOffset.UTC).minusDays(3);

		Long listeningAttemptId = createAttempt(listeningTestId, studentId, "submitted", baseTime);
		Long listeningResultId = createResult(listeningAttemptId, "23.00", "40.00", "6.0", 23, 17, "{\"multiple_choice\": 1}");
		for (int index = 0; index < 101; index++) {
			Long readingAttemptId = createAttempt(
					readingTestId,
					studentId,
					"submitted",
					baseTime.plusHours(1).plusMinutes(index));
			createResult(readingAttemptId, "31.00", "40.00", "7.0", 31, 9, "{\"gap_fill\": 1}");
		}

		mockMvc.perform(get("/api/v1/student/progress")
						.header("Authorization", "Bearer " + accessToken)
						.param("days", "30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.state").value("active"))
				.andExpect(jsonPath("$.data.summary.completedAttemptCount").value(102))
				.andExpect(jsonPath("$.data.summary.skillBands.reading").value(7.0))
				.andExpect(jsonPath("$.data.summary.skillBands.listening").value(6.0))
				.andExpect(jsonPath("$.data.summary.missingSkills[0]").value("writing"))
				.andExpect(jsonPath("$.data.summary.missingSkills[1]").value("speaking"))
				.andExpect(jsonPath("$.data.skillHistories.reading.length()").value(100))
				.andExpect(jsonPath("$.data.skillHistories.listening.length()").value(1))
				.andExpect(jsonPath("$.data.skillHistories.listening[0].resultId").value(listeningResultId));
	}

	@Test
	void progressRequiresStudentRole() throws Exception {
		String teacherToken = registerTeacher(uniqueEmail("progress-teacher"));

		mockMvc.perform(get("/api/v1/student/progress")
						.header("Authorization", "Bearer " + teacherToken))
				.andExpect(status().isForbidden());
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
		JsonNode root = objectMapper.readTree(response);
		return root.at("/data/accessToken").asText();
	}

	private Long userIdForEmail(String email) {
		return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
	}

	private Long createAdminUser(String emailPrefix) {
		Long userId = createUser(emailPrefix, "Admin Fixture");
		assignRole(userId, "admin");
		return userId;
	}

	private Long createUser(String emailPrefix, String fullName) {
		return insertAndReturnId("""
				insert into users (email, password_hash, full_name, status, preferred_language)
				values (?, '$2a$10$6ibR7Y5HvfQCXkZEkQOBfO/3GCB3mF01R/ArQKq9s2MLwSj9Eg1wS', ?, 'active', 'en')
				""", uniqueEmail(emailPrefix), fullName);
	}

	private void assignRole(Long userId, String roleCode) {
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select ?, r.id
				from roles r
				where r.code = ?
				""", userId, roleCode);
	}

	private Long createTest(Long authorUserId, String title, String type, String visibility, boolean free) {
		BigDecimal price = free ? BigDecimal.ZERO : new BigDecimal("15000.00");
		return insertAndReturnId("""
				insert into tests (
				  teacher_profile_id, title, description, type, difficulty, estimated_band_min,
				  estimated_band_max, duration_minutes, price_amount, currency, is_free, language,
				  visibility, published_at, created_by_user_id
				)
				values (null, ?, 'Progress test fixture.', ?, 'intermediate', 5.5, 7.0, 60, ?, 'UZS', ?, 'en', ?, ?, ?)
				""",
				title,
				type,
				price,
				free,
				visibility,
				OffsetDateTime.now(ZoneOffset.UTC),
				authorUserId);
	}

	private Long createAttempt(Long testId, Long studentId, String status, OffsetDateTime startedAt) {
		OffsetDateTime submittedAt = "submitted".equals(status) || "expired".equals(status) ? startedAt.plusMinutes(45) : null;
		return insertAndReturnId("""
				insert into test_attempts (
				  test_id, student_user_id, status, started_at, submitted_at, expires_at, time_spent_seconds,
				  created_at, updated_at
				)
				values (?, ?, ?, ?, ?, ?, 2700, ?, ?)
				""",
				testId,
				studentId,
				status,
				startedAt,
				submittedAt,
				startedAt.plusMinutes(60),
				startedAt,
				submittedAt == null ? startedAt : submittedAt);
	}

	private Long createResult(
			Long attemptId,
			String rawScore,
			String maxScore,
			String estimatedBand,
			int correctCount,
			int incorrectCount,
			String weakQuestionTypes
	) {
		return insertAndReturnId("""
				insert into test_results (
				  attempt_id, raw_score, max_score, estimated_band, correct_count, incorrect_count,
				  weak_question_types, summary, created_at
				)
				values (?, ?, ?, ?, ?, ?, ? format json, '{}' format json, (
				  select updated_at from test_attempts where id = ?
				))
				""",
				attemptId,
				new BigDecimal(rawScore),
				new BigDecimal(maxScore),
				new BigDecimal(estimatedBand),
				correctCount,
				incorrectCount,
				weakQuestionTypes,
				attemptId);
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
}
