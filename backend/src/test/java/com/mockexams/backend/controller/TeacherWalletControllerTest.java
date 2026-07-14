package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import com.mockexams.backend.service.TeacherWalletService;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherWalletControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private TeacherWalletService teacherWalletService;

	@BeforeEach
	void setUp() {
		cleanFixtures();
		updateCommissionPercent("25.00");
	}

	@AfterEach
	void tearDown() {
		cleanFixtures();
		updateCommissionPercent("25.00");
	}

	@Test
	void teacherReadsOnlyOwnWalletSummaryAndTransactionHistory() throws Exception {
		TeacherFixture owner = approvedTeacher("wallet-owner");
		TeacherFixture other = approvedTeacher("wallet-other");
		String ownerToken = login(owner.email());
		String otherToken = login(other.email());
		Long studentId = activeUser("wallet-api-student", "student");
		Long testId = publishedTest(owner.userId(), owner.profileId(), "Wallet API Reading");
		Long listingId = activeListing(owner.profileId(), testId, "Wallet API Listing", "20000.00", "UZS");
		Long paymentId = paidPayment(studentId, listingId, "20000.00", "UZS");
		Long purchaseId = purchase(studentId, listingId, paymentId, "payment", "payment:" + paymentId, "20000.00", "UZS");
		teacherWalletService.recordMarketplacePurchaseEarning(purchaseId);

		mockMvc.perform(get("/api/v1/teacher/wallet")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.teacherProfileId").value(owner.profileId()))
				.andExpect(jsonPath("$.data.balanceAmount").value(15000.00))
				.andExpect(jsonPath("$.data.pendingAmount").value(0.00))
				.andExpect(jsonPath("$.data.currency").value("UZS"));

		mockMvc.perform(get("/api/v1/teacher/wallet/transactions")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].purchaseId").value(purchaseId))
				.andExpect(jsonPath("$.data[0].paymentId").value(paymentId))
				.andExpect(jsonPath("$.data[0].netAmount").value(15000.00))
				.andExpect(jsonPath("$.meta.page.totalElements").value(1));

		mockMvc.perform(get("/api/v1/teacher/wallet")
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.teacherProfileId").value(other.profileId()))
				.andExpect(jsonPath("$.data.balanceAmount").value(0.00));
		mockMvc.perform(get("/api/v1/teacher/wallet/transactions")
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(0))
				.andExpect(jsonPath("$.meta.page.totalElements").value(0));
	}

	@Test
	void studentCannotReadTeacherWallet() throws Exception {
		Long studentId = activeUser("wallet-api-forbidden", "student");
		String studentEmail = jdbcTemplate.queryForObject("select email from users where id = ?", String.class, studentId);
		String studentToken = login(studentEmail);

		mockMvc.perform(get("/api/v1/teacher/wallet")
						.header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
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

	private void updateCommissionPercent(String percent) {
		jdbcTemplate.update("""
				update platform_settings
				set setting_value = ?
				where setting_key = 'marketplace.default_commission_percent'
				""", "{\"value\": " + percent + "}");
	}

	private TeacherFixture approvedTeacher(String prefix) {
		String email = prefix + "-" + UUID.randomUUID() + "@example.test";
		Long userId = activeUser(prefix + "-teacher", email, "teacher");
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

	private String login(String email) throws Exception {
		String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
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

	private Long publishedTest(Long userId, Long teacherProfileId, String title) {
		return insertAndReturnId("""
				insert into tests (
				  teacher_profile_id, title, description, type, difficulty, estimated_band_min,
				  estimated_band_max, duration_minutes, price_amount, currency, is_free, language,
				  visibility, published_at, created_by_user_id
				)
				values (?, ?, 'Wallet API fixture.', 'reading', 'intermediate', 5.5, 7.0, 60, 20000.00, 'UZS', false, 'en', 'published', ?, ?)
				""",
				teacherProfileId,
				title,
				now(),
				userId);
	}

	private Long activeListing(Long teacherProfileId, Long testId, String title, String price, String currency) {
		return insertAndReturnId("""
				insert into marketplace_listings (
				  teacher_profile_id, test_id, title, description, listing_type, price_amount, currency, is_active, language
				)
				values (?, ?, ?, 'Wallet API listing fixture.', 'single_test', ?, ?, true, 'en')
				""",
				teacherProfileId,
				testId,
				title,
				new BigDecimal(price),
				currency);
	}

	private Long paidPayment(Long studentId, Long listingId, String amount, String currency) {
		OffsetDateTime occurredAt = now();
		return insertAndReturnId("""
				insert into payments (
				  student_user_id, provider, provider_payment_id, status, amount, currency, target_type,
				  target_listing_id, created_at, paid_at, confirmation_dispatched_at
				)
				values (?, 'mock', ?, 'paid', ?, ?, 'marketplace_listing', ?, ?, ?, ?)
				""",
				studentId,
				"mock-" + UUID.randomUUID(),
				new BigDecimal(amount),
				currency,
				listingId,
				occurredAt,
				occurredAt,
				occurredAt);
	}

	private Long purchase(
			Long studentId,
			Long listingId,
			Long paymentId,
			String sourceType,
			String sourceReference,
			String amount,
			String currency
	) {
		return insertAndReturnId("""
				insert into purchases (
				  student_user_id, listing_id, payment_id, status, source_type, source_reference,
				  amount, currency, purchased_at
				)
				values (?, ?, ?, 'granted', ?, ?, ?, ?, ?)
				""",
				studentId,
				listingId,
				paymentId,
				sourceType,
				sourceReference,
				new BigDecimal(amount),
				currency,
				now());
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

	private record TeacherFixture(Long userId, Long profileId, String email) {
	}
}
