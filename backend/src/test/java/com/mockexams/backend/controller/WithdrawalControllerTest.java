package com.mockexams.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
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
class WithdrawalControllerTest {

	private static final String PASSWORD = "correct-password-123";
	private static final String CARD_PAYOUT_DETAILS = """
			{
			  "method": "card",
			  "cardNumber": "8600 1234 1234 1234",
			  "cardHolder": "Teacher Payout"
			}
			""";

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
		updateMinimumWithdrawalAmount("10000.00");
	}

	@AfterEach
	void tearDown() {
		cleanFixtures();
		updateMinimumWithdrawalAmount("10000.00");
	}

	@Test
	void teacherCreatesListsReadsAndCancelsOwnWithdrawal() throws Exception {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-http-teacher", "50000.00");
		String teacherToken = login(teacher.email());

		String createResponse = mockMvc.perform(post("/api/v1/teacher/wallet/withdrawals")
						.header("Authorization", "Bearer " + teacherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody("20000.00")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.teacherProfileId").value(teacher.profileId()))
				.andExpect(jsonPath("$.data.status").value("requested"))
				.andExpect(jsonPath("$.data.requestedAmount").value(20000.00))
				.andExpect(jsonPath("$.data.currency").value("UZS"))
				.andExpect(jsonPath("$.data.payoutDetails.method").value("card"))
				.andExpect(jsonPath("$.data.payoutDetails.cardNumber").value("**** **** **** 1234"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		// Full number is still persisted (validated at create) even though the RDO masks it.
		assertThat(jdbcTemplate.queryForObject("""
				select cast(payout_details as varchar) from withdrawal_requests
				where teacher_profile_id = ?
				""", String.class, teacher.profileId())).contains("8600123412341234");
		Long withdrawalId = objectMapper.readTree(createResponse).at("/data/id").asLong();

		mockMvc.perform(get("/api/v1/teacher/wallet/withdrawals")
						.header("Authorization", "Bearer " + teacherToken)
						.param("status", "requested"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(withdrawalId))
				.andExpect(jsonPath("$.meta.page.totalElements").value(1))
				.andExpect(jsonPath("$.meta.filters.status").value("requested"));

		mockMvc.perform(get("/api/v1/teacher/wallet/withdrawals/" + withdrawalId)
						.header("Authorization", "Bearer " + teacherToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(withdrawalId))
				.andExpect(jsonPath("$.data.status").value("requested"));

		mockMvc.perform(post("/api/v1/teacher/wallet/withdrawals/" + withdrawalId + "/cancel")
						.header("Authorization", "Bearer " + teacherToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("cancelled"));

		mockMvc.perform(post("/api/v1/teacher/wallet/withdrawals/" + withdrawalId + "/cancel")
						.header("Authorization", "Bearer " + teacherToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("WITHDRAWAL_INVALID_STATUS"));
	}

	@Test
	void createBelowMinimumReturnsStableBusinessError() throws Exception {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-http-minimum", "50000.00");
		String teacherToken = login(teacher.email());

		mockMvc.perform(post("/api/v1/teacher/wallet/withdrawals")
						.header("Authorization", "Bearer " + teacherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody("9999.99")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("WITHDRAWAL_BELOW_MINIMUM"));
	}

	@Test
	void adminReviewsQueueApprovesRejectsAndMarksPaid() throws Exception {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-http-admin", "50000.00");
		Long adminId = activeUser("wd-http-admin-user", "admin");
		String adminEmail = jdbcTemplate.queryForObject("select email from users where id = ?", String.class, adminId);
		String teacherToken = login(teacher.email());
		String adminToken = login(adminEmail);
		Long firstId = createWithdrawal(teacherToken, "15000.00");
		Long secondId = createWithdrawal(teacherToken, "10000.00");

		mockMvc.perform(get("/api/v1/admin/withdrawals")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "requested")
						.param("teacherProfileId", String.valueOf(teacher.profileId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].teacherProfileId").value(teacher.profileId()))
				.andExpect(jsonPath("$.data[0].teacherDisplayName").value("Teacher wd-http-admin"))
				.andExpect(jsonPath("$.meta.page.totalElements").value(2));

		mockMvc.perform(get("/api/v1/admin/withdrawals/" + firstId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(firstId))
				.andExpect(jsonPath("$.data.payoutDetails.method").value("card"));

		mockMvc.perform(post("/api/v1/admin/withdrawals/" + firstId + "/approve")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("approved"))
				.andExpect(jsonPath("$.data.reviewedByUserId").value(adminId));

		mockMvc.perform(post("/api/v1/admin/withdrawals/" + secondId + "/reject")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\": \"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/admin/withdrawals/" + secondId + "/reject")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\": \"Payout details could not be verified.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("rejected"))
				.andExpect(jsonPath("$.data.rejectionReason").value("Payout details could not be verified."));

		mockMvc.perform(post("/api/v1/admin/withdrawals/" + firstId + "/mark-paid")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("paid"))
				.andExpect(jsonPath("$.data.paidAt").isNotEmpty());
	}

	@Test
	void teacherDetailAndAdminQueueRdosMaskCardAndBankPayoutNumbers() throws Exception {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-http-mask", "50000.00");
		Long adminId = activeUser("wd-http-mask-admin", "admin");
		String adminEmail = jdbcTemplate.queryForObject("select email from users where id = ?", String.class, adminId);
		String teacherToken = login(teacher.email());
		String adminToken = login(adminEmail);

		String cardResponse = mockMvc.perform(post("/api/v1/teacher/wallet/withdrawals")
						.header("Authorization", "Bearer " + teacherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody("20000.00")))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Long cardWithdrawalId = objectMapper.readTree(cardResponse).at("/data/id").asLong();

		String bankBody = """
				{
				  "amount": 15000.00,
				  "payoutDetails": {
				    "method": "bank_transfer",
				    "bankName": "Test Bank",
				    "accountNumber": "20208000123456789",
				    "accountHolder": "Teacher Payout"
				  }
				}
				""";
		String bankResponse = mockMvc.perform(post("/api/v1/teacher/wallet/withdrawals")
						.header("Authorization", "Bearer " + teacherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(bankBody))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Long bankWithdrawalId = objectMapper.readTree(bankResponse).at("/data/id").asLong();

		// Teacher detail RDO masks both payout methods; never the full number.
		mockMvc.perform(get("/api/v1/teacher/wallet/withdrawals/" + cardWithdrawalId)
						.header("Authorization", "Bearer " + teacherToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.payoutDetails.cardNumber").value("**** **** **** 1234"))
				.andExpect(jsonPath("$.data.payoutDetails.cardHolder").value("Teacher Payout"));
		mockMvc.perform(get("/api/v1/teacher/wallet/withdrawals/" + bankWithdrawalId)
						.header("Authorization", "Bearer " + teacherToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.payoutDetails.accountNumber").value("…6789"))
				.andExpect(jsonPath("$.data.payoutDetails.bankName").value("Test Bank"))
				.andExpect(jsonPath("$.data.payoutDetails.accountHolder").value("Teacher Payout"));

		// Admin queue and detail RDOs mask the same way.
		mockMvc.perform(get("/api/v1/admin/withdrawals")
						.header("Authorization", "Bearer " + adminToken)
						.param("teacherProfileId", String.valueOf(teacher.profileId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[?(@.id == " + cardWithdrawalId + ")].payoutDetails.cardNumber")
						.value("**** **** **** 1234"))
				.andExpect(jsonPath("$.data[?(@.id == " + bankWithdrawalId + ")].payoutDetails.accountNumber")
						.value("…6789"));
		mockMvc.perform(get("/api/v1/admin/withdrawals/" + cardWithdrawalId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.payoutDetails.cardNumber").value("**** **** **** 1234"));

		String cardBody = mockMvc.perform(get("/api/v1/admin/withdrawals/" + cardWithdrawalId)
						.header("Authorization", "Bearer " + adminToken))
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(cardBody).doesNotContain("8600123412341234");
		String bankBodyResponse = mockMvc.perform(get("/api/v1/admin/withdrawals/" + bankWithdrawalId)
						.header("Authorization", "Bearer " + adminToken))
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(bankBodyResponse).doesNotContain("20208000123456789");
	}

	@Test
	void nonOwnerTeacherCannotViewOrCancelAnotherTeachersWithdrawal() throws Exception {
		TeacherFixture owner = approvedTeacherWithWallet("wd-http-owner", "50000.00");
		TeacherFixture other = approvedTeacherWithWallet("wd-http-other", "50000.00");
		String ownerToken = login(owner.email());
		String otherToken = login(other.email());
		Long withdrawalId = createWithdrawal(ownerToken, "20000.00");

		mockMvc.perform(get("/api/v1/teacher/wallet/withdrawals/" + withdrawalId)
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("WITHDRAWAL_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/teacher/wallet/withdrawals/" + withdrawalId + "/cancel")
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("WITHDRAWAL_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/teacher/wallet/withdrawals")
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	void roleBoundariesAreEnforced() throws Exception {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-http-roles", "50000.00");
		Long studentId = activeUser("wd-http-student", "student");
		String studentEmail = jdbcTemplate.queryForObject("select email from users where id = ?", String.class, studentId);
		String teacherToken = login(teacher.email());
		String studentToken = login(studentEmail);
		Long withdrawalId = createWithdrawal(teacherToken, "20000.00");

		mockMvc.perform(post("/api/v1/teacher/wallet/withdrawals")
						.header("Authorization", "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody("20000.00")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(get("/api/v1/admin/withdrawals")
						.header("Authorization", "Bearer " + teacherToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(post("/api/v1/admin/withdrawals/" + withdrawalId + "/approve")
						.header("Authorization", "Bearer " + teacherToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(get("/api/v1/teacher/wallet/withdrawals"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
	}

	private Long createWithdrawal(String teacherToken, String amount) throws Exception {
		String response = mockMvc.perform(post("/api/v1/teacher/wallet/withdrawals")
						.header("Authorization", "Bearer " + teacherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody(amount)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).at("/data/id").asLong();
	}

	private String createBody(String amount) {
		return """
				{
				  "amount": %s,
				  "payoutDetails": %s
				}
				""".formatted(amount, CARD_PAYOUT_DETAILS);
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
		JsonNode root = objectMapper.readTree(response);
		return root.at("/data/accessToken").asText();
	}

	private void updateMinimumWithdrawalAmount(String amount) {
		jdbcTemplate.update("""
				update platform_settings
				set setting_value = ?
				where setting_key = 'wallet.minimum_withdrawal_amount'
				""", "{\"value\": " + amount + ", \"currency\": \"UZS\"}");
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

	private TeacherFixture approvedTeacherWithWallet(String prefix, String balance) {
		String email = prefix + "-" + UUID.randomUUID() + "@example.test";
		Long userId = activeUser(prefix + "-teacher", email, "teacher");
		Long profileId = insertAndReturnId("""
				insert into teacher_profiles (
				  user_id, display_name, headline, bio, status, approved_by_user_id, approved_at
				)
				values (?, ?, 'IELTS teacher', 'Experienced teacher.', 'approved', ?, current_timestamp)
				""",
				userId,
				"Teacher " + prefix,
				userId);
		jdbcTemplate.update("""
				insert into teacher_wallets (teacher_profile_id, balance_amount, pending_amount, currency)
				values (?, ?, 0, 'UZS')
				""", profileId, new BigDecimal(balance));
		return new TeacherFixture(userId, profileId, email);
	}

	private Long activeUser(String prefix, String roleCode) {
		return activeUser(prefix, prefix + "-" + UUID.randomUUID() + "@example.test", roleCode);
	}

	private Long activeUser(String prefix, String email, String roleCode) {
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

	private record TeacherFixture(Long userId, Long profileId, String email) {
	}
}
