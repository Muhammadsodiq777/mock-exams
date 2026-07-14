package com.mockexams.backend.controller;

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
import org.junit.jupiter.api.AfterEach;
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
class AdminReportControllerTest {

	private static final String PASSWORD = "correct-password-123";
	private static final OffsetDateTime FROM = OffsetDateTime.of(2003, 3, 4, 0, 0, 0, 0, ZoneOffset.UTC);
	private static final OffsetDateTime TO = OffsetDateTime.of(2003, 3, 4, 23, 59, 59, 0, ZoneOffset.UTC);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		cleanReportFixtures();
	}

	@AfterEach
	void tearDown() {
		cleanReportFixtures();
	}

	@Test
	void nonAdminCannotReadReports() throws Exception {
		Registration student = registerStudent(uniqueEmail("reports-forbidden"));

		mockMvc.perform(get("/api/v1/admin/reports/sales")
						.header("Authorization", "Bearer " + student.accessToken()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void invalidDateRangeAndUnsupportedParametersAreRejected() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("reports-invalid"));

		mockMvc.perform(get("/api/v1/admin/reports/ai-usage")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", TO.toString())
						.param("to", FROM.toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(get("/api/v1/admin/reports/sales")
						.header("Authorization", "Bearer " + adminToken)
						.param("sort", "amount"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(get("/api/v1/admin/reports/test-activity")
						.header("Authorization", "Bearer " + adminToken)
						.param("unknownFilter", "x"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void emptyReportsReturnZeroTotalsAndEmptyRows() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("reports-empty"));

		mockMvc.perform(get("/api/v1/admin/reports/ai-usage")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", FROM.toString())
						.param("to", TO.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.generatedAt").isNotEmpty())
				.andExpect(jsonPath("$.data.filters.from").value("2003-03-04T00:00:00Z"))
				.andExpect(jsonPath("$.data.filters.to").value("2003-03-04T23:59:59Z"))
				.andExpect(jsonPath("$.data.totals.totalRequests").value(0))
				.andExpect(jsonPath("$.data.totals.estimatedCost").value(0))
				.andExpect(jsonPath("$.data.rows").isEmpty());

		mockMvc.perform(get("/api/v1/admin/reports/provider-failures")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", FROM.toString())
						.param("to", TO.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totals.failureCount").value(0))
				.andExpect(jsonPath("$.data.totals.paymentFailures").value(0))
				.andExpect(jsonPath("$.data.totals.aiFailures").value(0))
				.andExpect(jsonPath("$.data.rows").isEmpty());
	}

	@Test
	void aiUsageReportAggregatesStoredFeedbackWithInclusiveBoundaries() throws Exception {
		Registration student = registerStudent(uniqueEmail("reports-ai-student"));
		String adminToken = registerAdmin(uniqueEmail("reports-ai-admin"));
		insertAiFeedback(student.userId(), "study_plan", "completed", "openai", "gpt-4.1-mini", 10, 5,
				new BigDecimal("0.120000"), null, FROM);
		insertAiFeedback(student.userId(), "study_plan", "failed", "openai", "gpt-4.1-mini", 3, 1,
				new BigDecimal("0.030000"), "AI_PROVIDER_FAILURE", FROM.plusMinutes(1));
		insertAiFeedback(student.userId(), "study_plan", "failed", "openai", "gpt-4.1-mini", 4, 2,
				new BigDecimal("0.040000"), "VALIDATION_FAILED", FROM.plusMinutes(2));
		insertAiFeedback(student.userId(), "study_plan", "failed", "openai", "gpt-4.1-mini", 0, 0,
				BigDecimal.ZERO, "AI_USAGE_LIMIT_EXCEEDED", TO);
		insertAiFeedback(student.userId(), "study_plan", "completed", "openai", "gpt-4.1-mini", 100, 20,
				new BigDecimal("9.990000"), null, FROM.minusSeconds(1));

		mockMvc.perform(get("/api/v1/admin/reports/ai-usage")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", FROM.toString())
						.param("to", TO.toString())
						.param("studentUserId", student.userId().toString())
						.param("feedbackType", "study_plan")
						.param("model", "gpt-4.1-mini"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totals.totalRequests").value(4))
				.andExpect(jsonPath("$.data.totals.completedRequests").value(1))
				.andExpect(jsonPath("$.data.totals.failedRequests").value(1))
				.andExpect(jsonPath("$.data.totals.validationFailedRequests").value(1))
				.andExpect(jsonPath("$.data.totals.blockedRequests").value(1))
				.andExpect(jsonPath("$.data.totals.promptTokens").value(17))
				.andExpect(jsonPath("$.data.totals.completionTokens").value(8))
				.andExpect(jsonPath("$.data.totals.estimatedCost").value(0.19))
				.andExpect(jsonPath("$.data.rows.length()").value(4))
				.andExpect(jsonPath("$.data.rows[0].studentUserId").value(student.userId()))
				.andExpect(jsonPath("$.data.rows[0].feedbackType").value("study_plan"))
				.andExpect(jsonPath("$.data.rows[0].model").value("gpt-4.1-mini"))
				.andExpect(jsonPath("$.data.rows[1].failureCode").value("AI_PROVIDER_FAILURE"))
				.andExpect(jsonPath("$.data.rows[1].validationFailedRequests").value(0))
				.andExpect(jsonPath("$.data.rows[2].failureCode").value("AI_USAGE_LIMIT_EXCEEDED"))
				.andExpect(jsonPath("$.data.rows[2].blockedRequests").value(1))
				.andExpect(jsonPath("$.data.rows[3].failureCode").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.data.rows[3].validationFailedRequests").value(1));
	}

	@Test
	void salesAndRevenueReportsUsePersistedPaymentsPurchasesAndTeacherTransactions() throws Exception {
		Registration student = registerStudent(uniqueEmail("reports-sales-student"));
		TeacherFixture teacher = approvedTeacher("reports-sales-teacher");
		String adminToken = registerAdmin(uniqueEmail("reports-sales-admin"));
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Revenue Reading", "reading");
		Long listingId = createActiveListing(teacher.profileId(), testId, "Revenue Listing", new BigDecimal("40000.00"));
		Long paymentId = insertPayment(student.userId(), listingId, "mock", "paid", new BigDecimal("40000.00"),
				FROM);
		Long purchaseId = insertPurchase(student.userId(), listingId, paymentId, new BigDecimal("40000.00"), FROM);
		Long walletId = insertWallet(teacher.profileId());
		insertTeacherTransaction(walletId, purchaseId, paymentId, "earning", "marketplace_purchase",
				new BigDecimal("40000.00"), new BigDecimal("10000.00"), new BigDecimal("30000.00"), FROM);
		Long refundedPaymentId = insertPayment(student.userId(), listingId, "mock", "refunded",
				new BigDecimal("15000.00"), FROM.plusHours(1));
		insertPaymentDispute(refundedPaymentId, student.userId(), "refunded", FROM.plusHours(2));
		insertPayment(student.userId(), listingId, "mock", "failed", new BigDecimal("99999.00"), FROM.minusDays(2));

		mockMvc.perform(get("/api/v1/admin/reports/sales")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", FROM.toString())
						.param("to", TO.toString())
						.param("teacherProfileId", teacher.profileId().toString())
						.param("listingId", listingId.toString())
						.param("provider", "mock"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totals.totalPayments").value(2))
				.andExpect(jsonPath("$.data.totals.paidPayments").value(1))
				.andExpect(jsonPath("$.data.totals.refundedPayments").value(1))
				.andExpect(jsonPath("$.data.totals.failedPayments").value(0))
				.andExpect(jsonPath("$.data.totals.grossAmount").value(55000.00))
				.andExpect(jsonPath("$.data.totals.paidGrossAmount").value(40000.00))
				.andExpect(jsonPath("$.data.totals.refundedGrossAmount").value(15000.00))
				.andExpect(jsonPath("$.data.totals.disputeCount").value(1))
				.andExpect(jsonPath("$.data.totals.marketplaceGrossAmount").value(55000.00))
				.andExpect(jsonPath("$.data.totals.liveSpeakingGrossAmount").value(0))
				.andExpect(jsonPath("$.data.rows[0].disputeCount").value(0))
				.andExpect(jsonPath("$.data.rows[1].disputeCount").value(1));

		mockMvc.perform(get("/api/v1/admin/reports/revenue")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", FROM.toString())
						.param("to", TO.toString())
						.param("teacherProfileId", teacher.profileId().toString())
						.param("listingId", listingId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totals.transactionCount").value(1))
				.andExpect(jsonPath("$.data.totals.grossTotal").value(40000.00))
				.andExpect(jsonPath("$.data.totals.platformCommissionTotal").value(10000.00))
				.andExpect(jsonPath("$.data.totals.netTotal").value(30000.00))
				.andExpect(jsonPath("$.data.rows[0].teacherProfileId").value(teacher.profileId()))
				.andExpect(jsonPath("$.data.rows[0].listingId").value(listingId));
	}

	@Test
	void testActivityReportAggregatesAttemptsByTestTypeAndTeacher() throws Exception {
		Registration student = registerStudent(uniqueEmail("reports-activity-student"));
		TeacherFixture teacher = approvedTeacher("reports-activity-teacher");
		String adminToken = registerAdmin(uniqueEmail("reports-activity-admin"));
		Long readingTestId = createPublishedTest(teacher.userId(), teacher.profileId(), "Activity Reading", "reading");
		Long writingTestId = createPublishedTest(teacher.userId(), teacher.profileId(), "Activity Writing",
				"writing_task_1");
		insertAttempt(readingTestId, student.userId(), "submitted", FROM, FROM.plusMinutes(25), 1500);
		insertAttempt(readingTestId, student.userId(), "expired", TO.minusHours(1), null, 3600);
		insertAttempt(writingTestId, student.userId(), "submitted", FROM.plusHours(2), FROM.plusHours(3), 1800);

		mockMvc.perform(get("/api/v1/admin/reports/test-activity")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", FROM.toString())
						.param("to", TO.toString())
						.param("teacherProfileId", teacher.profileId().toString())
						.param("testType", "reading"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totals.totalAttempts").value(2))
				.andExpect(jsonPath("$.data.totals.submittedAttempts").value(1))
				.andExpect(jsonPath("$.data.totals.expiredAttempts").value(1))
				.andExpect(jsonPath("$.data.totals.inProgressAttempts").value(0))
				.andExpect(jsonPath("$.data.rows.length()").value(2))
				.andExpect(jsonPath("$.data.rows[0].teacherProfileId").value(teacher.profileId()))
				.andExpect(jsonPath("$.data.rows[0].testType").value("reading"));
	}

	@Test
	void providerFailureReportSeparatesPaymentAndAiFailureSignals() throws Exception {
		Registration student = registerStudent(uniqueEmail("reports-provider-student"));
		String adminToken = registerAdmin(uniqueEmail("reports-provider-admin"));
		insertPayment(student.userId(), null, "mock", "failed", new BigDecimal("25000.00"), FROM,
				"PAYMENT_AMOUNT_MISMATCH");
		insertAiFeedback(student.userId(), "study_plan", "failed", "openai", "gpt-4.1-mini", 0, 0,
				BigDecimal.ZERO, "AI_PROVIDER_FAILURE", FROM.plusMinutes(1));
		insertAiFeedback(student.userId(), "study_plan", "failed", "openai", "gpt-4.1-mini", 1, 1,
				BigDecimal.ZERO, "VALIDATION_FAILED", FROM.plusMinutes(2));
		insertAiFeedback(student.userId(), "study_plan", "failed", "openai", "gpt-4.1-mini", 0, 0,
				BigDecimal.ZERO, "AI_USAGE_LIMIT_EXCEEDED", FROM.plusMinutes(3));

		mockMvc.perform(get("/api/v1/admin/reports/provider-failures")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", FROM.toString())
						.param("to", TO.toString())
						.param("provider", "openai"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totals.failureCount").value(2))
				.andExpect(jsonPath("$.data.totals.paymentFailures").value(0))
				.andExpect(jsonPath("$.data.totals.aiFailures").value(2))
				.andExpect(jsonPath("$.data.totals.validationFailures").value(1))
				.andExpect(jsonPath("$.data.totals.providerFailures").value(1));

		mockMvc.perform(get("/api/v1/admin/reports/provider-failures")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", FROM.toString())
						.param("to", TO.toString())
						.param("provider", "mock"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totals.failureCount").value(1))
				.andExpect(jsonPath("$.data.totals.paymentFailures").value(1))
				.andExpect(jsonPath("$.data.totals.aiFailures").value(0))
				.andExpect(jsonPath("$.data.totals.amountMismatchPayments").value(1))
				.andExpect(jsonPath("$.data.rows[0].sourceType").value("payment"))
				.andExpect(jsonPath("$.data.rows[0].failureCode").value("PAYMENT_AMOUNT_MISMATCH"));
	}

	private void cleanReportFixtures() {
		jdbcTemplate.update("delete from notifications");
		jdbcTemplate.update("delete from payment_disputes");
		jdbcTemplate.update("delete from teacher_transactions");
		jdbcTemplate.update("delete from promo_code_redemptions");
		jdbcTemplate.update("delete from withdrawal_requests");
		jdbcTemplate.update("delete from teacher_wallets");
		jdbcTemplate.update("delete from subscriptions");
		jdbcTemplate.update("delete from purchases");
		jdbcTemplate.update("delete from payments");
		jdbcTemplate.update("delete from marketplace_listing_items");
		jdbcTemplate.update("delete from marketplace_listings");
		jdbcTemplate.update("delete from reviews");
		jdbcTemplate.update("delete from study_plan_tasks");
		jdbcTemplate.update("delete from study_plans");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from test_results");
		jdbcTemplate.update("delete from student_answers");
		jdbcTemplate.update("delete from writing_submissions");
		jdbcTemplate.update("delete from speaking_submissions");
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

	private void insertAiFeedback(
			Long studentUserId,
			String feedbackType,
			String status,
			String provider,
			String model,
			int promptTokens,
			int completionTokens,
			BigDecimal estimatedCost,
			String failureCode,
			OffsetDateTime createdAt
	) {
		jdbcTemplate.update("""
				insert into ai_feedbacks (
				  student_user_id, feedback_type, status, provider, model, prompt_tokens, completion_tokens,
				  estimated_cost, failure_code, error_message, created_at, completed_at
				)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'fixture failure', ?, ?)
				""",
				studentUserId,
				feedbackType,
				status,
				provider,
				model,
				promptTokens,
				completionTokens,
				estimatedCost,
				failureCode,
				createdAt,
				createdAt);
	}

	private Long insertPayment(Long studentUserId, Long listingId, String provider, String status, BigDecimal amount,
			OffsetDateTime createdAt) {
		return insertPayment(studentUserId, listingId, provider, status, amount, createdAt, null);
	}

	private Long insertPayment(Long studentUserId, Long listingId, String provider, String status, BigDecimal amount,
			OffsetDateTime createdAt, String failureCode) {
		return insertAndReturnId("""
				insert into payments (
				  student_user_id, target_listing_id, target_type, provider, provider_payment_id, status, amount,
				  currency, failure_code, created_at, paid_at, updated_at
				)
				values (?, ?, ?, ?, ?, ?, ?, 'UZS', ?, ?, ?, ?)
				""",
				studentUserId,
				listingId,
				listingId == null ? "legacy" : "marketplace_listing",
				provider,
				"report-payment-" + UUID.randomUUID(),
				status,
				amount,
				failureCode,
				createdAt,
				"paid".equals(status) || "refunded".equals(status) ? createdAt : null,
				createdAt);
	}

	private void insertPaymentDispute(Long paymentId, Long studentUserId, String status, OffsetDateTime createdAt) {
		jdbcTemplate.update("""
				insert into payment_disputes (
				  payment_id, opened_by_user_id, status, reason, description, reviewed_at, created_at, updated_at
				)
				values (?, ?, ?, 'refund_requested', 'fixture dispute', ?, ?, ?)
				""",
				paymentId,
				studentUserId,
				status,
				createdAt,
				createdAt,
				createdAt);
	}

	private Long insertPurchase(Long studentUserId, Long listingId, Long paymentId, BigDecimal amount,
			OffsetDateTime purchasedAt) {
		return insertAndReturnId("""
				insert into purchases (
				  student_user_id, listing_id, payment_id, status, source_type, source_reference, amount, currency,
				  purchased_at, updated_at
				)
				values (?, ?, ?, 'granted', 'payment', ?, ?, 'UZS', ?, ?)
				""",
				studentUserId,
				listingId,
				paymentId,
				"report-purchase-" + paymentId,
				amount,
				purchasedAt,
				purchasedAt);
	}

	private Long insertWallet(Long teacherProfileId) {
		return insertAndReturnId("""
				insert into teacher_wallets (teacher_profile_id, balance_amount, pending_amount, currency)
				values (?, 0, 0, 'UZS')
				""",
				teacherProfileId);
	}

	private void insertTeacherTransaction(
			Long walletId,
			Long purchaseId,
			Long paymentId,
			String transactionType,
			String sourceType,
			BigDecimal grossAmount,
			BigDecimal commissionAmount,
			BigDecimal netAmount,
			OffsetDateTime createdAt
	) {
		jdbcTemplate.update("""
				insert into teacher_transactions (
				  wallet_id, purchase_id, payment_id, transaction_type, source_type, source_reference, gross_amount,
				  platform_commission_amount, net_amount, currency, description, created_at
				)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'UZS', 'report fixture', ?)
				""",
				walletId,
				purchaseId,
				paymentId,
				transactionType,
				sourceType,
				"report-tx-" + UUID.randomUUID(),
				grossAmount,
				commissionAmount,
				netAmount,
				createdAt);
	}

	private void insertAttempt(Long testId, Long studentUserId, String status, OffsetDateTime startedAt,
			OffsetDateTime submittedAt, int timeSpentSeconds) {
		jdbcTemplate.update("""
				insert into test_attempts (
				  test_id, student_user_id, status, started_at, submitted_at, expires_at, time_spent_seconds,
				  created_at, updated_at
				)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
				testId,
				studentUserId,
				status,
				startedAt,
				submittedAt,
				startedAt.plusMinutes(60),
				timeSpentSeconds,
				startedAt,
				startedAt);
	}

	private TeacherFixture approvedTeacher(String prefix) throws Exception {
		Registration registration = registerStudent(uniqueEmail(prefix));
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select ?, r.id
				from roles r
				where r.code = 'teacher'
				""", registration.userId());
		Long profileId = insertAndReturnId("""
				insert into teacher_profiles (
				  user_id, display_name, headline, bio, status, approved_by_user_id, approved_at
				)
				values (?, ?, 'IELTS teacher', 'Experienced teacher.', 'approved', ?, ?)
				""",
				registration.userId(),
				"Teacher " + registration.userId(),
				registration.userId(),
				FROM);
		return new TeacherFixture(registration.userId(), profileId);
	}

	private Long createPublishedTest(Long userId, Long teacherProfileId, String title, String type) {
		return insertAndReturnId("""
				insert into tests (
				  teacher_profile_id, title, description, type, difficulty, estimated_band_min, estimated_band_max,
				  duration_minutes, price_amount, currency, is_free, language, visibility, published_at, created_by_user_id
				)
				values (?, ?, 'Report fixture.', ?, 'intermediate', 5.5, 7.0, 60, 15000.00, 'UZS', false, 'en',
				  'published', ?, ?)
				""",
				teacherProfileId,
				title,
				type,
				FROM,
				userId);
	}

	private Long createActiveListing(Long teacherProfileId, Long testId, String title, BigDecimal priceAmount) {
		return insertAndReturnId("""
				insert into marketplace_listings (
				  teacher_profile_id, test_id, title, description, listing_type, price_amount, currency, is_active, language
				)
				values (?, ?, ?, 'Report listing fixture.', 'single_test', ?, 'UZS', true, 'en')
				""",
				teacherProfileId,
				testId,
				title,
				priceAmount);
	}

	private String registerAdmin(String email) throws Exception {
		Registration registration = registerStudent(email);
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select ?, r.id
				from roles r
				where r.code = 'admin'
				""", registration.userId());
		return registration.accessToken();
	}

	private Registration registerStudent(String email) throws Exception {
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
		JsonNode root = objectMapper.readTree(response);
		return new Registration(root.at("/data/accessToken").asText(), root.at("/data/user/id").asLong());
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
		String boundedPrefix = prefix.length() > 24 ? prefix.substring(0, 24) : prefix;
		return boundedPrefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private record Registration(String accessToken, Long userId) {
	}

	private record TeacherFixture(Long userId, Long profileId) {
	}
}
