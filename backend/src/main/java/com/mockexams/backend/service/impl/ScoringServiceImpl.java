package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.ResultDetailRDO;
import com.mockexams.backend.entity.BandConversion;
import com.mockexams.backend.entity.Question;
import com.mockexams.backend.entity.QuestionAnswer;
import com.mockexams.backend.entity.StudentAnswer;
import com.mockexams.backend.entity.TestAttempt;
import com.mockexams.backend.entity.TestAttemptStatuses;
import com.mockexams.backend.entity.TestResult;
import com.mockexams.backend.entity.TestSection;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.ResultMapper;
import com.mockexams.backend.repository.BandConversionRepository;
import com.mockexams.backend.repository.QuestionAnswerRepository;
import com.mockexams.backend.repository.QuestionRepository;
import com.mockexams.backend.repository.StudentAnswerRepository;
import com.mockexams.backend.repository.TestAttemptRepository;
import com.mockexams.backend.repository.TestResultRepository;
import com.mockexams.backend.repository.TestSectionRepository;
import com.mockexams.backend.service.AnswerNormalizationService;
import com.mockexams.backend.service.ScoringService;
import com.mockexams.backend.users.entity.User;

@Service
class ScoringServiceImpl implements ScoringService {

	private static final Set<String> SCORABLE_TEST_TYPES = Set.of(TestTypes.READING, TestTypes.LISTENING);
	private static final Set<String> OBJECTIVE_QUESTION_TYPES = Set.of(
			"multiple_choice",
			"true_false_not_given",
			"yes_no_not_given",
			"matching_headings",
			"matching_information",
			"sentence_completion",
			"summary_completion",
			"table_completion",
			"diagram_labeling",
			"short_answer",
			"gap_fill",
			"ordering");

	private final TestAttemptRepository testAttemptRepository;
	private final TestResultRepository testResultRepository;
	private final TestSectionRepository testSectionRepository;
	private final QuestionRepository questionRepository;
	private final QuestionAnswerRepository questionAnswerRepository;
	private final StudentAnswerRepository studentAnswerRepository;
	private final BandConversionRepository bandConversionRepository;
	private final AnswerNormalizationService answerNormalizationService;
	private final ResultMapper resultMapper;

	ScoringServiceImpl(
			TestAttemptRepository testAttemptRepository,
			TestResultRepository testResultRepository,
			TestSectionRepository testSectionRepository,
			QuestionRepository questionRepository,
			QuestionAnswerRepository questionAnswerRepository,
			StudentAnswerRepository studentAnswerRepository,
			BandConversionRepository bandConversionRepository,
			AnswerNormalizationService answerNormalizationService,
			ResultMapper resultMapper
	) {
		this.testAttemptRepository = testAttemptRepository;
		this.testResultRepository = testResultRepository;
		this.testSectionRepository = testSectionRepository;
		this.questionRepository = questionRepository;
		this.questionAnswerRepository = questionAnswerRepository;
		this.studentAnswerRepository = studentAnswerRepository;
		this.bandConversionRepository = bandConversionRepository;
		this.answerNormalizationService = answerNormalizationService;
		this.resultMapper = resultMapper;
	}

	@Override
	@Transactional
	public ResultDetailRDO getOrCreateResultForAttempt(User student, Long attemptId) {
		TestAttempt attempt = testAttemptRepository.findByIdAndStudentIdForUpdate(attemptId, student.getId())
				.orElseThrow(this::attemptNotFound);
		requireScorableAttempt(attempt);
		TestResult result = testResultRepository.findByAttemptId(attempt.getId())
				.orElseGet(() -> scoreAttempt(attempt));
		return toDetail(result);
	}

	@Override
	@Transactional(readOnly = true)
	public ResultDetailRDO getResult(User student, Long resultId) {
		TestResult result = testResultRepository.findByIdAndStudentId(resultId, student.getId())
				.orElseThrow(this::resultNotFound);
		return toDetail(result);
	}

	private TestResult scoreAttempt(TestAttempt attempt) {
		ResultReadData readData = readData(attempt.getTest().getId(), attempt.getId());
		List<Question> objectiveQuestions = objectiveQuestions(readData);
		if (objectiveQuestions.isEmpty()) {
			throw new BusinessException(
					"RESULT_NOT_READY",
					"Attempt has no objective answer keys to score.",
					HttpStatus.CONFLICT);
		}

		BigDecimal rawScore = BigDecimal.ZERO;
		BigDecimal maxScore = BigDecimal.ZERO;
		int correctCount = 0;
		int incorrectCount = 0;
		Map<String, Integer> weakQuestionTypes = new LinkedHashMap<>();
		for (Question question : objectiveQuestions) {
			StudentAnswer studentAnswer = readData.studentAnswersByQuestionId().get(question.getId());
			boolean correct = isCorrect(studentAnswer, readData.answerKeysByQuestionId().get(question.getId()));
			BigDecimal awardedScore = correct ? question.getScore() : BigDecimal.ZERO;
			if (studentAnswer != null) {
				studentAnswer.markScored(correct, awardedScore);
			}
			rawScore = rawScore.add(awardedScore);
			maxScore = maxScore.add(question.getScore());
			if (correct) {
				correctCount++;
			}
			else {
				incorrectCount++;
				weakQuestionTypes.merge(question.getQuestionType(), 1, Integer::sum);
			}
		}
		if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException(
					"RESULT_NOT_READY",
					"Attempt has no positive objective score to calculate.",
					HttpStatus.CONFLICT);
		}
		studentAnswerRepository.saveAll(readData.studentAnswersByQuestionId().values());
		BigDecimal estimatedBand = estimateBand(attempt.getTest().getType(), rawScore);
		TestResult result = TestResult.create(
				attempt,
				rawScore,
				maxScore,
				estimatedBand,
				correctCount,
				incorrectCount,
				toJsonMap(weakQuestionTypes),
				Map.of(
						"objectiveQuestionCount", objectiveQuestions.size(),
						"scoringMethod", "exact_normalized_match"));
		return testResultRepository.saveAndFlush(result);
	}

	private ResultDetailRDO toDetail(TestResult result) {
		ResultReadData readData = readData(result.getAttempt().getTest().getId(), result.getAttempt().getId());
		return resultMapper.toDetail(
				result,
				readData.sections(),
				objectiveQuestions(readData),
				readData.studentAnswersByQuestionId(),
				readData.answerKeysByQuestionId());
	}

	private List<Question> objectiveQuestions(ResultReadData readData) {
		return readData.questions().stream()
				.filter(question -> OBJECTIVE_QUESTION_TYPES.contains(question.getQuestionType()))
				.filter(question -> readData.answerKeysByQuestionId().containsKey(question.getId()))
				.toList();
	}

	private ResultReadData readData(Long testId, Long attemptId) {
		List<TestSection> sections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(testId);
		List<Long> sectionIds = sections.stream().map(TestSection::getId).toList();
		List<Question> questions = sectionIds.isEmpty()
				? List.of()
				: questionRepository.findBySectionIds(sectionIds);
		List<Long> questionIds = questions.stream().map(Question::getId).toList();
		Map<Long, List<QuestionAnswer>> answerKeysByQuestionId = questionIds.isEmpty()
				? Map.of()
				: questionAnswerRepository.findByQuestionIdInOrderByQuestionIdAscAnswerOrderAscIdAsc(questionIds).stream()
						.collect(Collectors.groupingBy(
								answer -> answer.getQuestion().getId(),
								LinkedHashMap::new,
								Collectors.toList()));
		Map<Long, StudentAnswer> studentAnswersByQuestionId = studentAnswerRepository.findByAttemptIdForResult(attemptId)
				.stream()
				.collect(Collectors.toMap(
						answer -> answer.getQuestion().getId(),
						answer -> answer,
						(first, second) -> first,
						LinkedHashMap::new));
		return new ResultReadData(sections, questions, answerKeysByQuestionId, studentAnswersByQuestionId);
	}

	private boolean isCorrect(StudentAnswer studentAnswer, List<QuestionAnswer> answerKeys) {
		String normalizedStudentAnswer = normalizeStudentAnswer(studentAnswer);
		if (normalizedStudentAnswer == null) {
			return false;
		}
		return answerKeys.stream()
				.map(this::normalizedAnswerKey)
				.anyMatch(normalizedStudentAnswer::equals);
	}

	private String normalizeStudentAnswer(StudentAnswer studentAnswer) {
		if (studentAnswer == null) {
			return null;
		}
		if (studentAnswer.getSelectedOption() != null) {
			return answerNormalizationService.normalize(studentAnswer.getSelectedOption().getOptionLabel());
		}
		return answerNormalizationService.normalize(studentAnswer.getAnswerText());
	}

	private String normalizedAnswerKey(QuestionAnswer answer) {
		if (answer.getNormalizedValue() != null && !answer.getNormalizedValue().isBlank()) {
			return answer.getNormalizedValue();
		}
		return answerNormalizationService.normalize(answer.getAnswerValue());
	}

	private BigDecimal estimateBand(String testType, BigDecimal rawScore) {
		return bandConversionRepository.findMatching(testType, rawScore).stream()
				.findFirst()
				.map(BandConversion::getEstimatedBand)
				.orElse(null);
	}

	private void requireScorableAttempt(TestAttempt attempt) {
		if (!SCORABLE_TEST_TYPES.contains(attempt.getTest().getType())) {
			throw new BusinessException(
					"RESULT_NOT_READY",
					"Only Reading and Listening objective attempts can be auto-scored.",
					HttpStatus.CONFLICT);
		}
		if (TestAttemptStatuses.IN_PROGRESS.equals(attempt.getStatus())) {
			throw new BusinessException(
					"ATTEMPT_NOT_SUBMITTED",
					"Attempt must be submitted before results are available.",
					HttpStatus.CONFLICT);
		}
		if (!TestAttemptStatuses.SUBMITTED.equals(attempt.getStatus())
				&& !TestAttemptStatuses.EXPIRED.equals(attempt.getStatus())) {
			throw new BusinessException(
					"ATTEMPT_STATUS_CONFLICT",
					"Attempt cannot be scored in its current status.",
					HttpStatus.CONFLICT);
		}
	}

	private Map<String, Object> toJsonMap(Map<String, Integer> values) {
		return new LinkedHashMap<>(values);
	}

	private BusinessException attemptNotFound() {
		return new BusinessException("ATTEMPT_NOT_FOUND", "Attempt was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException resultNotFound() {
		return new BusinessException("RESULT_NOT_FOUND", "Result was not found.", HttpStatus.NOT_FOUND);
	}

	private record ResultReadData(
			List<TestSection> sections,
			List<Question> questions,
			Map<Long, List<QuestionAnswer>> answerKeysByQuestionId,
			Map<Long, StudentAnswer> studentAnswersByQuestionId
	) {
	}
}
