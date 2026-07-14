package com.mockexams.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

import com.mockexams.backend.dto.AdminPaymentDisputeRDO;
import com.mockexams.backend.dto.PaymentDisputeCDO;
import com.mockexams.backend.dto.PaymentDisputeDecisionCDO;
import com.mockexams.backend.dto.PaymentDisputeRDO;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@ActiveProfiles("test")
class PaymentDisputeServiceTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PaymentDisputeService paymentDisputeService;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		cleanFixtures();
	}

	@AfterEach
	void tearDown() {
		cleanFixtures();
	}

	@Test
	void studentOpensDisputeForOwnPaidPaymentAndIsNotified() {
		Long studentId = activeUser("dispute-open", "student");
		Long paymentId = createPayment(studentId, "paid", "25000.00");

		PaymentDisputeRDO created = paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(paymentId, "Charged twice", "Please investigate."));

		assertThat(created.status()).isEqualTo("open");
		assertThat(created.paymentId()).isEqualTo(paymentId);
		assertThat(created.paymentStatus()).isEqualTo("paid");
		assertThat(created.reason()).isEqualTo("Charged twice");
		assertThat(disputeCount()).isEqualTo(1);
		assertThat(disputeNotificationCount(studentId, "open")).isEqualTo(1);
	}

	@Test
	void onlyPaidOrFailedPaymentsCanBeDisputed() {
		Long studentId = activeUser("dispute-status", "student");
		Long pendingPaymentId = createPayment(studentId, "pending", "25000.00");
		Long failedPaymentId = createPayment(studentId, "failed", "25000.00");
		Long cancelledPaymentId = createPayment(studentId, "cancelled", "25000.00");

		assertBusinessError(
				() -> paymentDisputeService.create(
						user(studentId), new PaymentDisputeCDO(pendingPaymentId, "Wrong amount", null)),
				"PAYMENT_DISPUTE_STATUS_NOT_ALLOWED",
				HttpStatus.CONFLICT);
		assertBusinessError(
				() -> paymentDisputeService.create(
						user(studentId), new PaymentDisputeCDO(cancelledPaymentId, "Wrong amount", null)),
				"PAYMENT_DISPUTE_STATUS_NOT_ALLOWED",
				HttpStatus.CONFLICT);

		PaymentDisputeRDO created = paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(failedPaymentId, "Charge failed but money left my account", null));
		assertThat(created.paymentStatus()).isEqualTo("failed");
		assertThat(disputeCount()).isEqualTo(1);
	}

	@Test
	void studentCannotDisputeAnotherStudentsPayment() {
		Long owner = activeUser("dispute-owner", "student");
		Long other = activeUser("dispute-other", "student");
		Long paymentId = createPayment(owner, "paid", "25000.00");

		assertBusinessError(
				() -> paymentDisputeService.create(
						user(other), new PaymentDisputeCDO(paymentId, "Not my charge", null)),
				"PAYMENT_NOT_FOUND",
				HttpStatus.NOT_FOUND);
		assertThat(disputeCount()).isZero();
	}

	@Test
	void studentCannotReadAnotherStudentsDispute() {
		Long owner = activeUser("dispute-read-owner", "student");
		Long other = activeUser("dispute-read-other", "student");
		Long paymentId = createPayment(owner, "paid", "25000.00");
		PaymentDisputeRDO created = paymentDisputeService.create(
				user(owner), new PaymentDisputeCDO(paymentId, "Charged twice", null));

		assertBusinessError(
				() -> paymentDisputeService.myDispute(user(other), created.id()),
				"PAYMENT_DISPUTE_NOT_FOUND",
				HttpStatus.NOT_FOUND);

		assertThat(paymentDisputeService.myDisputes(user(other), null, PageRequest.of(0, 20)).getTotalElements())
				.isZero();
		assertThat(paymentDisputeService.myDisputes(user(owner), null, PageRequest.of(0, 20)).getTotalElements())
				.isEqualTo(1);
	}

	@Test
	void duplicateOpenDisputeForTheSamePaymentIsRejected() {
		Long studentId = activeUser("dispute-duplicate", "student");
		Long paymentId = createPayment(studentId, "paid", "25000.00");
		paymentDisputeService.create(user(studentId), new PaymentDisputeCDO(paymentId, "Charged twice", null));

		assertBusinessError(
				() -> paymentDisputeService.create(
						user(studentId), new PaymentDisputeCDO(paymentId, "Still not resolved", null)),
				"PAYMENT_DISPUTE_ALREADY_OPEN",
				HttpStatus.CONFLICT);
		assertThat(disputeCount()).isEqualTo(1);
	}

	@Test
	void concurrentOpenDisputesForTheSamePaymentCreateOnlyOneNonTerminalDispute() throws Exception {
		Long studentId = activeUser("dispute-race", "student");
		User student = user(studentId);
		Long paymentId = createPayment(studentId, "paid", "25000.00");
		int workers = 8;
		ExecutorService executorService = Executors.newFixedThreadPool(workers);
		CountDownLatch ready = new CountDownLatch(workers);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger created = new AtomicInteger();
		AtomicInteger duplicateRejected = new AtomicInteger();
		List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

		try {
			for (int i = 0; i < workers; i++) {
				executorService.submit(() -> {
					ready.countDown();
					try {
						start.await(5, TimeUnit.SECONDS);
						paymentDisputeService.create(student, new PaymentDisputeCDO(paymentId, "Charged twice", null));
						created.incrementAndGet();
					}
					catch (BusinessException ex) {
						if ("PAYMENT_DISPUTE_ALREADY_OPEN".equals(ex.getCode())) {
							duplicateRejected.incrementAndGet();
						}
						else {
							unexpected.add(ex);
						}
					}
					catch (Throwable ex) {
						unexpected.add(ex);
					}
				});
			}
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			executorService.shutdown();
			assertThat(executorService.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
		}
		finally {
			executorService.shutdownNow();
		}

		assertThat(unexpected).isEmpty();
		assertThat(created.get()).isOne();
		assertThat(duplicateRejected.get()).isEqualTo(workers - 1);
		assertThat(disputeCount()).isOne();
		assertThat(nonTerminalDisputeCount(paymentId)).isOne();
	}

	@Test
	void newDisputeCanBeOpenedAfterThePriorOneReachesATerminalStatus() {
		Long studentId = activeUser("dispute-reopen", "student");
		Long adminId = activeUser("dispute-reopen-admin", "admin");
		Long paymentId = createPayment(studentId, "paid", "25000.00");
		PaymentDisputeRDO first = paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(paymentId, "Charged twice", null));
		paymentDisputeService.decide(
				user(adminId), first.id(), new PaymentDisputeDecisionCDO("rejected", "Charge was legitimate."));

		PaymentDisputeRDO second = paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(paymentId, "Still disagree with the charge", null));

		assertThat(second.status()).isEqualTo("open");
		assertThat(disputeCount()).isEqualTo(2);
	}

	@Test
	void adminResolutionRecordsReviewerAndTimestampAndNotifiesStudent() {
		Long studentId = activeUser("dispute-resolve", "student");
		Long adminId = activeUser("dispute-resolve-admin", "admin");
		Long paymentId = createPayment(studentId, "paid", "25000.00");
		PaymentDisputeRDO created = paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(paymentId, "Charged twice", null));

		AdminPaymentDisputeRDO decided = paymentDisputeService.decide(
				user(adminId), created.id(), new PaymentDisputeDecisionCDO("resolved", "Confirmed single charge; explained to student."));

		assertThat(decided.status()).isEqualTo("resolved");
		assertThat(decided.reviewedByUserId()).isEqualTo(adminId);
		assertThat(decided.reviewedAt()).isNotNull();
		assertThat(decided.resolutionNote()).isEqualTo("Confirmed single charge; explained to student.");
		assertThat(disputeNotificationCount(studentId, "resolved")).isEqualTo(1);
	}

	@Test
	void resolvedOrRejectedDisputesCannotBeDecidedAgain() {
		Long studentId = activeUser("dispute-terminal", "student");
		Long adminId = activeUser("dispute-terminal-admin", "admin");
		Long paymentId = createPayment(studentId, "paid", "25000.00");
		PaymentDisputeRDO created = paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(paymentId, "Charged twice", null));
		paymentDisputeService.decide(
				user(adminId), created.id(), new PaymentDisputeDecisionCDO("rejected", "Charge was legitimate."));

		assertBusinessError(
				() -> paymentDisputeService.decide(
						user(adminId), created.id(), new PaymentDisputeDecisionCDO("resolved", "Changed my mind.")),
				"PAYMENT_DISPUTE_INVALID_STATUS_TRANSITION",
				HttpStatus.CONFLICT);
	}

	@Test
	void terminalDecisionWithoutResolutionNoteIsRejected() {
		Long studentId = activeUser("dispute-note", "student");
		Long adminId = activeUser("dispute-note-admin", "admin");
		Long paymentId = createPayment(studentId, "paid", "25000.00");
		PaymentDisputeRDO created = paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(paymentId, "Charged twice", null));

		assertBusinessError(
				() -> paymentDisputeService.decide(
						user(adminId), created.id(), new PaymentDisputeDecisionCDO("refunded", "   ")),
				"VALIDATION_FAILED",
				HttpStatus.BAD_REQUEST);
		assertThat(jdbcTemplate.queryForObject(
				"select status from payment_disputes where id = ?", String.class, created.id())).isEqualTo("open");
	}

	@Test
	void underReviewDecisionDoesNotRequireAResolutionNote() {
		Long studentId = activeUser("dispute-under-review", "student");
		Long adminId = activeUser("dispute-under-review-admin", "admin");
		Long paymentId = createPayment(studentId, "paid", "25000.00");
		PaymentDisputeRDO created = paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(paymentId, "Charged twice", null));

		AdminPaymentDisputeRDO decided = paymentDisputeService.decide(
				user(adminId), created.id(), new PaymentDisputeDecisionCDO("under_review", null));

		assertThat(decided.status()).isEqualTo("under_review");
		assertThat(decided.reviewedByUserId()).isEqualTo(adminId);
		assertThat(disputeNotificationCount(studentId, "under_review")).isEqualTo(1);
	}

	@Test
	void noOpUnderReviewDecisionDoesNotSendAnotherStatusChangeNotification() {
		Long studentId = activeUser("dispute-under-review-noop", "student");
		Long adminId = activeUser("dispute-under-review-noop-admin", "admin");
		Long paymentId = createPayment(studentId, "paid", "25000.00");
		PaymentDisputeRDO created = paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(paymentId, "Charged twice", null));
		paymentDisputeService.decide(
				user(adminId), created.id(), new PaymentDisputeDecisionCDO("under_review", null));

		AdminPaymentDisputeRDO decided = paymentDisputeService.decide(
				user(adminId), created.id(), new PaymentDisputeDecisionCDO("under_review", null));

		assertThat(decided.status()).isEqualTo("under_review");
		assertThat(disputeNotificationCount(studentId, "open")).isEqualTo(1);
		assertThat(disputeNotificationCount(studentId, "under_review")).isEqualTo(1);
	}

	@Test
	void adminQueueFiltersByStatusPaymentStatusAndTeacher() {
		Long studentId = activeUser("dispute-queue-student", "student");
		Long teacherUserId = activeUser("dispute-queue-teacher", "teacher");
		Long teacherProfileId = createApprovedTeacherProfile(teacherUserId, "Dispute Queue Teacher");
		Long listingId = createActiveListing(teacherProfileId, "Dispute Queue Listing");
		Long adminId = activeUser("dispute-queue-admin", "admin");

		Long paidPaymentId = createPaymentForListing(studentId, "paid", "25000.00", listingId);
		Long failedPaymentId = createPayment(studentId, "failed", "10000.00");
		PaymentDisputeRDO paidDispute = paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(paidPaymentId, "Charged twice", null));
		paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(failedPaymentId, "Charge failed but money left", null));
		paymentDisputeService.decide(
				user(adminId), paidDispute.id(), new PaymentDisputeDecisionCDO("under_review", "Investigating."));

		var teacherFiltered = paymentDisputeService.adminQueue(
				new AdminPaymentDisputeSearchCriteria(null, null, null, null, teacherProfileId, null, null),
				PageRequest.of(0, 20));
		assertThat(teacherFiltered.getTotalElements()).isEqualTo(1);
		assertThat(teacherFiltered.getContent().get(0).paymentId()).isEqualTo(paidPaymentId);
		assertThat(teacherFiltered.getContent().get(0).teacherProfileId()).isEqualTo(teacherProfileId);

		var statusFiltered = paymentDisputeService.adminQueue(
				new AdminPaymentDisputeSearchCriteria("under_review", null, null, null, null, null, null),
				PageRequest.of(0, 20));
		assertThat(statusFiltered.getTotalElements()).isEqualTo(1);
		assertThat(statusFiltered.getContent().get(0).paymentId()).isEqualTo(paidPaymentId);

		var paymentStatusFiltered = paymentDisputeService.adminQueue(
				new AdminPaymentDisputeSearchCriteria(null, "failed", null, null, null, null, null),
				PageRequest.of(0, 20));
		assertThat(paymentStatusFiltered.getTotalElements()).isEqualTo(1);
		assertThat(paymentStatusFiltered.getContent().get(0).paymentId()).isEqualTo(failedPaymentId);

		var unpaged = paymentDisputeService.adminQueue(
				new AdminPaymentDisputeSearchCriteria(null, null, null, null, null, null, null),
				PageRequest.of(0, 1));
		assertThat(unpaged.getTotalElements()).isEqualTo(2);
		assertThat(unpaged.getTotalPages()).isEqualTo(2);
	}

	@Test
	void unsupportedDecisionStatusIsRejected() {
		Long studentId = activeUser("dispute-bad-status", "student");
		Long adminId = activeUser("dispute-bad-status-admin", "admin");
		Long paymentId = createPayment(studentId, "paid", "25000.00");
		PaymentDisputeRDO created = paymentDisputeService.create(
				user(studentId), new PaymentDisputeCDO(paymentId, "Charged twice", null));

		assertBusinessError(
				() -> paymentDisputeService.decide(
						user(adminId), created.id(), new PaymentDisputeDecisionCDO("open", null)),
				"VALIDATION_FAILED",
				HttpStatus.BAD_REQUEST);
		assertBusinessError(
				() -> paymentDisputeService.decide(
						user(adminId), created.id(), new PaymentDisputeDecisionCDO("not_a_status", null)),
				"VALIDATION_FAILED",
				HttpStatus.BAD_REQUEST);
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

	private int disputeCount() {
		return jdbcTemplate.queryForObject("select count(*) from payment_disputes", Integer.class);
	}

	private int nonTerminalDisputeCount(Long paymentId) {
		return jdbcTemplate.queryForObject("""
				select count(*) from payment_disputes
				where payment_id = ? and status in ('open', 'under_review')
				""", Integer.class, paymentId);
	}

	private int disputeNotificationCount(Long userId, String status) {
		return jdbcTemplate.queryForObject("""
				select count(*) from notifications
				where user_id = ? and type = 'payment' and cast(payload as varchar) like ?
				""", Integer.class, userId, "%\"status\":\"" + status + "\"%");
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

	private Long createPaymentForListing(Long studentUserId, String status, String amount, Long listingId) {
		return insertAndReturnId("""
				insert into payments (student_user_id, provider, status, amount, currency, target_type, target_listing_id)
				values (?, 'mock', ?, ?, 'UZS', 'marketplace_listing', ?)
				""",
				studentUserId,
				status,
				new BigDecimal(amount),
				listingId);
	}

	private Long createApprovedTeacherProfile(Long teacherUserId, String displayName) {
		return insertAndReturnId("""
				insert into teacher_profiles (
				  user_id, display_name, headline, bio, status, approved_by_user_id, approved_at
				)
				values (?, ?, 'IELTS teacher', 'Experienced teacher.', 'approved', ?, current_timestamp)
				""",
				teacherUserId,
				displayName,
				teacherUserId);
	}

	private Long createActiveListing(Long teacherProfileId, String title) {
		return insertAndReturnId("""
				insert into marketplace_listings (
				  teacher_profile_id, title, description, listing_type, price_amount, currency, is_active, language
				)
				values (?, ?, 'Dispute fixture listing.', 'single_test', 25000.00, 'UZS', true, 'en')
				""",
				teacherProfileId,
				title);
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

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run();
	}
}
