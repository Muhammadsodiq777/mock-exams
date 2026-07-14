package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.mockexams.backend.entity.PaymentStatuses;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.entity.WithdrawalRequestStatuses;
import com.mockexams.backend.repository.PaymentRepository;
import com.mockexams.backend.repository.TestCatalogRepository;
import com.mockexams.backend.repository.WithdrawalRequestRepository;
import com.mockexams.backend.users.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDashboardControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private WithdrawalRequestRepository withdrawalRequestRepository;

	@Autowired
	private TestCatalogRepository testCatalogRepository;

	@AfterEach
	void cleanDashboardFixtures() {
		jdbcTemplate.update("delete from payments where provider = 'dash-test'");
		jdbcTemplate.update("delete from ai_feedbacks where model = 'dash-test-model'");
	}

	@Test
	void anonymousRequestIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/admin/dashboard"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void nonAdminIsForbidden() throws Exception {
		Registration student = registerStudent(uniqueEmail("dash-forbidden"));

		mockMvc.perform(get("/api/v1/admin/dashboard")
						.header("Authorization", "Bearer " + student.accessToken()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void invalidDateRangeIsRejected() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("dash-range"));
		OffsetDateTime from = OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		mockMvc.perform(get("/api/v1/admin/dashboard")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", from.toString())
						.param("to", from.minusDays(1).toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void emptyWindowReturnsZeroCountsAndSafeDefaults() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("dash-empty"));
		OffsetDateTime from = uniqueWindowStart();
		OffsetDateTime to = from.plusHours(1);

		mockMvc.perform(get("/api/v1/admin/dashboard")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", from.toString())
						.param("to", to.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.users.newInWindow").value(0))
				.andExpect(jsonPath("$.data.payments.total").value(0))
				.andExpect(jsonPath("$.data.payments.paid").value(0))
				.andExpect(jsonPath("$.data.payments.amountMismatch").value(0))
				.andExpect(jsonPath("$.data.payments.paidAmountTotal").value(0))
				.andExpect(jsonPath("$.data.withdrawals.total").value(0))
				.andExpect(jsonPath("$.data.withdrawals.requestedAmountTotal").value(0))
				.andExpect(jsonPath("$.data.aiUsage.totalRequests").value(0))
				.andExpect(jsonPath("$.data.aiUsage.estimatedCost").value(0))
				.andExpect(jsonPath("$.data.queues.pendingTeacherApprovals").isNumber())
				.andExpect(jsonPath("$.data.queues.pendingTestModeration").isNumber())
				.andExpect(jsonPath("$.data.queues.pendingWithdrawals").isNumber())
				.andExpect(jsonPath("$.data.queues.failedPayments").isNumber())
				.andExpect(jsonPath("$.data.queues.amountMismatchPayments").isNumber())
				.andExpect(jsonPath("$.data.recentActivity").isArray())
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty())
				.andExpect(jsonPath("$.meta.asOf").isNotEmpty())
				.andExpect(jsonPath("$.meta.window.from").value(from.toString()))
				.andExpect(jsonPath("$.meta.window.to").value(to.toString()));
	}

	@Test
	void dateRangeFiltersPaymentAndAiTotals() throws Exception {
		Registration student = registerStudent(uniqueEmail("dash-win-stud"));
		String adminToken = registerAdmin(uniqueEmail("dash-win-adm"));
		OffsetDateTime from = uniqueWindowStart();
		OffsetDateTime to = from.plusHours(1);
		OffsetDateTime inside = from.plusMinutes(30);
		OffsetDateTime outside = from.minusDays(5);

		insertPaidPayment(student.userId(), new BigDecimal("50000.00"), inside);
		insertPaidPayment(student.userId(), new BigDecimal("70000.00"), outside);
		insertMismatchPayment(student.userId(), new BigDecimal("30000.00"), inside);
		insertCompletedAiFeedback(student.userId(), inside, null);
		insertCompletedAiFeedback(student.userId(), outside, null);

		mockMvc.perform(get("/api/v1/admin/dashboard")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", from.toString())
						.param("to", to.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.payments.total").value(2))
				.andExpect(jsonPath("$.data.payments.paid").value(1))
				.andExpect(jsonPath("$.data.payments.failed").value(1))
				.andExpect(jsonPath("$.data.payments.amountMismatch").value(1))
				.andExpect(jsonPath("$.data.payments.paidAmountTotal").value(50000.00))
				.andExpect(jsonPath("$.data.aiUsage.totalRequests").value(1))
				.andExpect(jsonPath("$.data.aiUsage.completedRequests").value(1))
				.andExpect(jsonPath("$.data.aiUsage.promptTokens").value(10))
				.andExpect(jsonPath("$.data.aiUsage.completionTokens").value(5))
				.andExpect(jsonPath("$.meta.window.from").value(from.toString()));
	}

	@Test
	void countsMatchUnderlyingRepositoryTotals() throws Exception {
		Registration student = registerStudent(uniqueEmail("dash-match-stud"));
		String adminToken = registerAdmin(uniqueEmail("dash-match-adm"));
		insertPaidPayment(student.userId(), new BigDecimal("15000.00"), OffsetDateTime.now(ZoneOffset.UTC));

		String response = mockMvc.perform(get("/api/v1/admin/dashboard")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode data = objectMapper.readTree(response).at("/data");

		assertThat(data.at("/users/total").asLong()).isEqualTo(userRepository.count());
		assertThat(data.at("/payments/total").asLong()).isEqualTo(paymentRepository.count());
		assertThat(data.at("/payments/paid").asLong())
				.isEqualTo(paymentRepository.countByStatus(PaymentStatuses.PAID));
		assertThat(data.at("/queues/pendingWithdrawals").asLong())
				.isEqualTo(withdrawalRequestRepository.countByStatus(WithdrawalRequestStatuses.REQUESTED));
		assertThat(data.at("/queues/failedPayments").asLong())
				.isEqualTo(paymentRepository.countByStatus(PaymentStatuses.FAILED));
		assertThat(data.at("/queues/pendingTestModeration").asLong())
				.isEqualTo(testCatalogRepository.countByVisibilityAndDeletedAtIsNull(TestVisibilities.SUBMITTED));
	}

	@Test
	void dashboardDoesNotExposePrivateAnswerOrFeedbackText() throws Exception {
		Registration student = registerStudent(uniqueEmail("dash-priv-stud"));
		String adminToken = registerAdmin(uniqueEmail("dash-priv-adm"));
		String marker = "private-feedback-" + UUID.randomUUID();
		insertCompletedAiFeedback(student.userId(), OffsetDateTime.now(ZoneOffset.UTC), marker);

		String response = mockMvc.perform(get("/api/v1/admin/dashboard")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(response).doesNotContain(marker);
	}

	private OffsetDateTime uniqueWindowStart() {
		// Non-zero seconds keep OffsetDateTime.toString() identical to the serialized JSON value.
		return OffsetDateTime.of(2001, 1, 1, 0, 0, 30, 0, ZoneOffset.UTC)
				.plusDays(ThreadLocalRandom.current().nextLong(0, 2000))
				.plusMinutes(ThreadLocalRandom.current().nextLong(0, 1200));
	}

	private void insertPaidPayment(Long studentUserId, BigDecimal amount, OffsetDateTime createdAt) {
		jdbcTemplate.update("""
				insert into payments (student_user_id, provider, status, amount, currency, created_at, paid_at, updated_at)
				values (?, 'dash-test', 'paid', ?, 'UZS', ?, ?, ?)
				""", studentUserId, amount, createdAt, createdAt, createdAt);
	}

	private void insertMismatchPayment(Long studentUserId, BigDecimal amount, OffsetDateTime createdAt) {
		jdbcTemplate.update("""
				insert into payments (student_user_id, provider, status, amount, currency, failure_code, created_at, updated_at)
				values (?, 'dash-test', 'failed', ?, 'UZS', 'PAYMENT_AMOUNT_MISMATCH', ?, ?)
				""", studentUserId, amount, createdAt, createdAt);
	}

	private void insertCompletedAiFeedback(Long studentUserId, OffsetDateTime createdAt, String improvedAnswer) {
		jdbcTemplate.update("""
				insert into ai_feedbacks (
					student_user_id, feedback_type, status, provider, model,
					prompt_tokens, completion_tokens, estimated_cost, improved_answer, created_at, completed_at
				)
				values (?, 'study_plan', 'completed', 'mock', 'dash-test-model', 10, 5, 0.5, ?, ?, ?)
				""", studentUserId, improvedAnswer, createdAt, createdAt);
	}

	private String registerAdmin(String email) throws Exception {
		Registration registration = registerStudent(email);
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select u.id, r.id
				from users u
				join roles r on r.code = 'admin'
				where u.email = ?
				""", email);
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
		return new Registration(
				root.at("/data/accessToken").asText(),
				root.at("/data/user/id").asLong());
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private record Registration(String accessToken, Long userId) {
	}
}
