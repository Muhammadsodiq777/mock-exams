package com.mockexams.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.mockexams.backend.dto.AiUsageRemainingRDO;
import com.mockexams.backend.error.BusinessException;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"app.openai.enabled=true",
		"app.openai.api-key=test-real-openai-key",
		"app.ai.usage.free-requests-limit=1",
		"app.ai.usage.hard-limit-requests=2"
})
@ActiveProfiles("test")
class AiUsageServiceTest {

	@Autowired
	private AiUsageService aiUsageService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MutableClock clock;

	@BeforeEach
	void setUp() {
		clock.setInstant(Instant.parse("2026-06-15T10:00:00Z"));
		updateUsageLimitSetting("ai.free_monthly_requests", 1);
		updateUsageLimitSetting("ai.hard_monthly_requests", 2);
	}

	@AfterEach
	void tearDown() {
		updateUsageLimitSetting("ai.free_monthly_requests", 20);
		updateUsageLimitSetting("ai.hard_monthly_requests", 50);
	}

	@Test
	void userWithinFreeLimitCanReserveAndRecordSuccessfulAiUsage() {
		Long userId = createUser("ai-free");

		AiUsageReservation reservation = aiUsageService.guardAndReserve(userId, AiUsageRequest.studyPlan());
		aiUsageService.recordSuccess(reservation, new AiUsageAccounting(
				"openai",
				"gpt-4.1-mini",
				120,
				80,
				new BigDecimal("0.004200")));

		AiUsageRemainingRDO remaining = aiUsageService.remainingUsage(userId);
		assertThat(remaining.usedRequests()).isEqualTo(1);
		assertThat(remaining.freeRequestsRemaining()).isZero();
		assertThat(remaining.hardRequestsRemaining()).isEqualTo(1);

		MapRow feedback = jdbcTemplate.queryForObject("""
				select status, provider, model, prompt_tokens, completion_tokens, estimated_cost, failure_code
				from ai_feedbacks
				where id = ?
				""", (rs, rowNum) -> new MapRow(
				rs.getString("status"),
				rs.getString("provider"),
				rs.getString("model"),
				rs.getInt("prompt_tokens"),
				rs.getInt("completion_tokens"),
				rs.getBigDecimal("estimated_cost"),
				rs.getString("failure_code")), reservation.feedbackId());
		assertThat(feedback.status()).isEqualTo("completed");
		assertThat(feedback.provider()).isEqualTo("openai");
		assertThat(feedback.model()).isEqualTo("gpt-4.1-mini");
		assertThat(feedback.promptTokens()).isEqualTo(120);
		assertThat(feedback.completionTokens()).isEqualTo(80);
		assertThat(feedback.estimatedCost()).isEqualByComparingTo("0.004200");
		assertThat(feedback.failureCode()).isNull();
	}

	@Test
	void newUsageRowUsesPlatformSettingsInsteadOfEnvironmentLimits() {
		Long userId = createUser("ai-platform-settings");
		updateUsageLimitSetting("ai.free_monthly_requests", 3);
		updateUsageLimitSetting("ai.hard_monthly_requests", 4);

		aiUsageService.guardAndReserve(userId, AiUsageRequest.studyPlan());

		AiUsageRemainingRDO remaining = aiUsageService.remainingUsage(userId);
		assertThat(remaining.freeRequestsLimit()).isEqualTo(3);
		assertThat(remaining.hardLimitRequests()).isEqualTo(4);
		assertThat(jdbcTemplate.queryForObject("""
				select free_requests_limit
				from ai_usage_limits
				where user_id = ?
				""", Integer.class, userId)).isEqualTo(3);
		assertThat(jdbcTemplate.queryForObject("""
				select hard_limit_requests
				from ai_usage_limits
				where user_id = ?
				""", Integer.class, userId)).isEqualTo(4);
	}

	@Test
	void userOverHardLimitIsBlockedAndAuditedWithoutIncrementingUsage() {
		Long userId = createUser("ai-hard");
		recordSuccessfulStudyPlan(userId);
		recordSuccessfulStudyPlan(userId);

		assertThatThrownBy(() -> aiUsageService.guardAndReserve(userId, AiUsageRequest.studyPlan()))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("AI_USAGE_LIMIT_EXCEEDED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
				});

		AiUsageRemainingRDO remaining = aiUsageService.remainingUsage(userId);
		assertThat(remaining.usedRequests()).isEqualTo(2);
		assertThat(remaining.hardLimitReached()).isTrue();
		Integer blockedRows = jdbcTemplate.queryForObject("""
				select count(*)
				from ai_feedbacks
				where student_user_id = ? and status = 'failed' and failure_code = 'AI_USAGE_LIMIT_EXCEEDED'
				""", Integer.class, userId);
		assertThat(blockedRows).isEqualTo(1);
	}

	@Test
	void providerFailureIsRecordedAndDoesNotConsumeLimitByDefault() {
		Long userId = createUser("ai-failure");
		AiUsageReservation reservation = aiUsageService.guardAndReserve(userId, AiUsageRequest.studyPlan());

		aiUsageService.recordProviderFailure(reservation, "AI_PROVIDER_FAILURE", "Provider timed out.");

		AiUsageRemainingRDO remaining = aiUsageService.remainingUsage(userId);
		assertThat(remaining.usedRequests()).isZero();
		String status = jdbcTemplate.queryForObject(
				"select status from ai_feedbacks where id = ?", String.class, reservation.feedbackId());
		String failureCode = jdbcTemplate.queryForObject(
				"select failure_code from ai_feedbacks where id = ?", String.class, reservation.feedbackId());
		assertThat(status).isEqualTo("failed");
		assertThat(failureCode).isEqualTo("AI_PROVIDER_FAILURE");
	}

	@Test
	void accountingCannotBeAppliedTwiceToTheSameReservation() {
		Long userId = createUser("ai-double-accounting");
		AiUsageReservation reservation = aiUsageService.guardAndReserve(userId, AiUsageRequest.studyPlan());
		aiUsageService.recordSuccess(reservation, AiUsageAccounting.zeroCost("openai", "gpt-4.1-mini"));

		assertThatThrownBy(() -> aiUsageService.recordProviderFailure(
				reservation,
				"AI_PROVIDER_FAILURE",
				"Provider timed out."))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("AI_FEEDBACK_STATUS_CONFLICT");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				});

		assertThat(aiUsageService.remainingUsage(userId).usedRequests()).isEqualTo(1);
	}

	@Test
	void unsupportedAdminReportStatusFilterIsRejected() {
		assertThatThrownBy(() -> aiUsageService.summarizeReport(new AiUsageReportCriteria(
				null,
				null,
				"not_a_status",
				null,
				null,
				null)))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("VALIDATION_FAILED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
				});
	}

	@Test
	void newPeriodCreatesNewUsageRowWithoutMutatingPriorPeriod() {
		Long userId = createUser("ai-period");
		recordSuccessfulStudyPlan(userId);
		LocalDate firstPeriodStart = currentPeriodStart(userId);

		clock.setInstant(Instant.parse("2026-07-01T10:00:00Z"));
		recordSuccessfulStudyPlan(userId);

		List<Date> periodStarts = jdbcTemplate.queryForList("""
				select period_start
				from ai_usage_limits
				where user_id = ?
				order by period_start
				""", Date.class, userId);
		assertThat(periodStarts).extracting(Date::toLocalDate)
				.containsExactly(firstPeriodStart, LocalDate.of(2026, 7, 1));
		List<Integer> usedRequests = jdbcTemplate.queryForList("""
				select used_requests
				from ai_usage_limits
				where user_id = ?
				order by period_start
				""", Integer.class, userId);
		assertThat(usedRequests).containsExactly(1, 1);
	}

	@Test
	void concurrentReservationsCannotExceedHardLimit() throws Exception {
		Long userId = createUser("ai-concurrent");
		int workers = 8;
		ExecutorService executorService = Executors.newFixedThreadPool(workers);
		CountDownLatch ready = new CountDownLatch(workers);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successes = new AtomicInteger();
		AtomicInteger blocked = new AtomicInteger();
		List<Throwable> unexpected = new ArrayList<>();

		for (int i = 0; i < workers; i++) {
			executorService.submit(() -> {
				ready.countDown();
				try {
					start.await(5, TimeUnit.SECONDS);
					AiUsageReservation reservation = aiUsageService.guardAndReserve(userId, AiUsageRequest.studyPlan());
					successes.incrementAndGet();
					aiUsageService.recordSuccess(reservation, AiUsageAccounting.zeroCost("openai", "gpt-4.1-mini"));
				}
				catch (BusinessException ex) {
					if ("AI_USAGE_LIMIT_EXCEEDED".equals(ex.getCode())) {
						blocked.incrementAndGet();
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
		assertThat(executorService.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

		assertThat(unexpected).isEmpty();
		assertThat(successes.get()).isEqualTo(2);
		assertThat(blocked.get()).isEqualTo(workers - 2);
		assertThat(aiUsageService.remainingUsage(userId).usedRequests()).isEqualTo(2);
	}

	private void recordSuccessfulStudyPlan(Long userId) {
		AiUsageReservation reservation = aiUsageService.guardAndReserve(userId, AiUsageRequest.studyPlan());
		aiUsageService.recordSuccess(reservation, AiUsageAccounting.zeroCost("openai", "gpt-4.1-mini"));
	}

	private Long createUser(String prefix) {
		String email = prefix + "-" + UUID.randomUUID() + "@example.test";
		jdbcTemplate.update("""
				insert into users (email, password_hash, full_name)
				values (?, 'hash', 'AI User')
				""", email);
		return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
	}

	private void updateUsageLimitSetting(String key, int limit) {
		String value = "{\"value\": " + limit + ", \"period\": \"monthly\"}";
		int updated = jdbcTemplate.update("""
				update platform_settings
				set setting_value = ?
				where setting_key = ?
				""", value, key);
		if (updated == 0) {
			jdbcTemplate.update("""
					insert into platform_settings (setting_key, setting_value)
					values (?, ?)
					""", key, value);
		}
	}

	private LocalDate currentPeriodStart(Long userId) {
		return jdbcTemplate.queryForObject("""
				select period_start
				from ai_usage_limits
				where user_id = ?
				""", Date.class, userId).toLocalDate();
	}

	private record MapRow(
			String status,
			String provider,
			String model,
			int promptTokens,
			int completionTokens,
			BigDecimal estimatedCost,
			String failureCode
	) {
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ClockConfig {

		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock(Instant.parse("2026-06-15T10:00:00Z"), ZoneId.of("UTC"));
		}
	}

	static class MutableClock extends Clock {

		private Instant instant;
		private final ZoneId zone;

		MutableClock(Instant instant, ZoneId zone) {
			this.instant = instant;
			this.zone = zone;
		}

		void setInstant(Instant instant) {
			this.instant = instant;
		}

		@Override
		public ZoneId getZone() {
			return zone;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return new MutableClock(instant, zone);
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
