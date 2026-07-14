package com.mockexams.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

import com.mockexams.backend.dto.SpeakingFeedbackRDO;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.service.speaking.SpeakingFeedbackPrompt;
import com.mockexams.backend.service.speaking.SpeakingFeedbackProvider;
import com.mockexams.backend.service.speaking.SpeakingFeedbackProviderException;
import com.mockexams.backend.service.speaking.SpeakingFeedbackProviderResponse;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"app.ai.speaking.provider=mock",
		"app.ai.usage.free-requests-limit=10",
		"app.ai.usage.hard-limit-requests=10"
})
@ActiveProfiles("test")
class SpeakingFeedbackServiceTest {

	@Autowired
	private SpeakingSubmissionService speakingSubmissionService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ControlledSpeakingFeedbackProvider provider;

	@BeforeEach
	void resetProvider() {
		provider.reset();
	}

	@Test
	void missingTranscriptCannotBeEvaluated() {
		User student = createUser("speaking-no-transcript");
		Long submissionId = createSpeakingSubmission(student.getId(), TestTypes.SPEAKING_PART_1, null, "failed");

		assertThatThrownBy(() -> speakingSubmissionService.startFeedback(student, submissionId))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("TRANSCRIPT_REQUIRED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				});

		assertThat(provider.calls()).isZero();
		Integer feedbackRows = jdbcTemplate.queryForObject(
				"select count(*) from ai_feedbacks where speaking_submission_id = ?", Integer.class, submissionId);
		assertThat(feedbackRows).isZero();
	}

	@Test
	void mockAiResponseStoresExpectedCriteriaAndNarrativeSections() {
		User student = createUser("speaking-success");
		Long submissionId = createSpeakingSubmission(
				student.getId(),
				TestTypes.SPEAKING_PART_2,
				"I spoke about a journey and explained why it was memorable.",
				"completed");

		SpeakingFeedbackRDO feedback = speakingSubmissionService.startFeedback(student, submissionId);

		assertThat(feedback.status()).isEqualTo("completed");
		assertThat(feedback.feedbackType()).isEqualTo("speaking");
		assertThat(feedback.submissionId()).isEqualTo(submissionId);
		assertThat(feedback.overallBand()).isEqualByComparingTo("6.5");
		assertThat(feedback.criteriaScores())
				.containsKeys("fluency_and_coherence", "lexical_resource",
						"grammatical_range_and_accuracy", "pronunciation_proxy");
		assertThat(feedback.repeatedMistakes()).hasSize(1);
		assertThat(feedback.betterAnswerExample()).contains("more natural");
		assertThat(feedback.actionPlan()).isNotEmpty();

		String criteriaJson = jdbcTemplate.queryForObject(
				"select cast(criteria_scores as varchar) from ai_feedbacks where id = ?",
				String.class,
				feedback.id());
		assertThat(criteriaJson).contains("pronunciation_proxy");
	}

	@Test
	void usageLimitBlocksSpeakingFeedbackBeforeProviderCall() {
		User student = createUser("speaking-limit");
		Long submissionId = createSpeakingSubmission(
				student.getId(),
				TestTypes.SPEAKING_PART_3,
				"I think public transport helps cities because it reduces traffic.",
				"completed");
		jdbcTemplate.update("""
				insert into ai_usage_limits
				  (user_id, period_start, period_end, free_requests_limit, used_requests, hard_limit_requests)
				values (?, ?, ?, 0, 0, 0)
				""", student.getId(), Date.valueOf(LocalDate.of(2026, 7, 1)), Date.valueOf(LocalDate.of(2026, 7, 31)));

		assertThatThrownBy(() -> speakingSubmissionService.startFeedback(student, submissionId))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("AI_USAGE_LIMIT_EXCEEDED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
				});

		assertThat(provider.calls()).isZero();
		Integer blockedRows = jdbcTemplate.queryForObject("""
				select count(*)
				from ai_feedbacks
				where speaking_submission_id = ? and student_user_id = ? and failure_code = 'AI_USAGE_LIMIT_EXCEEDED'
				""", Integer.class, submissionId, student.getId());
		assertThat(blockedRows).isEqualTo(1);
	}

	@Test
	void feedbackIsLinkedToCorrectStudentAndSubmission() {
		User owner = createUser("speaking-owner");
		User other = createUser("speaking-other");
		Long submissionId = createSpeakingSubmission(
				owner.getId(),
				TestTypes.SPEAKING_PART_1,
				"I live in a quiet city and I enjoy the parks.",
				"completed");

		assertThatThrownBy(() -> speakingSubmissionService.startFeedback(other, submissionId))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
				});

		SpeakingFeedbackRDO feedback = speakingSubmissionService.startFeedback(owner, submissionId);
		Long storedStudentId = jdbcTemplate.queryForObject(
				"select student_user_id from ai_feedbacks where id = ?", Long.class, feedback.id());
		Long storedSubmissionId = jdbcTemplate.queryForObject(
				"select speaking_submission_id from ai_feedbacks where id = ?", Long.class, feedback.id());
		assertThat(storedStudentId).isEqualTo(owner.getId());
		assertThat(storedSubmissionId).isEqualTo(submissionId);
	}

	@Test
	void transcriptOnlyPronunciationLimitationIsReturned() {
		User student = createUser("speaking-limitation");
		Long submissionId = createSpeakingSubmission(
				student.getId(),
				TestTypes.SPEAKING_PART_1,
				"I usually study at home because it is peaceful.",
				"completed");

		SpeakingFeedbackRDO feedback = speakingSubmissionService.startFeedback(student, submissionId);

		assertThat(feedback.transcriptOnlyPronunciation()).isTrue();
		assertThat(feedback.pronunciationScoringNote())
				.contains("transcript-only proxy")
				.contains("acoustic pronunciation");
		assertThat(provider.lastPrompt().userPrompt()).contains("pronunciation_proxy");
	}

	@Test
	void malformedAiResponseFailsSafelyWithoutPartialScores() {
		User student = createUser("speaking-malformed");
		Long submissionId = createSpeakingSubmission(
				student.getId(),
				TestTypes.SPEAKING_PART_2,
				"I described a book and why it influenced me.",
				"completed");
		provider.malformedNext();

		SpeakingFeedbackRDO feedback = speakingSubmissionService.startFeedback(student, submissionId);

		assertThat(feedback.status()).isEqualTo("failed");
		assertThat(feedback.failureCode()).isEqualTo("VALIDATION_FAILED");
		assertThat(feedback.overallBand()).isNull();
		assertThat(feedback.criteriaScores()).isEmpty();
		String criteria = jdbcTemplate.queryForObject(
				"select cast(criteria_scores as varchar) from ai_feedbacks where id = ?",
				String.class,
				feedback.id());
		assertThat(criteria).doesNotContain("fluency_and_coherence");
	}

	@Test
	void providerFailureMarksFeedbackFailedAndReleasesUsageReservation() {
		User student = createUser("speaking-provider-failure");
		Long submissionId = createSpeakingSubmission(
				student.getId(),
				TestTypes.SPEAKING_PART_3,
				"I believe public transport improves city life by reducing traffic.",
				"completed");
		provider.failNext();

		SpeakingFeedbackRDO feedback = speakingSubmissionService.startFeedback(student, submissionId);

		assertThat(feedback.status()).isEqualTo("failed");
		assertThat(feedback.failureCode()).isEqualTo("AI_PROVIDER_FAILURE");
		assertThat(feedback.overallBand()).isNull();
		assertThat(feedback.criteriaScores()).isEmpty();
		assertThat(provider.calls()).isEqualTo(1);
		Integer usedRequests = jdbcTemplate.queryForObject(
				"select used_requests from ai_usage_limits where user_id = ?",
				Integer.class,
				student.getId());
		assertThat(usedRequests).isZero();
		String errorMessage = jdbcTemplate.queryForObject(
				"select error_message from ai_feedbacks where id = ?",
				String.class,
				feedback.id());
		assertThat(errorMessage).contains("provider failed");
	}

	private Long createSpeakingSubmission(Long studentUserId, String partType, String transcript, String status) {
		Long fileId = createAudioFile(studentUserId);
		String prompt = "Speak about this IELTS practice topic.";
		jdbcTemplate.update("""
				insert into speaking_submissions (
				  student_user_id, part_type, prompt, audio_file_id, transcript, duration_seconds,
				  transcription_status, transcription_provider, transcription_model,
				  transcription_estimated_cost, transcription_started_at, transcription_completed_at
				)
				values (?, ?, ?, ?, ?, 45, ?, 'mock', 'mock-transcriber', 0,
				  ?, ?)
				""",
				studentUserId,
				partType,
				prompt,
				fileId,
				transcript,
				status,
				Timestamp.from(Instant.parse("2026-07-01T09:59:00Z")),
				Timestamp.from(Instant.parse("2026-07-01T10:00:00Z")));
		return jdbcTemplate.queryForObject(
				"select id from speaking_submissions where audio_file_id = ?", Long.class, fileId);
	}

	private Long createAudioFile(Long ownerUserId) {
		String objectKey = "speaking-feedback-test/" + UUID.randomUUID();
		jdbcTemplate.update("""
				insert into files (
				  owner_user_id, purpose, bucket, object_key, original_name, content_type, size_bytes, duration_seconds
				)
				values (?, 'speaking_audio', 'mock-exams-test', ?, 'answer.webm', 'audio/webm', 5, 45)
				""", ownerUserId, objectKey);
		return jdbcTemplate.queryForObject("select id from files where object_key = ?", Long.class, objectKey);
	}

	private User createUser(String prefix) {
		String email = prefix + "-" + UUID.randomUUID() + "@example.test";
		jdbcTemplate.update("""
				insert into users (email, password_hash, full_name)
				values (?, 'hash', 'Speaking Student')
				""", email);
		Long id = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
		return userRepository.findById(id).orElseThrow();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class SpeakingFeedbackTestConfig {

		@Bean
		@Primary
		ControlledSpeakingFeedbackProvider controlledSpeakingFeedbackProvider() {
			return new ControlledSpeakingFeedbackProvider();
		}

		@Bean
		@Primary
		Clock speakingFeedbackClock() {
			return Clock.fixed(Instant.parse("2026-07-01T10:00:00Z"), ZoneId.of("UTC"));
		}
	}

	static class ControlledSpeakingFeedbackProvider implements SpeakingFeedbackProvider {

		private final AtomicInteger calls = new AtomicInteger();
		private Mode mode = Mode.VALID;
		private SpeakingFeedbackPrompt lastPrompt;

		@Override
		public SpeakingFeedbackProviderResponse evaluate(SpeakingFeedbackPrompt prompt) {
			calls.incrementAndGet();
			lastPrompt = prompt;
			Mode current = mode;
			mode = Mode.VALID;
			if (current == Mode.FAIL) {
				throw new SpeakingFeedbackProviderException("Provider failed.");
			}
			String content = current == Mode.MALFORMED ? "{\"overallBand\": 6.5"
					: validJson();
			return new SpeakingFeedbackProviderResponse("mock", "mock-speaking-test", content, 30, 20, BigDecimal.ZERO);
		}

		void reset() {
			calls.set(0);
			mode = Mode.VALID;
			lastPrompt = null;
		}

		void malformedNext() {
			mode = Mode.MALFORMED;
		}

		void failNext() {
			mode = Mode.FAIL;
		}

		int calls() {
			return calls.get();
		}

		SpeakingFeedbackPrompt lastPrompt() {
			return lastPrompt;
		}

		private String validJson() {
			return """
					{
					  "overallBand": 6.5,
					  "criteriaScores": {
					    "fluency_and_coherence": {"band": 6.5, "feedback": "Ideas are easy to follow.", "suggestions": ["Extend answers with clearer linking."]},
					    "lexical_resource": {"band": 6.0, "feedback": "Vocabulary is suitable but sometimes repeated.", "suggestions": ["Use more precise topic phrases."]},
					    "grammatical_range_and_accuracy": {"band": 6.0, "feedback": "Meaning is clear despite grammar slips.", "suggestions": ["Practice complex sentences accurately."]},
					    "pronunciation_proxy": {"band": 6.5, "feedback": "Transcript suggests mostly clear word choice and phrasing.", "suggestions": ["Record and review stress and intonation separately."]}
					  },
					  "repeatedMistakes": [
					    {"type": "grammar", "original": "he go", "correction": "he goes", "explanation": "Use third-person singular verb agreement."}
					  ],
					  "betterAnswerExample": "A more natural and extended version of the answer.",
					  "actionPlan": ["Practice extending answers with reasons.", "Review frequent grammar patterns."]
					}
					""";
		}

		private enum Mode {
			VALID,
			FAIL,
			MALFORMED
		}
	}
}
