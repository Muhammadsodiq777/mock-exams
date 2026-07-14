package com.mockexams.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;

import com.mockexams.backend.dto.WithdrawalRequestCDO;
import com.mockexams.backend.dto.WithdrawalRequestRDO;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@ActiveProfiles("test")
class WithdrawalServiceTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private WithdrawalService withdrawalService;

	@Autowired
	private UserRepository userRepository;

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
	void withdrawalBelowMinimumIsRejected() {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-minimum", "50000.00");
		updateMinimumWithdrawalAmount("15000.00");

		assertBusinessError(
				() -> withdrawalService.create(user(teacher.userId()), createCommand("14999.99")),
				"WITHDRAWAL_BELOW_MINIMUM",
				HttpStatus.BAD_REQUEST);

		assertThat(withdrawalCount()).isZero();
		assertThat(walletBalance(teacher.profileId())).isEqualByComparingTo("50000.00");
		assertThat(walletPending(teacher.profileId())).isEqualByComparingTo("0.00");
	}

	@Test
	void teacherCannotWithdrawMoreThanAvailableBalance() {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-available", "20000.00");

		assertBusinessError(
				() -> withdrawalService.create(user(teacher.userId()), createCommand("20000.01")),
				"WALLET_INSUFFICIENT_BALANCE",
				HttpStatus.CONFLICT);

		assertThat(withdrawalCount()).isZero();
		assertThat(walletBalance(teacher.profileId())).isEqualByComparingTo("20000.00");
		assertThat(walletPending(teacher.profileId())).isEqualByComparingTo("0.00");
	}

	@Test
	void createReservesBalanceAndNotifiesTeacher() {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-create", "50000.00");

		WithdrawalRequestRDO created = withdrawalService.create(user(teacher.userId()), createCommand("20000.00"));

		assertThat(created.status()).isEqualTo("requested");
		assertThat(created.requestedAmount()).isEqualByComparingTo("20000.00");
		assertThat(created.currency()).isEqualTo("UZS");
		assertThat(created.payoutDetails()).containsEntry("method", "card");
		assertThat(walletBalance(teacher.profileId())).isEqualByComparingTo("30000.00");
		assertThat(walletPending(teacher.profileId())).isEqualByComparingTo("20000.00");
		assertThat(withdrawalNotificationCount(teacher.userId(), "requested")).isEqualTo(1);
	}

	@Test
	void invalidPayoutDetailsAreRejected() {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-payout", "50000.00");
		User teacherUser = user(teacher.userId());

		Map<String, Object> missingMethod = new LinkedHashMap<>();
		missingMethod.put("cardNumber", "8600123412341234");
		missingMethod.put("cardHolder", "Teacher Payout");
		assertBusinessError(
				() -> withdrawalService.create(teacherUser, createCommand("20000.00", missingMethod)),
				"WITHDRAWAL_PAYOUT_DETAILS_INVALID",
				HttpStatus.BAD_REQUEST);

		Map<String, Object> badCardNumber = cardPayoutDetails();
		badCardNumber.put("cardNumber", "not-a-card");
		assertBusinessError(
				() -> withdrawalService.create(teacherUser, createCommand("20000.00", badCardNumber)),
				"WITHDRAWAL_PAYOUT_DETAILS_INVALID",
				HttpStatus.BAD_REQUEST);

		Map<String, Object> unknownKey = cardPayoutDetails();
		unknownKey.put("unexpected", "value");
		assertBusinessError(
				() -> withdrawalService.create(teacherUser, createCommand("20000.00", unknownKey)),
				"WITHDRAWAL_PAYOUT_DETAILS_INVALID",
				HttpStatus.BAD_REQUEST);

		assertThat(withdrawalCount()).isZero();
		assertThat(walletBalance(teacher.profileId())).isEqualByComparingTo("50000.00");
	}

	@Test
	void bankTransferPayoutDetailsAreAccepted() {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-bank", "50000.00");
		Map<String, Object> bankDetails = new LinkedHashMap<>();
		bankDetails.put("method", "bank_transfer");
		bankDetails.put("bankName", "Test Bank");
		bankDetails.put("accountNumber", "20208000123456789");
		bankDetails.put("accountHolder", "Teacher Payout");

		WithdrawalRequestRDO created = withdrawalService.create(user(teacher.userId()), createCommand("20000.00", bankDetails));

		assertThat(created.status()).isEqualTo("requested");
		assertThat(created.payoutDetails()).containsEntry("method", "bank_transfer");
	}

	@Test
	void rejectedWithdrawalReleasesReservationAndRecordsDecision() {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-reject", "50000.00");
		Long adminId = activeUser("wd-reject-admin", "admin");
		WithdrawalRequestRDO created = withdrawalService.create(user(teacher.userId()), createCommand("20000.00"));

		withdrawalService.reject(user(adminId), created.id(), "Payout details could not be verified.");

		assertThat(walletBalance(teacher.profileId())).isEqualByComparingTo("50000.00");
		assertThat(walletPending(teacher.profileId())).isEqualByComparingTo("0.00");
		assertThat(jdbcTemplate.queryForMap("select status, reviewed_by_user_id, reviewed_at, rejection_reason from withdrawal_requests where id = " + created.id()))
				.containsEntry("status", "rejected")
				.containsEntry("reviewed_by_user_id", adminId)
				.containsEntry("rejection_reason", "Payout details could not be verified.");
		assertThat(jdbcTemplate.queryForObject(
				"select reviewed_at from withdrawal_requests where id = ?", java.sql.Timestamp.class, created.id())).isNotNull();
		assertThat(withdrawalNotificationCount(teacher.userId(), "rejected")).isEqualTo(1);
		assertThat(ledgerWithdrawalCount()).isZero();
	}

	@Test
	void adminRejectionRequiresReason() {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-reason", "50000.00");
		Long adminId = activeUser("wd-reason-admin", "admin");
		WithdrawalRequestRDO created = withdrawalService.create(user(teacher.userId()), createCommand("20000.00"));

		assertBusinessError(
				() -> withdrawalService.reject(user(adminId), created.id(), "   "),
				"VALIDATION_FAILED",
				HttpStatus.BAD_REQUEST);

		assertThat(walletPending(teacher.profileId())).isEqualByComparingTo("20000.00");
		assertThat(jdbcTemplate.queryForObject(
				"select status from withdrawal_requests where id = ?", String.class, created.id())).isEqualTo("requested");
	}

	@Test
	void approvedWithdrawalCreatesLedgerTransactionOnceAndSettlesReservation() {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-approve", "50000.00");
		Long adminId = activeUser("wd-approve-admin", "admin");
		WithdrawalRequestRDO created = withdrawalService.create(user(teacher.userId()), createCommand("20000.00"));

		withdrawalService.approve(user(adminId), created.id());

		assertThat(walletBalance(teacher.profileId())).isEqualByComparingTo("30000.00");
		assertThat(walletPending(teacher.profileId())).isEqualByComparingTo("0.00");
		assertThat(ledgerWithdrawalCount()).isEqualTo(1);
		assertThat(jdbcTemplate.queryForMap("select transaction_type, source_type, source_reference from teacher_transactions"))
				.containsEntry("transaction_type", "withdrawal")
				.containsEntry("source_type", "withdrawal_request")
				.containsEntry("source_reference", "withdrawal:" + created.id());
		assertThat(jdbcTemplate.queryForMap("select status, reviewed_by_user_id from withdrawal_requests where id = " + created.id()))
				.containsEntry("status", "approved")
				.containsEntry("reviewed_by_user_id", adminId);
		assertThat(withdrawalNotificationCount(teacher.userId(), "approved")).isEqualTo(1);

		assertBusinessError(
				() -> withdrawalService.approve(user(adminId), created.id()),
				"WITHDRAWAL_INVALID_STATUS",
				HttpStatus.CONFLICT);
		assertThat(ledgerWithdrawalCount()).isEqualTo(1);
		assertThat(walletBalance(teacher.profileId())).isEqualByComparingTo("30000.00");
		assertThat(walletPending(teacher.profileId())).isEqualByComparingTo("0.00");
	}

	@Test
	void teacherCancelReleasesReservation() {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-cancel", "50000.00");
		WithdrawalRequestRDO created = withdrawalService.create(user(teacher.userId()), createCommand("20000.00"));

		WithdrawalRequestRDO cancelled = withdrawalService.cancel(user(teacher.userId()), created.id());

		assertThat(cancelled.status()).isEqualTo("cancelled");
		assertThat(walletBalance(teacher.profileId())).isEqualByComparingTo("50000.00");
		assertThat(walletPending(teacher.profileId())).isEqualByComparingTo("0.00");
		assertThat(withdrawalNotificationCount(teacher.userId(), "cancelled")).isEqualTo(1);
	}

	@Test
	void teacherCannotCancelApprovedOrPaidWithdrawal() {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-cancel-late", "50000.00");
		Long adminId = activeUser("wd-cancel-late-admin", "admin");
		WithdrawalRequestRDO created = withdrawalService.create(user(teacher.userId()), createCommand("20000.00"));
		withdrawalService.approve(user(adminId), created.id());

		assertBusinessError(
				() -> withdrawalService.cancel(user(teacher.userId()), created.id()),
				"WITHDRAWAL_INVALID_STATUS",
				HttpStatus.CONFLICT);

		withdrawalService.markPaid(user(adminId), created.id());

		assertBusinessError(
				() -> withdrawalService.cancel(user(teacher.userId()), created.id()),
				"WITHDRAWAL_INVALID_STATUS",
				HttpStatus.CONFLICT);
		assertThat(walletBalance(teacher.profileId())).isEqualByComparingTo("30000.00");
		assertThat(walletPending(teacher.profileId())).isEqualByComparingTo("0.00");
	}

	@Test
	void markPaidRequiresApprovedStatusAndNotifiesTeacher() {
		TeacherFixture teacher = approvedTeacherWithWallet("wd-paid", "50000.00");
		Long adminId = activeUser("wd-paid-admin", "admin");
		WithdrawalRequestRDO created = withdrawalService.create(user(teacher.userId()), createCommand("20000.00"));

		assertBusinessError(
				() -> withdrawalService.markPaid(user(adminId), created.id()),
				"WITHDRAWAL_INVALID_STATUS",
				HttpStatus.CONFLICT);

		withdrawalService.approve(user(adminId), created.id());
		withdrawalService.markPaid(user(adminId), created.id());

		assertThat(jdbcTemplate.queryForObject(
				"select status from withdrawal_requests where id = ?", String.class, created.id())).isEqualTo("paid");
		assertThat(jdbcTemplate.queryForObject(
				"select paid_at from withdrawal_requests where id = ?", java.sql.Timestamp.class, created.id())).isNotNull();
		assertThat(withdrawalNotificationCount(teacher.userId(), "paid")).isEqualTo(1);
	}

	@Test
	void nonOwnerTeacherCannotViewOrCancelAnotherTeachersWithdrawal() {
		TeacherFixture owner = approvedTeacherWithWallet("wd-owner", "50000.00");
		TeacherFixture other = approvedTeacherWithWallet("wd-other", "50000.00");
		WithdrawalRequestRDO created = withdrawalService.create(user(owner.userId()), createCommand("20000.00"));

		assertBusinessError(
				() -> withdrawalService.myWithdrawal(user(other.userId()), created.id()),
				"WITHDRAWAL_NOT_FOUND",
				HttpStatus.NOT_FOUND);
		assertBusinessError(
				() -> withdrawalService.cancel(user(other.userId()), created.id()),
				"WITHDRAWAL_NOT_FOUND",
				HttpStatus.NOT_FOUND);

		assertThat(withdrawalService.myWithdrawals(user(other.userId()), null, PageRequest.of(0, 20)).getTotalElements()).isZero();
		assertThat(withdrawalService.myWithdrawals(user(owner.userId()), null, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
		assertThat(walletPending(owner.profileId())).isEqualByComparingTo("20000.00");
	}

	private WithdrawalRequestCDO createCommand(String amount) {
		return createCommand(amount, cardPayoutDetails());
	}

	private WithdrawalRequestCDO createCommand(String amount, Map<String, Object> payoutDetails) {
		return new WithdrawalRequestCDO(new BigDecimal(amount), null, payoutDetails);
	}

	private Map<String, Object> cardPayoutDetails() {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("method", "card");
		details.put("cardNumber", "8600 1234 1234 1234");
		details.put("cardHolder", "Teacher Payout");
		return details;
	}

	private void assertBusinessError(ThrowingRunnable runnable, String code, HttpStatus status) {
		assertThatThrownBy(runnable::run)
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo(code);
					assertThat(ex.getStatus()).isEqualTo(status);
				});
	}

	private User user(Long userId) {
		return userRepository.findById(userId).orElseThrow();
	}

	private int withdrawalCount() {
		return jdbcTemplate.queryForObject("select count(*) from withdrawal_requests", Integer.class);
	}

	private int ledgerWithdrawalCount() {
		return jdbcTemplate.queryForObject(
				"select count(*) from teacher_transactions where transaction_type = 'withdrawal'", Integer.class);
	}

	private int withdrawalNotificationCount(Long userId, String status) {
		return jdbcTemplate.queryForObject("""
				select count(*) from notifications
				where user_id = ? and type = 'withdrawal' and cast(payload as varchar) like ?
				""", Integer.class, userId, "%\"status\":\"" + status + "\"%");
	}

	private BigDecimal walletBalance(Long teacherProfileId) {
		return jdbcTemplate.queryForObject(
				"select balance_amount from teacher_wallets where teacher_profile_id = ?", BigDecimal.class, teacherProfileId);
	}

	private BigDecimal walletPending(Long teacherProfileId) {
		return jdbcTemplate.queryForObject(
				"select pending_amount from teacher_wallets where teacher_profile_id = ?", BigDecimal.class, teacherProfileId);
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
		Long userId = activeUser(prefix + "-teacher", "teacher");
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
		return new TeacherFixture(userId, profileId);
	}

	private Long activeUser(String prefix, String roleCode) {
		Long userId = insertAndReturnId("""
				insert into users (email, password_hash, full_name, status, preferred_language)
				values (?, 'hash', ?, 'active', 'en')
				""",
				prefix + "-" + UUID.randomUUID() + "@example.test",
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

	private record TeacherFixture(Long userId, Long profileId) {
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run();
	}
}
