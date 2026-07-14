package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentTestAttemptControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanAttemptFixtures() {
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
		jdbcTemplate.update("delete from teacher_profiles");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
		jdbcTemplate.update("delete from files");
	}

	@Test
	void studentCanStartSaveAndSubmitFreePublishedTest() throws Exception {
		String studentEmail = uniqueEmail("attempt-happy");
		String accessToken = registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		Long authorId = createAdminUser("attempt-author");
		Long testId = createTest(authorId, null, "Published Attempt Test", "published", true, 60);
		Long sectionId = createSection(testId, "Reading Passage 1");
		Long questionId = createQuestion(sectionId, "multiple_choice", "Which answer is correct?");
		Long optionId = createOption(questionId, "A", "First option");

		Long attemptId = startAttempt(accessToken, testId)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.testId").value(testId))
				.andExpect(jsonPath("$.data.studentUserId").value(studentId))
				.andExpect(jsonPath("$.data.status").value("in_progress"))
				.andExpect(jsonPath("$.data.startedAt").isNotEmpty())
				.andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
				.andExpect(jsonPath("$.data.durationMinutes").value(60))
				.andExpect(jsonPath("$.data.remainingSeconds").isNumber())
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty())
				.andReturnId();

		saveAnswer(accessToken, attemptId, questionId, optionId, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.attemptId").value(attemptId))
				.andExpect(jsonPath("$.data.questionId").value(questionId))
				.andExpect(jsonPath("$.data.selectedOptionId").value(optionId))
				.andExpect(jsonPath("$.data.answerText").doesNotExist())
				.andExpect(jsonPath("$.data.correct").doesNotExist())
				.andExpect(jsonPath("$.data.scoreAwarded").doesNotExist());

		mockMvc.perform(post("/api/v1/student/attempts/{attemptId}/submit", attemptId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(attemptId))
				.andExpect(jsonPath("$.data.status").value("submitted"))
				.andExpect(jsonPath("$.data.submittedAt").isNotEmpty());
	}

	@Test
	void cannotStartDraftRejectedArchivedUnpublishedOrPaidTest() throws Exception {
		String accessToken = registerStudent(uniqueEmail("attempt-ineligible"));
		Long authorId = createAdminUser("attempt-ineligible-author");
		Long draftId = createTest(authorId, null, "Draft", "draft", true, 60);
		Long submittedId = createTest(authorId, null, "Submitted", "submitted", true, 60);
		Long rejectedId = createTest(authorId, null, "Rejected", "rejected", true, 60);
		Long archivedId = createTest(authorId, null, "Archived", "archived", true, 60);
		Long paidId = createTest(authorId, null, "Paid", "published", false, 60);

		startAttempt(accessToken, draftId)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEST_NOT_FOUND"));
		startAttempt(accessToken, submittedId)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEST_NOT_FOUND"));
		startAttempt(accessToken, rejectedId)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEST_NOT_FOUND"));
		startAttempt(accessToken, archivedId)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEST_NOT_FOUND"));
		startAttempt(accessToken, paidId)
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.error.code").value("PAYMENT_REQUIRED"));
	}

	@Test
	void cannotSaveAnswerAfterSubmit() throws Exception {
		String accessToken = registerStudent(uniqueEmail("attempt-save-after-submit"));
		Long authorId = createAdminUser("attempt-save-after-submit-author");
		Long testId = createTest(authorId, null, "Published Attempt Test", "published", true, 60);
		Long sectionId = createSection(testId, "Reading Passage 1");
		Long questionId = createQuestion(sectionId, "short_answer", "What is the answer?");
		Long attemptId = startAttempt(accessToken, testId).andReturnId();

		mockMvc.perform(post("/api/v1/student/attempts/{attemptId}/submit", attemptId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());

		saveAnswer(accessToken, attemptId, questionId, null, "late answer")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("ATTEMPT_ALREADY_SUBMITTED"));
	}

	@Test
	void expiredAttemptCannotBeSubmittedAsNormalAndRejectsAnswerChanges() throws Exception {
		String accessToken = registerStudent(uniqueEmail("attempt-expired"));
		Long authorId = createAdminUser("attempt-expired-author");
		Long testId = createTest(authorId, null, "Published Attempt Test", "published", true, 1);
		Long sectionId = createSection(testId, "Reading Passage 1");
		Long questionId = createQuestion(sectionId, "short_answer", "What is the answer?");
		Long attemptId = startAttempt(accessToken, testId).andReturnId();
		jdbcTemplate.update("""
				update test_attempts
				set started_at = ?, expires_at = ?, updated_at = ?
				where id = ?
				""",
				OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2),
				OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1),
				OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1),
				attemptId);

		mockMvc.perform(post("/api/v1/student/attempts/{attemptId}/submit", attemptId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("ATTEMPT_EXPIRED"));

		saveAnswer(accessToken, attemptId, questionId, null, "late answer")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("ATTEMPT_EXPIRED"));

		String status = jdbcTemplate.queryForObject(
				"select status from test_attempts where id = ?",
				String.class,
				attemptId);
		assertThat(status).isEqualTo("expired");
	}

	@Test
	void cancelledAttemptPastExpiryReturnsLifecycleConflictWithoutExpiring() throws Exception {
		String accessToken = registerStudent(uniqueEmail("attempt-cancelled-expired"));
		Long authorId = createAdminUser("attempt-cancelled-expired-author");
		Long testId = createTest(authorId, null, "Published Attempt Test", "published", true, 1);
		Long sectionId = createSection(testId, "Reading Passage 1");
		Long questionId = createQuestion(sectionId, "short_answer", "What is the answer?");
		Long attemptId = startAttempt(accessToken, testId).andReturnId();
		jdbcTemplate.update("""
				update test_attempts
				set status = 'cancelled', started_at = ?, expires_at = ?, updated_at = ?
				where id = ?
				""",
				OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2),
				OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1),
				OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1),
				attemptId);

		saveAnswer(accessToken, attemptId, questionId, null, "late answer")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("ATTEMPT_STATUS_CONFLICT"));
		assertThat(attemptStatus(attemptId)).isEqualTo("cancelled");

		mockMvc.perform(post("/api/v1/student/attempts/{attemptId}/submit", attemptId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("ATTEMPT_STATUS_CONFLICT"));
		assertThat(attemptStatus(attemptId)).isEqualTo("cancelled");
	}

	@Test
	void savingSameQuestionUpdatesOneAnswerRow() throws Exception {
		String accessToken = registerStudent(uniqueEmail("attempt-upsert"));
		Long authorId = createAdminUser("attempt-upsert-author");
		Long testId = createTest(authorId, null, "Published Attempt Test", "published", true, 60);
		Long sectionId = createSection(testId, "Reading Passage 1");
		Long questionId = createQuestion(sectionId, "short_answer", "What is the answer?");
		Long attemptId = startAttempt(accessToken, testId).andReturnId();

		saveAnswer(accessToken, attemptId, questionId, null, "first answer")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.answerText").value("first answer"));
		saveAnswer(accessToken, attemptId, questionId, null, "updated answer")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.answerText").value("updated answer"));

		Integer answerCount = jdbcTemplate.queryForObject(
				"select count(*) from student_answers where attempt_id = ? and question_id = ?",
				Integer.class,
				attemptId,
				questionId);
		String answerText = jdbcTemplate.queryForObject(
				"select answer_text from student_answers where attempt_id = ? and question_id = ?",
				String.class,
				attemptId,
				questionId);
		assertThat(answerCount).isOne();
		assertThat(answerText).isEqualTo("updated answer");
	}

	@Test
	void audioAnswerCanBeSavedAsOnlyResponse() throws Exception {
		String studentEmail = uniqueEmail("attempt-audio");
		String accessToken = registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		Long authorId = createAdminUser("attempt-audio-author");
		Long testId = createTest(authorId, null, "Speaking Attempt Test", "published", true, 60);
		Long sectionId = createSection(testId, "Speaking Part 1");
		Long questionId = createQuestion(sectionId, "audio_answer", "Tell me about your hometown.");
		Long attemptId = startAttempt(accessToken, testId).andReturnId();
		Long audioFileId = createFile(studentId, "speaking_audio", "answer.webm", "audio/webm");

		saveAudioAnswer(accessToken, attemptId, questionId, audioFileId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.attemptId").value(attemptId))
				.andExpect(jsonPath("$.data.questionId").value(questionId))
				.andExpect(jsonPath("$.data.answerText").doesNotExist())
				.andExpect(jsonPath("$.data.selectedOptionId").doesNotExist())
				.andExpect(jsonPath("$.data.audioFileId").value(audioFileId));

		Long savedAudioFileId = jdbcTemplate.queryForObject(
				"select audio_file_id from student_answers where attempt_id = ? and question_id = ?",
				Long.class,
				attemptId,
				questionId);
		assertThat(savedAudioFileId).isEqualTo(audioFileId);
	}

	@Test
	void audioAnswerRejectsOtherUsersPrivateOrWrongPurposeFile() throws Exception {
		String studentEmail = uniqueEmail("attempt-audio-owner");
		String accessToken = registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		String otherStudentEmail = uniqueEmail("attempt-audio-other");
		registerStudent(otherStudentEmail);
		Long otherStudentId = userIdForEmail(otherStudentEmail);
		Long authorId = createAdminUser("attempt-audio-invalid-author");
		Long testId = createTest(authorId, null, "Speaking Attempt Test", "published", true, 60);
		Long sectionId = createSection(testId, "Speaking Part 1");
		Long questionId = createQuestion(sectionId, "audio_answer", "Describe a useful app.");
		Long attemptId = startAttempt(accessToken, testId).andReturnId();
		Long otherUsersAudioFileId = createFile(otherStudentId, "speaking_audio", "other.webm", "audio/webm");
		Long wrongPurposeFileId = createFile(studentId, "listening_audio", "listening.mp3", "audio/mpeg");

		saveAudioAnswer(accessToken, attemptId, questionId, otherUsersAudioFileId)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

		saveAudioAnswer(accessToken, attemptId, questionId, wrongPurposeFileId)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void concurrentFirstSavesForSameQuestionRemainIdempotent() throws Exception {
		String accessToken = registerStudent(uniqueEmail("attempt-concurrent-upsert"));
		Long authorId = createAdminUser("attempt-concurrent-upsert-author");
		Long testId = createTest(authorId, null, "Published Attempt Test", "published", true, 60);
		Long sectionId = createSection(testId, "Reading Passage 1");
		Long questionId = createQuestion(sectionId, "short_answer", "What is the answer?");
		Long attemptId = startAttempt(accessToken, testId).andReturnId();
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> firstSave = executor.submit(
					() -> saveAnswerWhenReleased(ready, start, accessToken, attemptId, questionId, "first answer"));
			Future<Integer> secondSave = executor.submit(
					() -> saveAnswerWhenReleased(ready, start, accessToken, attemptId, questionId, "second answer"));

			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			assertThat(List.of(firstSave.get(10, TimeUnit.SECONDS), secondSave.get(10, TimeUnit.SECONDS)))
					.containsOnly(200);
		}
		finally {
			executor.shutdownNow();
		}

		Integer answerCount = jdbcTemplate.queryForObject(
				"select count(*) from student_answers where attempt_id = ? and question_id = ?",
				Integer.class,
				attemptId,
				questionId);
		assertThat(answerCount).isOne();
	}

	@Test
	void submittedObjectiveAttemptReturnsScoredResultWithExplanationsAndIsIdempotent() throws Exception {
		String accessToken = registerStudent(uniqueEmail("result-scoring"));
		Long authorId = createAdminUser("result-scoring-author");
		Long testId = createTest(authorId, null, "Published Result Test", "published", true, 60);
		Long sectionId = createSection(testId, "Reading Passage 1");
		Long multipleChoiceId = createQuestion(sectionId, "multiple_choice", "Which option is correct?");
		Long optionAId = createOption(multipleChoiceId, "A", "Wrong option");
		Long optionBId = createOption(multipleChoiceId, "B", "Correct option");
		createAnswerKey(multipleChoiceId, "B", "b", "Option B is supported by the passage.");
		Long shortAnswerId = createQuestion(sectionId, "short_answer", "Which river is named?");
		createAnswerKey(shortAnswerId, "The River Nile", "the river nile", "The passage names the River Nile.");
		Long unansweredId = createQuestion(sectionId, "gap_fill", "The capital is ____.");
		createAnswerKey(unansweredId, "Tashkent", "tashkent", "The text gives Tashkent as the capital.");
		Long unsupportedAnswerKeyedId = createQuestion(sectionId, "audio_answer", "Record a spoken answer.");
		createAnswerKey(unsupportedAnswerKeyedId, "Hidden answer", "hidden answer", "This explanation must not be exposed.");
		Long attemptId = startAttempt(accessToken, testId).andReturnId();

		saveAnswer(accessToken, attemptId, multipleChoiceId, optionBId, null).andExpect(status().isOk());
		saveAnswer(accessToken, attemptId, shortAnswerId, null, "  The   River, Nile!  ").andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/student/attempts/{attemptId}/submit", attemptId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());

		ResultWithId firstResult = new ResultWithId(mockMvc.perform(
						get("/api/v1/student/attempts/{attemptId}/result", attemptId)
								.header("Authorization", "Bearer " + accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.attemptId").value(attemptId))
				.andExpect(jsonPath("$.data.testId").value(testId))
				.andExpect(jsonPath("$.data.rawScore").value(2.0))
				.andExpect(jsonPath("$.data.maxScore").value(3.0))
				.andExpect(jsonPath("$.data.estimatedBand").isNumber())
				.andExpect(jsonPath("$.data.correctCount").value(2))
				.andExpect(jsonPath("$.data.incorrectCount").value(1))
				.andExpect(jsonPath("$.data.answers.length()").value(3))
				.andExpect(jsonPath("$.data.answers[0].questionId").value(multipleChoiceId))
				.andExpect(jsonPath("$.data.answers[0].correct").value(true))
				.andExpect(jsonPath("$.data.answers[0].scoreAwarded").value(1.0))
				.andExpect(jsonPath("$.data.answers[0].correctAnswers[0]").value("B"))
				.andExpect(jsonPath("$.data.answers[0].explanation").value("Option B is supported by the passage."))
				.andExpect(jsonPath("$.data.answers[1].questionId").value(shortAnswerId))
				.andExpect(jsonPath("$.data.answers[1].studentAnswer").value("The   River, Nile!"))
				.andExpect(jsonPath("$.data.answers[1].correct").value(true))
				.andExpect(jsonPath("$.data.answers[2].questionId").value(unansweredId))
				.andExpect(jsonPath("$.data.answers[2].unanswered").value(true))
				.andExpect(jsonPath("$.data.answers[2].correct").value(false))
				.andExpect(jsonPath("$.data.weakAreas[0].questionType").value("gap_fill"))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty())
				.andExpect(jsonPath("$.meta.generatedAt").doesNotExist());

		Long resultId = firstResult.andReturnId();
		new ResultWithId(mockMvc.perform(get("/api/v1/student/results/{resultId}", resultId)
						.header("Authorization", "Bearer " + accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(resultId))
				.andExpect(jsonPath("$.data.attemptId").value(attemptId))
				.andExpect(jsonPath("$.data.answers.length()").value(3))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty())
				.andExpect(jsonPath("$.meta.generatedAt").doesNotExist());
		new ResultWithId(mockMvc.perform(get("/api/v1/student/attempts/{attemptId}/result", attemptId)
						.header("Authorization", "Bearer " + accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(resultId));

		Integer resultCount = jdbcTemplate.queryForObject(
				"select count(*) from test_results where attempt_id = ?",
				Integer.class,
				attemptId);
		assertThat(resultCount).isOne();
		assertThat(jdbcTemplate.queryForObject(
				"select is_correct from student_answers where attempt_id = ? and question_id = ?",
				Boolean.class,
				attemptId,
				shortAnswerId)).isTrue();
		assertThat(jdbcTemplate.queryForObject(
				"select score_awarded from student_answers where attempt_id = ? and question_id = ?",
				BigDecimal.class,
				attemptId,
				multipleChoiceId)).isEqualByComparingTo("1.00");
		assertThat(optionAId).isPositive();
		assertThat(unsupportedAnswerKeyedId).isPositive();
	}

	@Test
	void studentCannotReadAnotherStudentsResult() throws Exception {
		String accessToken = registerStudent(uniqueEmail("result-owner"));
		String otherAccessToken = registerStudent(uniqueEmail("result-other"));
		Long authorId = createAdminUser("result-owner-author");
		Long testId = createTest(authorId, null, "Published Result Owner Test", "published", true, 60);
		Long sectionId = createSection(testId, "Reading Passage 1");
		Long questionId = createQuestion(sectionId, "short_answer", "What is the answer?");
		createAnswerKey(questionId, "Answer", "answer", "The answer key explains this.");
		Long attemptId = startAttempt(accessToken, testId).andReturnId();
		saveAnswer(accessToken, attemptId, questionId, null, "answer").andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/student/attempts/{attemptId}/submit", attemptId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());
		Long resultId = new ResultWithId(mockMvc.perform(
						get("/api/v1/student/attempts/{attemptId}/result", attemptId)
								.header("Authorization", "Bearer " + accessToken)))
				.andExpect(status().isOk())
				.andReturnId();

		mockMvc.perform(get("/api/v1/student/results/{resultId}", resultId)
						.header("Authorization", "Bearer " + otherAccessToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("RESULT_NOT_FOUND"));
	}

	@Test
	void concurrentResultGenerationCreatesOneResultRow() throws Exception {
		String accessToken = registerStudent(uniqueEmail("result-concurrent"));
		Long authorId = createAdminUser("result-concurrent-author");
		Long testId = createTest(authorId, null, "Concurrent Result Test", "published", true, 60);
		Long sectionId = createSection(testId, "Reading Passage 1");
		Long questionId = createQuestion(sectionId, "short_answer", "What is the answer?");
		createAnswerKey(questionId, "Answer", "answer", "The answer is shown directly.");
		Long attemptId = startAttempt(accessToken, testId).andReturnId();
		saveAnswer(accessToken, attemptId, questionId, null, "answer").andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/student/attempts/{attemptId}/submit", attemptId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> firstRead = executor.submit(
					() -> getResultWhenReleased(ready, start, accessToken, attemptId));
			Future<Integer> secondRead = executor.submit(
					() -> getResultWhenReleased(ready, start, accessToken, attemptId));

			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			assertThat(List.of(firstRead.get(10, TimeUnit.SECONDS), secondRead.get(10, TimeUnit.SECONDS)))
					.containsOnly(200);
		}
		finally {
			executor.shutdownNow();
		}

		Integer resultCount = jdbcTemplate.queryForObject(
				"select count(*) from test_results where attempt_id = ?",
				Integer.class,
				attemptId);
		assertThat(resultCount).isOne();
	}

	private ResultWithId startAttempt(String accessToken, Long testId) throws Exception {
		return new ResultWithId(mockMvc.perform(post("/api/v1/student/tests/{testId}/attempts", testId)
				.header("Authorization", "Bearer " + accessToken)));
	}

	private ResultWithId saveAnswer(
			String accessToken,
			Long attemptId,
			Long questionId,
			Long selectedOptionId,
			String answerText
	) throws Exception {
		String body;
		if (selectedOptionId == null) {
			body = """
					{
					  "questionId": %d,
					  "answerText": "%s"
					}
					""".formatted(questionId, answerText);
		}
		else {
			body = """
					{
					  "questionId": %d,
					  "selectedOptionId": %d
					}
					""".formatted(questionId, selectedOptionId);
		}
		return new ResultWithId(mockMvc.perform(patch("/api/v1/student/attempts/{attemptId}/answers", attemptId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)));
	}

	private ResultWithId saveAudioAnswer(
			String accessToken,
			Long attemptId,
			Long questionId,
			Long audioFileId
	) throws Exception {
		return new ResultWithId(mockMvc.perform(patch("/api/v1/student/attempts/{attemptId}/answers", attemptId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "questionId": %d,
						  "audioFileId": %d
						}
						""".formatted(questionId, audioFileId))));
	}

	private int saveAnswerWhenReleased(
			CountDownLatch ready,
			CountDownLatch start,
			String accessToken,
			Long attemptId,
			Long questionId,
			String answerText
	) throws Exception {
		ready.countDown();
		assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
		return saveAnswer(accessToken, attemptId, questionId, null, answerText).andReturnStatus();
	}

	private int getResultWhenReleased(
			CountDownLatch ready,
			CountDownLatch start,
			String accessToken,
			Long attemptId
	) throws Exception {
		ready.countDown();
		assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
		return new ResultWithId(mockMvc.perform(get("/api/v1/student/attempts/{attemptId}/result", attemptId)
				.header("Authorization", "Bearer " + accessToken))).andReturnStatus();
	}

	private String registerStudent(String email) throws Exception {
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
		return accessTokenFrom(response);
	}

	private String accessTokenFrom(String response) throws Exception {
		JsonNode root = objectMapper.readTree(response);
		return root.at("/data/accessToken").asText();
	}

	private Long userIdForEmail(String email) {
		return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
	}

	private Long createAdminUser(String emailPrefix) {
		Long userId = insertAndReturnId("""
				insert into users (email, password_hash, full_name, status, preferred_language)
				values (?, 'hash', 'Catalog Fixture', 'active', 'en')
				""", uniqueEmail(emailPrefix));
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select ?, r.id
				from roles r
				where r.code = 'admin'
				""", userId);
		return userId;
	}

	private Long createTest(
			Long authorUserId,
			Long teacherProfileId,
			String title,
			String visibility,
			boolean free,
			int durationMinutes
	) {
		BigDecimal price = free ? BigDecimal.ZERO : new BigDecimal("15000.00");
		return insertAndReturnId("""
				insert into tests (
				  teacher_profile_id, title, description, type, difficulty, estimated_band_min,
				  estimated_band_max, duration_minutes, price_amount, currency, is_free, language,
				  visibility, published_at, created_by_user_id
				)
				values (?, ?, 'Practice test fixture.', 'reading', 'intermediate', 5.5, 7.0, ?, ?, 'UZS', ?, 'en', ?, ?, ?)
				""",
				teacherProfileId,
				title,
				durationMinutes,
				price,
				free,
				visibility,
				OffsetDateTime.now(ZoneOffset.UTC),
				authorUserId);
	}

	private Long createSection(Long testId, String title) {
		return insertAndReturnId("""
				insert into test_sections (
				  test_id, title, instructions, passage_text, section_order, duration_minutes
				)
				values (?, ?, 'Read and answer.', 'Passage text.', 1, 20)
				""", testId, title);
	}

	private Long createQuestion(Long sectionId, String questionType, String questionText) {
		return insertAndReturnId("""
				insert into questions (
				  section_id, question_type, question_text, instructions, score, question_order
				)
				values (?, ?, ?, 'Choose one answer.', 1, (
				  select coalesce(max(question_order), 0) + 1 from questions where section_id = ?
				))
				""", sectionId, questionType, questionText, sectionId);
	}

	private Long createOption(Long questionId, String label, String optionText) {
		return insertAndReturnId("""
				insert into question_options (question_id, option_label, option_text, option_order)
				values (?, ?, ?, (select coalesce(max(option_order), 0) + 1 from question_options where question_id = ?))
				""", questionId, label, optionText, questionId);
	}

	private Long createAnswerKey(Long questionId, String answerValue, String normalizedValue, String explanation) {
		return insertAndReturnId("""
				insert into question_answers (
				  question_id, answer_value, normalized_value, is_primary, answer_order, explanation
				)
				values (?, ?, ?, true, (select coalesce(max(answer_order), 0) + 1 from question_answers where question_id = ?), ?)
				""", questionId, answerValue, normalizedValue, questionId, explanation);
	}

	private Long createFile(Long ownerUserId, String purpose, String originalName, String contentType) {
		return insertAndReturnId("""
				insert into files (
				  owner_user_id, purpose, bucket, object_key, original_name, content_type, size_bytes
				)
				values (?, ?, 'mock-exams-test', ?, ?, ?, 1024)
				""",
				ownerUserId,
				purpose,
				purpose + "/" + ownerUserId + "/" + UUID.randomUUID(),
				originalName,
				contentType);
	}

	private String attemptStatus(Long attemptId) {
		return jdbcTemplate.queryForObject(
				"select status from test_attempts where id = ?",
				String.class,
				attemptId);
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

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private final class ResultWithId {

		private final org.springframework.test.web.servlet.ResultActions delegate;

		private ResultWithId(org.springframework.test.web.servlet.ResultActions delegate) {
			this.delegate = delegate;
		}

		private ResultWithId andExpect(
				org.springframework.test.web.servlet.ResultMatcher matcher
		) throws Exception {
			delegate.andExpect(matcher);
			return this;
		}

		private Long andReturnId() throws Exception {
			String response = delegate.andReturn().getResponse().getContentAsString();
			return objectMapper.readTree(response).at("/data/id").asLong();
		}

		private int andReturnStatus() throws Exception {
			return delegate.andReturn().getResponse().getStatus();
		}
	}
}
