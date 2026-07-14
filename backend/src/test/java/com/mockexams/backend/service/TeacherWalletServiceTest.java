package com.mockexams.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;

import com.mockexams.backend.dto.TeacherWalletReportAggregateRDO;
import com.mockexams.backend.dto.TeacherWalletTransactionRDO;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.repository.TeacherTransactionRepository;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@ActiveProfiles("test")
class TeacherWalletServiceTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TeacherWalletService teacherWalletService;

	@Autowired
	private TeacherTransactionRepository teacherTransactionRepository;

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
	void marketplacePurchaseEarningUsesCommissionSettingAndIsIdempotent() {
		TeacherFixture teacher = approvedTeacher("wallet-earning");
		Long studentId = activeUser("wallet-earning-student", "student");
		Long testId = publishedTest(teacher.userId(), teacher.profileId(), "Wallet Paid Reading", false);
		Long listingId = activeListing(teacher.profileId(), testId, "Wallet Paid Listing", "20000.00", "UZS");
		Long paymentId = paidPayment(studentId, listingId, "20000.00", "UZS");
		Long purchaseId = purchase(studentId, listingId, paymentId, "payment", "payment:" + paymentId, "20000.00", "UZS");

		Optional<TeacherWalletTransactionRDO> first = teacherWalletService.recordMarketplacePurchaseEarning(purchaseId);
		Optional<TeacherWalletTransactionRDO> duplicate = teacherWalletService.recordMarketplacePurchaseEarning(purchaseId);

		assertThat(first).isPresent();
		assertThat(duplicate).isPresent();
		assertThat(duplicate.get().id()).isEqualTo(first.get().id());
		assertThat(first.get().grossAmount()).isEqualByComparingTo("20000.00");
		assertThat(first.get().platformCommissionAmount()).isEqualByComparingTo("5000.00");
		assertThat(first.get().netAmount()).isEqualByComparingTo("15000.00");
		assertThat(first.get().paymentId()).isEqualTo(paymentId);
		assertThat(jdbcTemplate.queryForObject("select count(*) from teacher_transactions", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject(
				"select balance_amount from teacher_wallets where teacher_profile_id = ?",
				BigDecimal.class,
				teacher.profileId())).isEqualByComparingTo("15000.00");
	}

	@Test
	void freeMarketplacePurchaseDoesNotCreateEarning() {
		TeacherFixture teacher = approvedTeacher("wallet-free");
		Long studentId = activeUser("wallet-free-student", "student");
		Long testId = publishedTest(teacher.userId(), teacher.profileId(), "Wallet Free Reading", true);
		Long listingId = activeListing(teacher.profileId(), testId, "Wallet Free Listing", "0.00", "UZS");
		Long purchaseId = purchase(studentId, listingId, null, "free_listing", "listing:" + listingId, "0.00", "UZS");

		assertThat(teacherWalletService.recordMarketplacePurchaseEarning(purchaseId)).isEmpty();

		assertThat(jdbcTemplate.queryForObject("select count(*) from teacher_transactions", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from teacher_wallets", Integer.class)).isZero();
	}

	@Test
	void currencyMismatchBetweenWalletAndPurchaseIsRejected() {
		TeacherFixture teacher = approvedTeacher("wallet-currency");
		Long studentId = activeUser("wallet-currency-student", "student");
		Long firstTestId = publishedTest(teacher.userId(), teacher.profileId(), "Wallet UZS Reading", false);
		Long firstListingId = activeListing(teacher.profileId(), firstTestId, "Wallet UZS Listing", "20000.00", "UZS");
		Long firstPurchaseId = purchase(studentId, firstListingId, null, "payment", "payment:uzs", "20000.00", "UZS");
		teacherWalletService.recordMarketplacePurchaseEarning(firstPurchaseId);
		Long secondTestId = publishedTest(teacher.userId(), teacher.profileId(), "Wallet USD Reading", false);
		Long secondListingId = activeListing(teacher.profileId(), secondTestId, "Wallet USD Listing", "20.00", "USD");
		Long secondPurchaseId = purchase(studentId, secondListingId, null, "payment", "payment:usd", "20.00", "USD");

		assertThatThrownBy(() -> teacherWalletService.recordMarketplacePurchaseEarning(secondPurchaseId))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("WALLET_CURRENCY_MISMATCH");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				});
	}

	@Test
	void withdrawalReservationAndRefundCannotMakeWalletBalanceNegative() {
		TeacherFixture teacher = approvedTeacher("wallet-negative");
		Long studentId = activeUser("wallet-negative-student", "student");
		Long testId = publishedTest(teacher.userId(), teacher.profileId(), "Wallet Negative Reading", false);
		Long listingId = activeListing(teacher.profileId(), testId, "Wallet Negative Listing", "20000.00", "UZS");
		Long purchaseId = purchase(studentId, listingId, null, "payment", "payment:negative", "20000.00", "UZS");
		teacherWalletService.recordMarketplacePurchaseEarning(purchaseId);

		teacherWalletService.reserveWithdrawal(teacher.profileId(), new BigDecimal("10000.00"), "UZS", "withdrawal:1");

		assertThat(jdbcTemplate.queryForObject(
				"select balance_amount from teacher_wallets where teacher_profile_id = ?",
				BigDecimal.class,
				teacher.profileId())).isEqualByComparingTo("5000.00");
		assertThat(jdbcTemplate.queryForObject(
				"select pending_amount from teacher_wallets where teacher_profile_id = ?",
				BigDecimal.class,
				teacher.profileId())).isEqualByComparingTo("10000.00");
		assertInsufficientBalance(() -> teacherWalletService.reserveWithdrawal(
				teacher.profileId(),
				new BigDecimal("6000.00"),
				"UZS",
				"withdrawal:2"));
		assertInsufficientBalance(() -> teacherWalletService.recordMarketplacePurchaseRefund(
				purchaseId,
				"refund:" + purchaseId));
		assertThat(jdbcTemplate.queryForObject("select count(*) from teacher_transactions where transaction_type = 'refund'",
				Integer.class)).isZero();
	}

	@Test
	void recordApprovedWithdrawalIsSafeWhenALedgerTransactionAlreadyExistsForTheSameReference() {
		// Deterministic regression test for the double-approve race: recordApprovedWithdrawal used to
		// look up the existing "withdrawal_request" ledger transaction only once, before acquiring the
		// wallet's pessimistic write lock. Two concurrent approvals could both pass that lookup before
		// either committed, both settle the reservation, and race into the unique constraint on
		// (source_type, source_reference, transaction_type) - surfacing a raw DataIntegrityViolation
		// instead of a business error. The fix re-checks the lookup again after the lock is acquired.
		// A true two-thread test against H2 proved unreliable in this suite (timing-dependent), so this
		// test instead pins down the resulting contract directly: once a ledger transaction exists for
		// a source reference, calling recordApprovedWithdrawal again for that same reference must never
		// settle the wallet a second time and must return safely instead of throwing a raw SQL error.
		TeacherFixture teacher = approvedTeacher("wallet-withdrawal-race");
		Long studentId = activeUser("wallet-withdrawal-race-student", "student");
		Long testId = publishedTest(teacher.userId(), teacher.profileId(), "Wallet Race Reading", false);
		Long listingId = activeListing(teacher.profileId(), testId, "Wallet Race Listing", "20000.00", "UZS");
		Long purchaseId = purchase(studentId, listingId, null, "payment", "payment:race", "20000.00", "UZS");
		teacherWalletService.recordMarketplacePurchaseEarning(purchaseId);
		teacherWalletService.reserveWithdrawal(teacher.profileId(), new BigDecimal("15000.00"), "UZS", "withdrawal:race");

		TeacherWalletTransactionRDO first = teacherWalletService.recordApprovedWithdrawal(
				teacher.profileId(),
				new BigDecimal("15000.00"),
				"UZS",
				"withdrawal:race");

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from teacher_transactions where transaction_type = 'withdrawal'", Integer.class))
				.isOne();
		assertThat(jdbcTemplate.queryForObject(
				"select pending_amount from teacher_wallets where teacher_profile_id = ?",
				BigDecimal.class, teacher.profileId())).isEqualByComparingTo("0.00");

		// A second call with the same source reference (the exact idempotency key checked both before
		// and after the wallet lock) must return the already-recorded transaction rather than settling
		// the wallet again or attempting a duplicate insert.
		TeacherWalletTransactionRDO second = teacherWalletService.recordApprovedWithdrawal(
				teacher.profileId(),
				new BigDecimal("15000.00"),
				"UZS",
				"withdrawal:race");

		assertThat(second.id()).isEqualTo(first.id());
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from teacher_transactions where transaction_type = 'withdrawal'", Integer.class))
				.isOne();
		assertThat(jdbcTemplate.queryForObject(
				"select pending_amount from teacher_wallets where teacher_profile_id = ?",
				BigDecimal.class, teacher.profileId())).isEqualByComparingTo("0.00");
	}

	@Test
	void reportAggregatesGroupTransactionsByDateAndSource() {
		TeacherFixture teacher = approvedTeacher("wallet-report");
		TeacherFixture otherTeacher = approvedTeacher("wallet-report-other");
		Long studentId = activeUser("wallet-report-student", "student");
		Long secondStudentId = activeUser("wallet-report-student-2", "student");
		Long testId = publishedTest(teacher.userId(), teacher.profileId(), "Wallet Report Reading", false);
		Long listingId = activeListing(teacher.profileId(), testId, "Wallet Report Listing", "20000.00", "UZS");
		Long firstPurchaseId = purchase(studentId, listingId, null, "payment", "payment:report-1", "20000.00", "UZS");
		Long secondPurchaseId = purchase(secondStudentId, listingId, null, "payment", "payment:report-2", "20000.00", "UZS");
		Long firstEarningId = teacherWalletService.recordMarketplacePurchaseEarning(firstPurchaseId).orElseThrow().id();
		teacherWalletService.recordMarketplacePurchaseEarning(secondPurchaseId);
		teacherWalletService.reserveWithdrawal(teacher.profileId(), new BigDecimal("10000.00"), "UZS", "withdrawal:report");
		teacherWalletService.recordApprovedWithdrawal(
				teacher.profileId(),
				new BigDecimal("10000.00"),
				"UZS",
				"withdrawal:report");
		Long otherTestId = publishedTest(otherTeacher.userId(), otherTeacher.profileId(), "Wallet Report Other", false);
		Long otherListingId = activeListing(otherTeacher.profileId(), otherTestId, "Wallet Report Other Listing", "20000.00", "UZS");
		Long otherPurchaseId = purchase(studentId, otherListingId, null, "payment", "payment:report-other", "20000.00", "UZS");
		teacherWalletService.recordMarketplacePurchaseEarning(otherPurchaseId);
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		LocalDate yesterday = today.minusDays(1);
		jdbcTemplate.update(
				"update teacher_transactions set created_at = ? where id = ?",
				yesterday.atStartOfDay().atOffset(ZoneOffset.UTC).plusHours(12),
				firstEarningId);
		Long walletId = jdbcTemplate.queryForObject(
				"select id from teacher_wallets where teacher_profile_id = ?",
				Long.class,
				teacher.profileId());

		List<TeacherWalletReportAggregateRDO> aggregates =
				teacherTransactionRepository.reportAggregatesByDateAndSource(walletId, null, null);

		assertThat(aggregates).hasSize(3);
		TeacherWalletReportAggregateRDO todayEarnings = aggregateFor(aggregates, today, "marketplace_purchase");
		assertThat(todayEarnings.transactionCount()).isEqualTo(1);
		assertThat(todayEarnings.grossTotal()).isEqualByComparingTo("20000.00");
		assertThat(todayEarnings.platformCommissionTotal()).isEqualByComparingTo("5000.00");
		assertThat(todayEarnings.netTotal()).isEqualByComparingTo("15000.00");
		TeacherWalletReportAggregateRDO yesterdayEarnings = aggregateFor(aggregates, yesterday, "marketplace_purchase");
		assertThat(yesterdayEarnings.transactionCount()).isEqualTo(1);
		assertThat(yesterdayEarnings.netTotal()).isEqualByComparingTo("15000.00");
		TeacherWalletReportAggregateRDO withdrawals = aggregateFor(aggregates, today, "withdrawal_request");
		assertThat(withdrawals.transactionCount()).isEqualTo(1);
		assertThat(withdrawals.grossTotal()).isEqualByComparingTo("10000.00");
		assertThat(withdrawals.netTotal()).isEqualByComparingTo("10000.00");

		List<TeacherWalletReportAggregateRDO> yesterdayOnly = teacherTransactionRepository
				.reportAggregatesByDateAndSource(
						walletId,
						yesterday.atStartOfDay().atOffset(ZoneOffset.UTC),
						today.atStartOfDay().atOffset(ZoneOffset.UTC).minusSeconds(1));
		assertThat(yesterdayOnly).hasSize(1);
		assertThat(yesterdayOnly.get(0).transactionDate()).isEqualTo(yesterday);

		List<TeacherWalletReportAggregateRDO> allWallets =
				teacherTransactionRepository.reportAggregatesByDateAndSource(null, null, null);
		assertThat(allWallets.stream().mapToLong(TeacherWalletReportAggregateRDO::transactionCount).sum()).isEqualTo(4);
	}

	private TeacherWalletReportAggregateRDO aggregateFor(
			List<TeacherWalletReportAggregateRDO> aggregates,
			LocalDate transactionDate,
			String sourceType
	) {
		return aggregates.stream()
				.filter(row -> row.transactionDate().equals(transactionDate) && row.sourceType().equals(sourceType))
				.findFirst()
				.orElseThrow();
	}

	private void assertInsufficientBalance(ThrowingRunnable runnable) {
		assertThatThrownBy(runnable::run)
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("WALLET_INSUFFICIENT_BALANCE");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				});
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
		Long userId = activeUser(prefix + "-teacher", "teacher");
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

	private Long publishedTest(Long userId, Long teacherProfileId, String title, boolean free) {
		return insertAndReturnId("""
				insert into tests (
				  teacher_profile_id, title, description, type, difficulty, estimated_band_min,
				  estimated_band_max, duration_minutes, price_amount, currency, is_free, language,
				  visibility, published_at, created_by_user_id
				)
				values (?, ?, 'Wallet fixture.', 'reading', 'intermediate', 5.5, 7.0, 60, ?, 'UZS', ?, 'en', 'published', ?, ?)
				""",
				teacherProfileId,
				title,
				free ? BigDecimal.ZERO : new BigDecimal("20000.00"),
				free,
				now(),
				userId);
	}

	private Long activeListing(Long teacherProfileId, Long testId, String title, String price, String currency) {
		return insertAndReturnId("""
				insert into marketplace_listings (
				  teacher_profile_id, test_id, title, description, listing_type, price_amount, currency, is_active, language
				)
				values (?, ?, ?, 'Wallet listing fixture.', 'single_test', ?, ?, true, 'en')
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

	private record TeacherFixture(Long userId, Long profileId) {
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run();
	}
}
