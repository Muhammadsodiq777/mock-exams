package com.mockexams.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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

import com.mockexams.backend.dto.WritingFeedbackRDO;
import com.mockexams.backend.dto.WritingSubmissionCDO;
import com.mockexams.backend.dto.WritingSubmissionRDO;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.service.writing.WritingFeedbackPrompt;
import com.mockexams.backend.service.writing.WritingFeedbackProvider;
import com.mockexams.backend.service.writing.WritingFeedbackProviderException;
import com.mockexams.backend.service.writing.WritingFeedbackProviderResponse;
import com.mockexams.backend.service.writing.WritingRubricService;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"app.ai.writing.provider=mock",
		"app.ai.usage.free-requests-limit=10",
		"app.ai.usage.hard-limit-requests=10"
})
@ActiveProfiles("test")
class WritingSubmissionServiceTest {

	@Autowired
	private WritingSubmissionService writingSubmissionService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ControlledWritingFeedbackProvider provider;

	@BeforeEach
	void resetProvider() {
		provider.reset();
	}

	@Test
	void emptyAnswerIsRejectedBeforeProviderCall() {
		User student = createUser("writing-empty");

		assertThatThrownBy(() -> writingSubmissionService.submit(student,
				new WritingSubmissionCDO(TestTypes.WRITING_TASK_2, null, "Discuss both views.", "  ")))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("VALIDATION_FAILED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
				});

		assertThat(provider.calls()).isZero();
	}

	@Test
	void usageLimitBlocksBeforeProviderCall() {
		User student = createUser("writing-limit");
		jdbcTemplate.update("""
				insert into ai_usage_limits
				  (user_id, period_start, period_end, free_requests_limit, used_requests, hard_limit_requests)
				values (?, ?, ?, 0, 0, 0)
				""", student.getId(), Date.valueOf(LocalDate.of(2026, 7, 1)), Date.valueOf(LocalDate.of(2026, 7, 31)));

		assertThatThrownBy(() -> writingSubmissionService.submit(student, validCommand(TestTypes.WRITING_TASK_2)))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("AI_USAGE_LIMIT_EXCEEDED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
				});

		assertThat(provider.calls()).isZero();
		Integer failures = jdbcTemplate.queryForObject("""
				select count(*)
				from ai_feedbacks
				where student_user_id = ? and status = 'failed' and failure_code = 'AI_USAGE_LIMIT_EXCEEDED'
				""", Integer.class, student.getId());
		assertThat(failures).isEqualTo(1);
	}

	@Test
	void mockAiResponseStoresCriteriaMistakesAndActionPlan() {
		User student = createUser("writing-success");

		WritingSubmissionRDO submission = writingSubmissionService.submit(student, validCommand(TestTypes.WRITING_TASK_2));

		assertThat(submission.wordCount()).isEqualTo(12);
		assertThat(submission.feedback().status()).isEqualTo("completed");
		assertThat(submission.feedback().overallBand()).isEqualByComparingTo("6.5");
		assertThat(submission.feedback().criteriaScores())
				.containsKeys("task_response", "coherence_and_cohesion", "lexical_resource",
						"grammatical_range_and_accuracy");
		assertThat(submission.feedback().mistakes()).hasSize(1);
		assertThat(submission.feedback().actionPlan()).isNotEmpty();

		BigDecimal storedBand = jdbcTemplate.queryForObject(
				"select overall_band from ai_feedbacks where id = ?",
				BigDecimal.class,
				submission.feedback().id());
		assertThat(storedBand).isEqualByComparingTo("6.5");
	}

	@Test
	void providerFailureLeavesFeedbackFailed() {
		User student = createUser("writing-provider-failure");
		provider.failNext();

		WritingSubmissionRDO submission = writingSubmissionService.submit(student, validCommand(TestTypes.WRITING_TASK_2));

		assertThat(submission.feedback().status()).isEqualTo("failed");
		assertThat(submission.feedback().failureCode()).isEqualTo("AI_PROVIDER_FAILURE");
		String errorMessage = jdbcTemplate.queryForObject(
				"select error_message from ai_feedbacks where id = ?",
				String.class,
				submission.feedback().id());
		assertThat(errorMessage).contains("provider failed");
	}

	@Test
	void malformedAiJsonFailsWithoutPartialVisibleBand() {
		User student = createUser("writing-malformed");
		provider.malformedNext();

		WritingSubmissionRDO submission = writingSubmissionService.submit(student, validCommand(TestTypes.WRITING_TASK_2));

		assertThat(submission.feedback().status()).isEqualTo("failed");
		assertThat(submission.feedback().failureCode()).isEqualTo("VALIDATION_FAILED");
		assertThat(submission.feedback().overallBand()).isNull();
		Integer validationErrorsLength = jdbcTemplate.queryForObject(
				"select length(cast(validation_errors as varchar)) from ai_feedbacks where id = ?",
				Integer.class,
				submission.feedback().id());
		assertThat(validationErrorsLength).isPositive();
	}

	@Test
	void retryingFailedJobCreatesLinkedSuccessfulRetry() {
		User student = createUser("writing-retry");
		provider.failNext();
		WritingSubmissionRDO failed = writingSubmissionService.submit(student, validCommand(TestTypes.WRITING_TASK_2));
		provider.validNext();

		WritingFeedbackRDO retry = writingSubmissionService.retryFeedback(student, failed.id());

		assertThat(retry.status()).isEqualTo("completed");
		assertThat(retry.retryOfFeedbackId()).isEqualTo(failed.feedback().id());
		assertThat(retry.retryCount()).isEqualTo(1);
		Integer rows = jdbcTemplate.queryForObject(
				"select count(*) from ai_feedbacks where writing_submission_id = ?",
				Integer.class,
				failed.id());
		assertThat(rows).isEqualTo(2);
	}

	@Test
	void taskRubricSelectionUsesTaskOneAndTaskTwoCriteria() {
		User student = createUser("writing-rubric");

		WritingSubmissionRDO taskOne = writingSubmissionService.submit(student, validCommand(TestTypes.WRITING_TASK_1));
		assertThat(taskOne.feedback().rubricVersion()).isEqualTo(WritingRubricService.TASK_1_RUBRIC_VERSION);
		assertThat(taskOne.feedback().criteriaScores()).containsKey("task_achievement");
		assertThat(provider.lastPrompt().userPrompt()).contains(
				"Required criteria: [task_achievement, coherence_and_cohesion, lexical_resource, grammatical_range_and_accuracy]");

		WritingSubmissionRDO taskTwo = writingSubmissionService.submit(student, validCommand(TestTypes.WRITING_TASK_2));
		assertThat(taskTwo.feedback().rubricVersion()).isEqualTo(WritingRubricService.TASK_2_RUBRIC_VERSION);
		assertThat(taskTwo.feedback().criteriaScores()).containsKey("task_response");
		assertThat(provider.lastPrompt().userPrompt()).contains(
				"Required criteria: [task_response, coherence_and_cohesion, lexical_resource, grammatical_range_and_accuracy]");
	}

	@Test
	void unsupportedTaskTypeIsRejectedBeforeProviderCall() {
		User student = createUser("writing-unsupported");

		assertThatThrownBy(() -> writingSubmissionService.submit(student, validCommand(TestTypes.READING)))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("UNSUPPORTED_WRITING_TASK_TYPE");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
				});

		assertThat(provider.calls()).isZero();
	}

	private WritingSubmissionCDO validCommand(String taskType) {
		return new WritingSubmissionCDO(
				taskType,
				null,
				"Describe the chart and summarize the main trends.",
				"This answer has enough words to trigger realistic feedback from the provider.");
	}

	private User createUser(String prefix) {
		String email = prefix + "-" + UUID.randomUUID() + "@example.test";
		jdbcTemplate.update("""
				insert into users (email, password_hash, full_name)
				values (?, 'hash', 'Writing Student')
				""", email);
		Long id = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
		return userRepository.findById(id).orElseThrow();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class WritingFeedbackTestConfig {

		@Bean
		@Primary
		ControlledWritingFeedbackProvider controlledWritingFeedbackProvider() {
			return new ControlledWritingFeedbackProvider();
		}

		@Bean
		@Primary
		Clock writingFeedbackClock() {
			return Clock.fixed(Instant.parse("2026-07-01T10:00:00Z"), ZoneId.of("UTC"));
		}
	}

	static class ControlledWritingFeedbackProvider implements WritingFeedbackProvider {

		private final AtomicInteger calls = new AtomicInteger();
		private Mode mode = Mode.VALID;
		private WritingFeedbackPrompt lastPrompt;

		@Override
		public WritingFeedbackProviderResponse evaluate(WritingFeedbackPrompt prompt) {
			calls.incrementAndGet();
			lastPrompt = prompt;
			Mode current = mode;
			mode = Mode.VALID;
			if (current == Mode.FAIL) {
				throw new WritingFeedbackProviderException("Provider failed.");
			}
			String content = current == Mode.MALFORMED ? "{\"overallBand\": 6.5"
					: validJson(prompt.submission().getTaskType());
			return new WritingFeedbackProviderResponse("mock", "mock-writing-test", content, 30, 20, BigDecimal.ZERO);
		}

		void reset() {
			calls.set(0);
			mode = Mode.VALID;
			lastPrompt = null;
		}

		void validNext() {
			mode = Mode.VALID;
		}

		void failNext() {
			mode = Mode.FAIL;
		}

		void malformedNext() {
			mode = Mode.MALFORMED;
		}

		int calls() {
			return calls.get();
		}

		WritingFeedbackPrompt lastPrompt() {
			return lastPrompt;
		}

		private String validJson(String taskType) {
			String firstCriterion = TestTypes.WRITING_TASK_1.equals(taskType) ? "task_achievement" : "task_response";
			return """
					{
					  "overallBand": 6.5,
					  "criteriaScores": {
					    "%s": {"band": 6.5, "feedback": "Focused task feedback.", "suggestions": ["Develop the main idea."]},
					    "coherence_and_cohesion": {"band": 6.0, "feedback": "Logical organization.", "suggestions": ["Improve linking."]},
					    "lexical_resource": {"band": 6.5, "feedback": "Adequate vocabulary.", "suggestions": ["Use more precise words."]},
					    "grammatical_range_and_accuracy": {"band": 6.0, "feedback": "Mostly clear grammar.", "suggestions": ["Check verb agreement."]}
					  },
					  "mistakes": [
					    {"type": "grammar", "original": "people is", "correction": "people are", "explanation": "People takes a plural verb."}
					  ],
					  "improvedAnswer": "A clearer and more accurate version of the answer.",
					  "actionPlan": ["Plan before writing.", "Proofread grammar."]
					}
					""".formatted(firstCriterion);
		}

		private enum Mode {
			VALID,
			FAIL,
			MALFORMED
		}
	}
}
