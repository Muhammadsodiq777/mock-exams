package com.mockexams.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockexams.backend.auth.service.PasswordHasher;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentDisputeControllerTest {

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
		cleanFixtures();
	}

	@AfterEach
	void tearDown() {
		cleanFixtures();
	}

	@Test
	void studentCreatesListsAndReadsOwnDispute() throws Exception {
		StudentFixture student = registerAndLoginStudent("dispute-http-student");
		Long paymentId = createPayment(student.userId(), "paid", "25000.00");

		String createResponse = mockMvc.perform(post("/api/v1/student/payment-disputes")
						.header("Authorization", "Bearer " + student.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "paymentId": %d,
								  "reason": "Charged twice",
								  "description": "Please investigate the duplicate charge."
								}
								""".formatted(paymentId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("open"))
				.andExpect(jsonPath("$.data.paymentId").value(paymentId))
				.andExpect(jsonPath("$.data.paymentStatus").value("paid"))
				.andExpect(jsonPath("$.data.reason").value("Charged twice"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		Long disputeId = objectMapper.readTree(createResponse).at("/data/id").asLong();

		mockMvc.perform(get("/api/v1/student/payment-disputes")
						.header("Authorization", "Bearer " + student.token())
						.param("status", "open"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(disputeId))
				.andExpect(jsonPath("$.meta.page.totalElements").value(1))
				.andExpect(jsonPath("$.meta.filters.status").value("open"));

		mockMvc.perform(get("/api/v1/student/payment-disputes/" + disputeId)
						.header("Authorization", "Bearer " + student.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(disputeId))
				.andExpect(jsonPath("$.data.status").value("open"));
	}

	@Test
	void studentCannotDisputeAnotherStudentsPaymentOrAnUndisputablePayment() throws Exception {
		StudentFixture owner = registerAndLoginStudent("dispute-http-owner");
		StudentFixture other = registerAndLoginStudent("dispute-http-other");
		Long paymentId = createPayment(owner.userId(), "paid", "25000.00");
		Long pendingPaymentId = createPayment(owner.userId(), "pending", "25000.00");

		mockMvc.perform(post("/api/v1/student/payment-disputes")
						.header("Authorization", "Bearer " + other.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "paymentId": %d, "reason": "Not my charge" }
								""".formatted(paymentId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("PAYMENT_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/student/payment-disputes")
						.header("Authorization", "Bearer " + owner.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "paymentId": %d, "reason": "Still pending" }
								""".formatted(pendingPaymentId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PAYMENT_DISPUTE_STATUS_NOT_ALLOWED"));
	}

	@Test
	void studentCannotReadAnotherStudentsDisputeAndCannotOpenADuplicateOpenDispute() throws Exception {
		StudentFixture owner = registerAndLoginStudent("dispute-http-read-owner");
		StudentFixture other = registerAndLoginStudent("dispute-http-read-other");
		Long paymentId = createPayment(owner.userId(), "paid", "25000.00");
		Long disputeId = createDispute(owner.token(), paymentId, "Charged twice");

		mockMvc.perform(get("/api/v1/student/payment-disputes/" + disputeId)
						.header("Authorization", "Bearer " + other.token()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("PAYMENT_DISPUTE_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/student/payment-disputes")
						.header("Authorization", "Bearer " + owner.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "paymentId": %d, "reason": "Still not resolved" }
								""".formatted(paymentId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PAYMENT_DISPUTE_ALREADY_OPEN"));
	}

	@Test
	void adminQueueDetailAndDecisionFlowRecordsReviewerAndNotifiesStudent() throws Exception {
		StudentFixture student = registerAndLoginStudent("dispute-http-admin-student");
		String adminToken = loginAdmin("dispute-http-admin");
		Long paymentId = createPayment(student.userId(), "paid", "25000.00");
		Long disputeId = createDispute(student.token(), paymentId, "Charged twice");

		mockMvc.perform(get("/api/v1/admin/payment-disputes")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "open"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(disputeId))
				.andExpect(jsonPath("$.data[0].studentUserId").value(student.userId()))
				.andExpect(jsonPath("$.meta.page.totalElements").value(1))
				.andExpect(jsonPath("$.meta.filters.status").value("open"));

		mockMvc.perform(get("/api/v1/admin/payment-disputes/" + disputeId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(disputeId))
				.andExpect(jsonPath("$.data.paymentAmount").value(25000.00));

		mockMvc.perform(post("/api/v1/admin/payment-disputes/" + disputeId + "/decision")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\": \"refunded\", \"resolutionNote\": \"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/admin/payment-disputes/" + disputeId + "/decision")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\": \"refunded\", \"resolutionNote\": \"Marked as refunded for support tracking.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("refunded"))
				.andExpect(jsonPath("$.data.resolutionNote").value("Marked as refunded for support tracking."))
				.andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());

		mockMvc.perform(post("/api/v1/admin/payment-disputes/" + disputeId + "/decision")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\": \"resolved\", \"resolutionNote\": \"Second look.\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PAYMENT_DISPUTE_INVALID_STATUS_TRANSITION"));

		assertNotificationExists(student.userId(), "refunded");

		// The payment's own status and wallet ledger are never mutated by a dispute decision.
		String paymentStatus = jdbcTemplate.queryForObject(
				"select status from payments where id = ?", String.class, paymentId);
		org.assertj.core.api.Assertions.assertThat(paymentStatus).isEqualTo("paid");
		int walletTransactionCount = jdbcTemplate.queryForObject(
				"select count(*) from teacher_transactions", Integer.class);
		org.assertj.core.api.Assertions.assertThat(walletTransactionCount).isZero();
	}

	@Test
	void adminQueueIsPagedAndFilterableByPaymentStatus() throws Exception {
		StudentFixture student = registerAndLoginStudent("dispute-http-page-student");
		String adminToken = loginAdmin("dispute-http-page-admin");
		Long paidPaymentId = createPayment(student.userId(), "paid", "25000.00");
		Long failedPaymentId = createPayment(student.userId(), "failed", "10000.00");
		createDispute(student.token(), paidPaymentId, "Charged twice");
		createDispute(student.token(), failedPaymentId, "Charge failed but money left");

		mockMvc.perform(get("/api/v1/admin/payment-disputes")
						.header("Authorization", "Bearer " + adminToken)
						.param("page", "0")
						.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.meta.page.totalElements").value(2))
				.andExpect(jsonPath("$.meta.page.totalPages").value(2))
				.andExpect(jsonPath("$.meta.sort[0].field").value("createdAt"))
				.andExpect(jsonPath("$.meta.sort[0].direction").value("desc"));

		mockMvc.perform(get("/api/v1/admin/payment-disputes")
						.header("Authorization", "Bearer " + adminToken)
						.param("paymentStatus", "failed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].paymentId").value(failedPaymentId))
				.andExpect(jsonPath("$.meta.filters.paymentStatus").value("failed"));

		mockMvc.perform(get("/api/v1/admin/payment-disputes")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "not-a-status"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(get("/api/v1/admin/payment-disputes")
						.header("Authorization", "Bearer " + adminToken)
						.param("paymentStatus", "not-a-payment-status"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void adminQueueFiltersByDateStudentAndReason() throws Exception {
		StudentFixture matchingStudent = registerAndLoginStudent("dispute-http-filter-student");
		StudentFixture otherStudent = registerAndLoginStudent("dispute-http-filter-other");
		String adminToken = loginAdmin("dispute-http-filter-admin");
		Long matchingPaymentId = createPayment(matchingStudent.userId(), "paid", "25000.00");
		Long outsideWindowPaymentId = createPayment(matchingStudent.userId(), "paid", "18000.00");
		Long otherStudentPaymentId = createPayment(otherStudent.userId(), "paid", "10000.00");
		Long matchingDisputeId = createDispute(matchingStudent.token(), matchingPaymentId, "Wrong amount charged");
		Long outsideWindowDisputeId = createDispute(matchingStudent.token(), outsideWindowPaymentId, "Wrong amount old");
		createDispute(otherStudent.token(), otherStudentPaymentId, "Wrong amount other student");
		setDisputeCreatedAt(matchingDisputeId, "2026-07-04T10:00:00Z");
		setDisputeCreatedAt(outsideWindowDisputeId, "2026-06-25T10:00:00Z");

		mockMvc.perform(get("/api/v1/admin/payment-disputes")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", "2026-07-01T00:00:00Z")
						.param("to", "2026-07-08T00:00:00Z")
						.param("studentUserId", matchingStudent.userId().toString())
						.param("reason", "amount"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(matchingDisputeId))
				.andExpect(jsonPath("$.data[0].studentUserId").value(matchingStudent.userId()))
				.andExpect(jsonPath("$.data[0].reason").value("Wrong amount charged"))
				.andExpect(jsonPath("$.meta.filters.studentUserId").value(matchingStudent.userId()))
				.andExpect(jsonPath("$.meta.filters.reason").value("amount"))
				.andExpect(jsonPath("$.meta.filters.from").value("2026-07-01T00:00:00Z"))
				.andExpect(jsonPath("$.meta.filters.to").value("2026-07-08T00:00:00Z"));
	}

	@Test
	void roleBoundariesAreEnforced() throws Exception {
		StudentFixture student = registerAndLoginStudent("dispute-http-roles-student");
		String adminToken = loginAdmin("dispute-http-roles-admin");
		Long paymentId = createPayment(student.userId(), "paid", "25000.00");
		Long disputeId = createDispute(student.token(), paymentId, "Charged twice");

		mockMvc.perform(get("/api/v1/admin/payment-disputes")
						.header("Authorization", "Bearer " + student.token()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(post("/api/v1/admin/payment-disputes/" + disputeId + "/decision")
						.header("Authorization", "Bearer " + student.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\": \"resolved\", \"resolutionNote\": \"Note.\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(post("/api/v1/student/payment-disputes")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "paymentId": %d, "reason": "Admin cannot open disputes" }
								""".formatted(paymentId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(get("/api/v1/student/payment-disputes"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
	}

	private void assertNotificationExists(Long userId, String status) {
		int count = jdbcTemplate.queryForObject("""
				select count(*) from notifications
				where user_id = ? and type = 'payment' and cast(payload as varchar) like ?
				""", Integer.class, userId, "%\"status\":\"" + status + "\"%");
		org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
	}

	private Long createDispute(String studentToken, Long paymentId, String reason) throws Exception {
		String response = mockMvc.perform(post("/api/v1/student/payment-disputes")
						.header("Authorization", "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "paymentId": %d, "reason": "%s" }
								""".formatted(paymentId, reason)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).at("/data/id").asLong();
	}

	private Long createPayment(Long studentUserId, String status, String amount) {
		return insertAndReturnId("""
				insert into payments (student_user_id, provider, status, amount, currency)
				values (?, 'mock', ?, ?, 'UZS')
				""",
				studentUserId,
				status,
				new BigDecimal(amount));
	}

	private void setDisputeCreatedAt(Long disputeId, String timestamp) {
		OffsetDateTime createdAt = OffsetDateTime.parse(timestamp);
		jdbcTemplate.update(
				"update payment_disputes set created_at = ?, updated_at = ? where id = ?",
				createdAt,
				createdAt,
				disputeId);
	}

	private StudentFixture registerAndLoginStudent(String prefix) throws Exception {
		String email = uniqueEmail(prefix);
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
		String token = root.at("/data/accessToken").asText();
		Long userId = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
		return new StudentFixture(userId, token);
	}

	private String loginAdmin(String prefix) throws Exception {
		Long userId = insertAndReturnId("""
				insert into users (email, password_hash, full_name, status, preferred_language)
				values (?, ?, ?, 'active', 'en')
				""",
				uniqueEmail(prefix),
				passwordHasher.hash(PASSWORD),
				"Fixture " + prefix);
		Long roleId = jdbcTemplate.queryForObject("select id from roles where code = ?", Long.class, "admin");
		jdbcTemplate.update("insert into user_roles (user_id, role_id) values (?, ?)", userId, roleId);
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
		return objectMapper.readTree(response).at("/data/accessToken").asText();
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
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

	private void cleanFixtures() {
		jdbcTemplate.update("delete from teacher_transactions");
		jdbcTemplate.update("delete from promo_code_redemptions");
		jdbcTemplate.update("delete from withdrawal_requests");
		jdbcTemplate.update("delete from teacher_wallets");
		jdbcTemplate.update("delete from subscriptions");
		jdbcTemplate.update("delete from purchases");
		jdbcTemplate.update("delete from payment_disputes");
		jdbcTemplate.update("delete from payments");
		jdbcTemplate.update("delete from reviews");
		jdbcTemplate.update("delete from marketplace_listing_items");
		jdbcTemplate.update("delete from marketplace_listings");
		jdbcTemplate.update("delete from study_plan_tasks");
		jdbcTemplate.update("delete from study_plans");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from writing_submissions");
		jdbcTemplate.update("delete from speaking_submissions");
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
		jdbcTemplate.update("delete from notifications");
		jdbcTemplate.update("delete from complaints");
		jdbcTemplate.update("delete from banners");
		jdbcTemplate.update("delete from school_members");
		jdbcTemplate.update("delete from school_accounts");
		jdbcTemplate.update("delete from referral_uses");
		jdbcTemplate.update("delete from referral_codes");
		jdbcTemplate.update("delete from leaderboard_entries");
		jdbcTemplate.update("delete from leaderboards");
		jdbcTemplate.update("delete from student_streaks");
		jdbcTemplate.update("delete from files");
		jdbcTemplate.update("delete from teacher_profiles");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
	}

	private record StudentFixture(Long userId, String token) {
	}
}
