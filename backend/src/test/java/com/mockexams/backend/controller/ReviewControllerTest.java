package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.mockexams.backend.auth.service.PasswordHasher;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordHasher passwordHasher;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("delete from reviews");
		jdbcTemplate.update("delete from teacher_transactions");
		jdbcTemplate.update("delete from promo_code_redemptions");
		jdbcTemplate.update("delete from subscriptions");
		jdbcTemplate.update("delete from purchases");
		jdbcTemplate.update("delete from payments");
		jdbcTemplate.update("delete from marketplace_listing_items");
		jdbcTemplate.update("delete from marketplace_listings");
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
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
	}

	@Test
	void studentCannotReviewContentTheyNeverAccessed() throws Exception {
		StudentFixture student = student("never-accessed");
		TeacherFixture teacher = approvedTeacher("never-accessed-teacher");
		Long testId = publishedTest(teacher, "Untouched Reading");

		createReview(student.token(), testId, 5, "Great test.")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("REVIEW_NOT_ELIGIBLE"));
	}

	@Test
	void studentCanReviewAfterSubmittedAttemptAndTeacherAggregateUpdates() throws Exception {
		StudentFixture student = student("attempted");
		TeacherFixture teacher = approvedTeacher("attempted-teacher");
		Long testId = publishedTest(teacher, "Attempted Reading");
		submittedAttempt(student.userId(), testId);

		createReview(student.token(), testId, 4, "Solid practice test.")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.testId").value(testId))
				.andExpect(jsonPath("$.data.rating").value(4))
				.andExpect(jsonPath("$.data.comment").value("Solid practice test."));

		assertThat(teacherRatingAverage(teacher.profileId())).isEqualByComparingTo("4.00");
		assertThat(teacherRatingCount(teacher.profileId())).isEqualTo(1);
	}

	@Test
	void studentCanReviewAfterPurchaseWithoutAttempt() throws Exception {
		StudentFixture student = student("purchased");
		TeacherFixture teacher = approvedTeacher("purchased-teacher");
		Long testId = publishedTest(teacher, "Purchased Reading");
		Long listingId = activeListing(teacher.profileId(), testId, new BigDecimal("15000.00"));
		grantedPurchase(student.userId(), listingId);

		createReview(student.token(), testId, 5, "Worth buying.")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.rating").value(5));
	}

	@Test
	void inProgressAttemptDoesNotGrantReviewEligibility() throws Exception {
		StudentFixture student = student("in-progress");
		TeacherFixture teacher = approvedTeacher("in-progress-teacher");
		Long testId = publishedTest(teacher, "In Progress Reading");
		attempt(student.userId(), testId, "in_progress", null);

		createReview(student.token(), testId, 4, null)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("REVIEW_NOT_ELIGIBLE"));
	}

	@Test
	void ratingMustBeWithinAllowedRange() throws Exception {
		StudentFixture student = student("rating-range");
		TeacherFixture teacher = approvedTeacher("rating-range-teacher");
		Long testId = publishedTest(teacher, "Rating Range Reading");
		submittedAttempt(student.userId(), testId);

		createReview(student.token(), testId, 0, null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
		createReview(student.token(), testId, 6, null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void overlongCommentIsRejected() throws Exception {
		StudentFixture student = student("long-comment");
		TeacherFixture teacher = approvedTeacher("long-comment-teacher");
		Long testId = publishedTest(teacher, "Long Comment Reading");
		submittedAttempt(student.userId(), testId);

		createReview(student.token(), testId, 4, "x".repeat(2001))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void duplicateReviewIsRejectedAndExplicitUpdateEndpointWorks() throws Exception {
		StudentFixture student = student("duplicate");
		TeacherFixture teacher = approvedTeacher("duplicate-teacher");
		Long testId = publishedTest(teacher, "Duplicate Reading");
		submittedAttempt(student.userId(), testId);

		Long reviewId = createdReviewId(student.token(), testId, 5, "First impression.");
		createReview(student.token(), testId, 3, "Second try.")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("REVIEW_ALREADY_EXISTS"));

		mockMvc.perform(patch("/api/v1/student/reviews/{reviewId}", reviewId)
						.header("Authorization", "Bearer " + student.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\": 2, \"comment\": \"Changed my mind.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(reviewId))
				.andExpect(jsonPath("$.data.rating").value(2))
				.andExpect(jsonPath("$.data.comment").value("Changed my mind."));

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from reviews where student_user_id = ?", Integer.class, student.userId())).isOne();
		assertThat(teacherRatingAverage(teacher.profileId())).isEqualByComparingTo("2.00");
		assertThat(teacherRatingCount(teacher.profileId())).isEqualTo(1);
	}

	@Test
	void studentCannotUpdateAnotherStudentsReview() throws Exception {
		StudentFixture owner = student("update-owner");
		StudentFixture other = student("update-other");
		TeacherFixture teacher = approvedTeacher("update-owner-teacher");
		Long testId = publishedTest(teacher, "Owned Review Reading");
		submittedAttempt(owner.userId(), testId);
		Long reviewId = createdReviewId(owner.token(), testId, 5, null);

		mockMvc.perform(patch("/api/v1/student/reviews/{reviewId}", reviewId)
						.header("Authorization", "Bearer " + other.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\": 1}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("REVIEW_NOT_FOUND"));

		assertThat(jdbcTemplate.queryForObject(
				"select rating from reviews where id = ?", Integer.class, reviewId)).isEqualTo(5);
	}

	@Test
	void studentCannotReviewDraftTest() throws Exception {
		StudentFixture student = student("draft-review");
		TeacherFixture teacher = approvedTeacher("draft-review-teacher");
		Long testId = draftTest(teacher, "Draft Reading");
		submittedAttempt(student.userId(), testId);

		createReview(student.token(), testId, 4, null)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEST_NOT_FOUND"));
	}

	@Test
	void publicReviewListsExposeOnlySafeDisplayInformation() throws Exception {
		StudentFixture student = student("public-safe");
		TeacherFixture teacher = approvedTeacher("public-safe-teacher");
		Long testId = publishedTest(teacher, "Public Safe Reading");
		submittedAttempt(student.userId(), testId);
		createdReviewId(student.token(), testId, 5, "Loved it.");

		mockMvc.perform(get("/api/v1/public/tests/{testId}/reviews", testId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].rating").value(5))
				.andExpect(jsonPath("$.data[0].comment").value("Loved it."))
				.andExpect(jsonPath("$.data[0].studentDisplayName").value("Student public-safe"))
				.andExpect(jsonPath("$.data[0].studentUserId").doesNotExist())
				.andExpect(jsonPath("$.data[0].email").doesNotExist())
				.andExpect(jsonPath("$.meta.page.totalElements").value(1));

		mockMvc.perform(get("/api/v1/public/teacher-profiles/{profileId}/reviews", teacher.profileId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].testId").value(testId))
				.andExpect(jsonPath("$.data[0].teacherProfileId").value(teacher.profileId()));
	}

	@Test
	void publicTeacherReviewsSupportRatingAndTestFilters() throws Exception {
		StudentFixture first = student("filters-one");
		StudentFixture second = student("filters-two");
		TeacherFixture teacher = approvedTeacher("filters-teacher");
		Long readingId = publishedTest(teacher, "Filters Reading");
		Long listeningId = publishedTest(teacher, "Filters Listening");
		submittedAttempt(first.userId(), readingId);
		submittedAttempt(second.userId(), listeningId);
		createdReviewId(first.token(), readingId, 5, null);
		createdReviewId(second.token(), listeningId, 3, null);

		mockMvc.perform(get("/api/v1/public/teacher-profiles/{profileId}/reviews", teacher.profileId())
						.param("rating", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].rating").value(5))
				.andExpect(jsonPath("$.meta.filters.rating").value(5));

		mockMvc.perform(get("/api/v1/public/teacher-profiles/{profileId}/reviews", teacher.profileId())
						.param("testId", String.valueOf(listeningId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].testId").value(listeningId));
	}

	@Test
	void hiddenReviewIsExcludedFromPublicListAndAggregateAndAdminDeleteStaysConsistent() throws Exception {
		StudentFixture first = student("hidden-one");
		StudentFixture second = student("hidden-two");
		String adminToken = admin("hidden-admin");
		TeacherFixture teacher = approvedTeacher("hidden-teacher");
		Long testId = publishedTest(teacher, "Hidden Reading");
		submittedAttempt(first.userId(), testId);
		submittedAttempt(second.userId(), testId);
		Long firstReviewId = createdReviewId(first.token(), testId, 5, "Keep me.");
		Long secondReviewId = createdReviewId(second.token(), testId, 1, "Hide me.");
		assertThat(teacherRatingAverage(teacher.profileId())).isEqualByComparingTo("3.00");
		assertThat(teacherRatingCount(teacher.profileId())).isEqualTo(2);

		mockMvc.perform(post("/api/v1/admin/reviews/{reviewId}/hide", secondReviewId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.hidden").value(true));

		mockMvc.perform(get("/api/v1/public/tests/{testId}/reviews", testId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(firstReviewId));
		assertThat(teacherRatingAverage(teacher.profileId())).isEqualByComparingTo("5.00");
		assertThat(teacherRatingCount(teacher.profileId())).isEqualTo(1);

		mockMvc.perform(post("/api/v1/admin/reviews/{reviewId}/unhide", secondReviewId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.hidden").value(false));
		assertThat(teacherRatingCount(teacher.profileId())).isEqualTo(2);

		mockMvc.perform(delete("/api/v1/admin/reviews/{reviewId}", secondReviewId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from reviews where id = ?", Integer.class, secondReviewId)).isZero();
		assertThat(teacherRatingAverage(teacher.profileId())).isEqualByComparingTo("5.00");
		assertThat(teacherRatingCount(teacher.profileId())).isEqualTo(1);
	}

	@Test
	void teacherReadsOwnContentReviewsButCannotModerateOrEditThem() throws Exception {
		StudentFixture student = student("teacher-list");
		TeacherFixture teacher = approvedTeacher("teacher-list-owner");
		TeacherFixture otherTeacher = approvedTeacher("teacher-list-other");
		Long ownTestId = publishedTest(teacher, "Own Reading");
		Long otherTestId = publishedTest(otherTeacher, "Other Reading");
		submittedAttempt(student.userId(), ownTestId);
		submittedAttempt(student.userId(), otherTestId);
		Long ownReviewId = createdReviewId(student.token(), ownTestId, 4, "Own content review.");
		createdReviewId(student.token(), otherTestId, 2, "Other content review.");

		mockMvc.perform(get("/api/v1/teacher/reviews")
						.header("Authorization", "Bearer " + teacher.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(ownReviewId))
				.andExpect(jsonPath("$.data[0].testId").value(ownTestId))
				.andExpect(jsonPath("$.data[0].comment").value("Own content review."));

		mockMvc.perform(get("/api/v1/teacher/reviews/summary")
						.header("Authorization", "Bearer " + teacher.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.ratingAverage").value(4.00))
				.andExpect(jsonPath("$.data.ratingCount").value(1));

		mockMvc.perform(patch("/api/v1/student/reviews/{reviewId}", ownReviewId)
						.header("Authorization", "Bearer " + teacher.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\": 5}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
		mockMvc.perform(post("/api/v1/admin/reviews/{reviewId}/hide", ownReviewId)
						.header("Authorization", "Bearer " + teacher.token()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void adminModerationListSupportsVisibilityFilterAndRequiresAdminRole() throws Exception {
		StudentFixture student = student("admin-list");
		String adminToken = admin("admin-list-admin");
		TeacherFixture teacher = approvedTeacher("admin-list-teacher");
		Long testId = publishedTest(teacher, "Admin List Reading");
		submittedAttempt(student.userId(), testId);
		Long reviewId = createdReviewId(student.token(), testId, 2, "Moderate me.");

		mockMvc.perform(get("/api/v1/admin/reviews")
						.header("Authorization", "Bearer " + student.token()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(post("/api/v1/admin/reviews/{reviewId}/hide", reviewId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/admin/reviews")
						.header("Authorization", "Bearer " + adminToken)
						.param("hidden", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(reviewId))
				.andExpect(jsonPath("$.data[0].hidden").value(true))
				.andExpect(jsonPath("$.data[0].studentUserId").value(student.userId()));

		mockMvc.perform(get("/api/v1/admin/reviews")
						.header("Authorization", "Bearer " + adminToken)
						.param("hidden", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	void moderatingMissingReviewReturnsReviewNotFound() throws Exception {
		String adminToken = admin("missing-admin");

		mockMvc.perform(post("/api/v1/admin/reviews/{reviewId}/hide", 987654)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("REVIEW_NOT_FOUND"));
		mockMvc.perform(delete("/api/v1/admin/reviews/{reviewId}", 987654)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("REVIEW_NOT_FOUND"));
	}

	@Test
	void createReviewRequiresStudentRoleAndAuthentication() throws Exception {
		TeacherFixture teacher = approvedTeacher("auth-required-teacher");
		Long testId = publishedTest(teacher, "Auth Required Reading");

		mockMvc.perform(post("/api/v1/student/tests/{testId}/reviews", testId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\": 5}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
		mockMvc.perform(post("/api/v1/student/tests/{testId}/reviews", testId)
						.header("Authorization", "Bearer " + teacher.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\": 5}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void publicReviewsForHiddenOrMissingTestReturnTestNotFound() throws Exception {
		TeacherFixture teacher = approvedTeacher("missing-test-teacher");
		Long draftTestId = draftTest(teacher, "Hidden Draft Reading");

		mockMvc.perform(get("/api/v1/public/tests/{testId}/reviews", draftTestId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEST_NOT_FOUND"));
		mockMvc.perform(get("/api/v1/public/teacher-profiles/{profileId}/reviews", 987654))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEACHER_PROFILE_NOT_FOUND"));
	}

	private org.springframework.test.web.servlet.ResultActions createReview(
			String token, Long testId, int rating, String comment) throws Exception {
		String body = comment == null
				? "{\"rating\": %d}".formatted(rating)
				: objectMapper.writeValueAsString(new ReviewBody(rating, comment));
		return mockMvc.perform(post("/api/v1/student/tests/{testId}/reviews", testId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private Long createdReviewId(String token, Long testId, int rating, String comment) throws Exception {
		String response = createReview(token, testId, rating, comment)
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).at("/data/id").asLong();
	}

	private BigDecimal teacherRatingAverage(Long teacherProfileId) {
		return jdbcTemplate.queryForObject(
				"select rating_average from teacher_profiles where id = ?", BigDecimal.class, teacherProfileId);
	}

	private Integer teacherRatingCount(Long teacherProfileId) {
		return jdbcTemplate.queryForObject(
				"select rating_count from teacher_profiles where id = ?", Integer.class, teacherProfileId);
	}

	private StudentFixture student(String prefix) throws Exception {
		String email = uniqueEmail(prefix);
		String response = mockMvc.perform(post("/api/v1/auth/register/student")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Student %s"
								}
								""".formatted(email, PASSWORD, prefix)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode root = objectMapper.readTree(response);
		Long userId = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
		return new StudentFixture(userId, root.at("/data/accessToken").asText());
	}

	private TeacherFixture approvedTeacher(String prefix) throws Exception {
		String email = uniqueEmail(prefix);
		Long userId = createUserWithRole(email, prefix, "teacher");
		Long profileId = insertAndReturnId("""
				insert into teacher_profiles (
				  user_id, display_name, headline, bio, status, approved_by_user_id, approved_at
				)
				values (?, ?, 'IELTS teacher', 'Experienced teacher.', 'approved', ?, ?)
				""",
				userId,
				"Teacher " + prefix,
				userId,
				now());
		return new TeacherFixture(userId, profileId, login(email));
	}

	private String admin(String prefix) throws Exception {
		String email = uniqueEmail(prefix);
		createUserWithRole(email, prefix, "admin");
		return login(email);
	}

	private Long createUserWithRole(String email, String prefix, String roleCode) {
		Long userId = insertAndReturnId("""
				insert into users (email, password_hash, full_name, status, preferred_language)
				values (?, ?, ?, 'active', 'en')
				""",
				email,
				passwordHasher.hash(PASSWORD),
				"Fixture " + prefix);
		Long roleId = jdbcTemplate.queryForObject("select id from roles where code = ?", Long.class, roleCode);
		jdbcTemplate.update("insert into user_roles (user_id, role_id) values (?, ?)", userId, roleId);
		return userId;
	}

	private String login(String email) throws Exception {
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
		return objectMapper.readTree(response).at("/data/accessToken").asText();
	}

	private Long publishedTest(TeacherFixture teacher, String title) {
		return insertTest(teacher, title, "published", now());
	}

	private Long draftTest(TeacherFixture teacher, String title) {
		return insertTest(teacher, title, "draft", null);
	}

	private Long insertTest(TeacherFixture teacher, String title, String visibility, OffsetDateTime publishedAt) {
		return insertAndReturnId("""
				insert into tests (
				  teacher_profile_id, title, description, type, difficulty, estimated_band_min,
				  estimated_band_max, duration_minutes, price_amount, currency, is_free, language,
				  visibility, published_at, created_by_user_id
				)
				values (?, ?, 'Review fixture.', 'reading', 'intermediate', 5.5, 7.0, 60, 15000.00, 'UZS', false, 'en', ?, ?, ?)
				""",
				teacher.profileId(),
				title,
				visibility,
				publishedAt,
				teacher.userId());
	}

	private Long activeListing(Long teacherProfileId, Long testId, BigDecimal priceAmount) {
		return insertAndReturnId("""
				insert into marketplace_listings (
				  teacher_profile_id, test_id, title, description, listing_type, price_amount, currency, is_active, language
				)
				values (?, ?, 'Review Listing', 'Review listing fixture.', 'single_test', ?, 'UZS', true, 'en')
				""",
				teacherProfileId,
				testId,
				priceAmount);
	}

	private void grantedPurchase(Long studentUserId, Long listingId) {
		jdbcTemplate.update("""
				insert into purchases (
				  student_user_id, listing_id, status, source_type, source_reference, amount, currency, purchased_at, updated_at
				)
				values (?, ?, 'granted', 'payment', 'payment-review', 15000.00, 'UZS', ?, ?)
				""",
				studentUserId,
				listingId,
				now(),
				now());
	}

	private Long submittedAttempt(Long studentUserId, Long testId) {
		return attempt(studentUserId, testId, "submitted", now());
	}

	private Long attempt(Long studentUserId, Long testId, String status, OffsetDateTime submittedAt) {
		return insertAndReturnId("""
				insert into test_attempts (test_id, student_user_id, status, started_at, submitted_at, time_spent_seconds)
				values (?, ?, ?, ?, ?, 0)
				""",
				testId,
				studentUserId,
				status,
				now().minusHours(1),
				submittedAt);
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

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private record StudentFixture(Long userId, String token) {
	}

	private record TeacherFixture(Long userId, Long profileId, String token) {
	}

	private record ReviewBody(int rating, String comment) {
	}
}
