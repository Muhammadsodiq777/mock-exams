package com.mockexams.backend.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.StudentAnswerRDO;
import com.mockexams.backend.dto.StudentAnswerUDO;
import com.mockexams.backend.dto.StudentTestAttemptRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.FileMetadata;
import com.mockexams.backend.entity.FilePurpose;
import com.mockexams.backend.entity.Question;
import com.mockexams.backend.entity.QuestionOption;
import com.mockexams.backend.entity.StudentAnswer;
import com.mockexams.backend.entity.TestAttempt;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.StudentTestAttemptMapper;
import com.mockexams.backend.repository.FileMetadataRepository;
import com.mockexams.backend.repository.QuestionOptionRepository;
import com.mockexams.backend.repository.QuestionRepository;
import com.mockexams.backend.repository.StudentAnswerRepository;
import com.mockexams.backend.repository.TestAttemptRepository;
import com.mockexams.backend.repository.TestCatalogRepository;
import com.mockexams.backend.service.StudentTestAccessService;
import com.mockexams.backend.service.StudentTestAttemptService;
import com.mockexams.backend.users.entity.User;

@Service
class StudentTestAttemptServiceImpl implements StudentTestAttemptService {

	private final TestCatalogRepository testCatalogRepository;
	private final TestAttemptRepository testAttemptRepository;
	private final StudentAnswerRepository studentAnswerRepository;
	private final QuestionRepository questionRepository;
	private final QuestionOptionRepository questionOptionRepository;
	private final FileMetadataRepository fileMetadataRepository;
	private final StudentTestAccessService studentTestAccessService;
	private final StudentTestAttemptMapper studentTestAttemptMapper;

	StudentTestAttemptServiceImpl(
			TestCatalogRepository testCatalogRepository,
			TestAttemptRepository testAttemptRepository,
			StudentAnswerRepository studentAnswerRepository,
			QuestionRepository questionRepository,
			QuestionOptionRepository questionOptionRepository,
			FileMetadataRepository fileMetadataRepository,
			StudentTestAccessService studentTestAccessService,
			StudentTestAttemptMapper studentTestAttemptMapper
	) {
		this.testCatalogRepository = testCatalogRepository;
		this.testAttemptRepository = testAttemptRepository;
		this.studentAnswerRepository = studentAnswerRepository;
		this.questionRepository = questionRepository;
		this.questionOptionRepository = questionOptionRepository;
		this.fileMetadataRepository = fileMetadataRepository;
		this.studentTestAccessService = studentTestAccessService;
		this.studentTestAttemptMapper = studentTestAttemptMapper;
	}

	@Override
	@Transactional
	public StudentTestAttemptRDO startAttempt(User student, Long testId) {
		CatalogTest test = testCatalogRepository.findVisibleDetailById(testId)
				.orElseThrow(this::testNotFound);
		studentTestAccessService.requireStartAccess(student, test);
		OffsetDateTime now = now();
		TestAttempt attempt = testAttemptRepository.save(TestAttempt.start(test, student, now));
		return studentTestAttemptMapper.toRDO(attempt, now);
	}

	@Override
	@Transactional(noRollbackFor = BusinessException.class)
	public StudentAnswerRDO saveAnswer(User student, Long attemptId, StudentAnswerUDO request) {
		OffsetDateTime now = now();
		TestAttempt attempt = requireOwnedAttemptForUpdate(student, attemptId);
		requireAttemptCanChangeAnswers(attempt, now);
		AnswerPayload payload = normalizeAnswerPayload(request);
		Question question = questionRepository.findByIdAndTestId(request.questionId(), attempt.getTest().getId())
				.orElseThrow(this::questionNotFound);
		QuestionOption selectedOption = null;
		if (payload.selectedOptionId() != null) {
			selectedOption = questionOptionRepository.findByIdAndQuestionId(payload.selectedOptionId(), question.getId())
					.orElseThrow(this::questionOptionNotFound);
		}
		if (payload.audioFileId() != null) {
			requireStudentSpeakingAudio(student, payload.audioFileId());
		}
		StudentAnswer answer = studentAnswerRepository
				.findByAttemptIdAndQuestionId(attempt.getId(), question.getId())
				.orElse(null);
		if (answer == null) {
			answer = StudentAnswer.create(
					attempt,
					question,
					payload.answerText(),
					selectedOption,
					payload.audioFileId(),
					now);
		}
		else {
			answer.update(payload.answerText(), selectedOption, payload.audioFileId(), now);
		}
		return studentTestAttemptMapper.toRDO(studentAnswerRepository.save(answer));
	}

	@Override
	@Transactional(noRollbackFor = BusinessException.class)
	public StudentTestAttemptRDO submitAttempt(User student, Long attemptId) {
		OffsetDateTime now = now();
		TestAttempt attempt = requireOwnedAttemptForUpdate(student, attemptId);
		if (attempt.isSubmitted()) {
			throw new BusinessException(
					"ATTEMPT_ALREADY_SUBMITTED",
					"Attempt has already been submitted.",
					HttpStatus.CONFLICT);
		}
		if (attempt.isExpired()) {
			throw attemptExpired();
		}
		if (!attempt.isInProgress()) {
			throw new BusinessException("ATTEMPT_STATUS_CONFLICT", "Attempt cannot be submitted.", HttpStatus.CONFLICT);
		}
		if (attempt.isPastExpiry(now)) {
			attempt.expire(now);
			persistExpiredAttempt(attempt);
			throw attemptExpired();
		}
		attempt.submit(now);
		return studentTestAttemptMapper.toRDO(testAttemptRepository.save(attempt), now);
	}

	private void requireAttemptCanChangeAnswers(TestAttempt attempt, OffsetDateTime now) {
		if (attempt.isSubmitted()) {
			throw new BusinessException(
					"ATTEMPT_ALREADY_SUBMITTED",
					"Answers cannot be changed after submission.",
					HttpStatus.CONFLICT);
		}
		if (attempt.isExpired()) {
			throw attemptExpired();
		}
		if (!attempt.isInProgress()) {
			throw new BusinessException("ATTEMPT_STATUS_CONFLICT", "Answers cannot be changed.", HttpStatus.CONFLICT);
		}
		if (attempt.isPastExpiry(now)) {
			attempt.expire(now);
			testAttemptRepository.save(attempt);
			throw attemptExpired();
		}
	}

	private void persistExpiredAttempt(TestAttempt attempt) {
		testAttemptRepository.save(attempt);
	}

	private TestAttempt requireOwnedAttemptForUpdate(User student, Long attemptId) {
		return testAttemptRepository.findByIdAndStudentIdForUpdate(attemptId, student.getId())
				.orElseThrow(this::attemptNotFound);
	}

	private AnswerPayload normalizeAnswerPayload(StudentAnswerUDO request) {
		String answerText = trimToNull(request.answerText());
		Long selectedOptionId = request.selectedOptionId();
		Long audioFileId = request.audioFileId();
		if (answerText == null && selectedOptionId == null && audioFileId == null) {
			throw validationFailed("Answer text, selected option, or audio file is required.");
		}
		return new AnswerPayload(answerText, selectedOptionId, audioFileId);
	}

	private void requireStudentSpeakingAudio(User student, Long audioFileId) {
		FileMetadata fileMetadata = fileMetadataRepository.findById(audioFileId)
				.orElseThrow(this::fileNotFound);
		if (!isOwner(student, fileMetadata)) {
			throw fileNotFound();
		}
		if (!FilePurpose.SPEAKING_AUDIO.equals(fileMetadata.getPurpose())) {
			throw validationFailed("Audio file must be uploaded for speaking audio.");
		}
	}

	private boolean isOwner(User student, FileMetadata fileMetadata) {
		return student != null
				&& student.getId() != null
				&& fileMetadata.getOwner() != null
				&& student.getId().equals(fileMetadata.getOwner().getId());
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private BusinessException testNotFound() {
		return new BusinessException("TEST_NOT_FOUND", "Test was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException attemptNotFound() {
		return new BusinessException("ATTEMPT_NOT_FOUND", "Attempt was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException questionNotFound() {
		return new BusinessException("QUESTION_NOT_FOUND", "Question was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException questionOptionNotFound() {
		return new BusinessException("QUESTION_OPTION_NOT_FOUND", "Question option was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException fileNotFound() {
		return new BusinessException("NOT_FOUND", "File was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private BusinessException attemptExpired() {
		return new BusinessException("ATTEMPT_EXPIRED", "Attempt duration has been exceeded.", HttpStatus.CONFLICT);
	}

	private record AnswerPayload(String answerText, Long selectedOptionId, Long audioFileId) {
	}
}
