package com.mockexams.backend.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.ResultAnswerReviewRDO;
import com.mockexams.backend.dto.ResultDetailRDO;
import com.mockexams.backend.dto.ResultSectionRDO;
import com.mockexams.backend.dto.ResultWeakAreaRDO;
import com.mockexams.backend.dto.RichTextContentRDO;
import com.mockexams.backend.entity.Question;
import com.mockexams.backend.entity.QuestionAnswer;
import com.mockexams.backend.entity.StudentAnswer;
import com.mockexams.backend.entity.TestResult;
import com.mockexams.backend.entity.TestSection;

@Component
public class ResultMapper {

	private final ObjectMapper objectMapper;

	ResultMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public ResultDetailRDO toDetail(
			TestResult result,
			List<TestSection> sections,
			List<Question> questions,
			Map<Long, StudentAnswer> studentAnswersByQuestionId,
			Map<Long, List<QuestionAnswer>> answerKeysByQuestionId
	) {
		List<ResultAnswerReviewRDO> answers = questions.stream()
				.filter(question -> answerKeysByQuestionId.containsKey(question.getId()))
				.sorted(Comparator
						.comparing((Question question) -> question.getSection().getSectionOrder())
						.thenComparing(Question::getQuestionOrder)
						.thenComparing(Question::getId))
				.map(question -> toAnswerReview(
						question,
						studentAnswersByQuestionId.get(question.getId()),
						answerKeysByQuestionId.getOrDefault(question.getId(), List.of())))
				.toList();
		List<ResultSectionRDO> sectionResults = sections.stream()
				.map(section -> toSection(section, answers))
				.toList();
		List<ResultWeakAreaRDO> weakAreas = weakAreas(answers);
		return new ResultDetailRDO(
				result.getId(),
				result.getAttempt().getId(),
				result.getAttempt().getTest().getId(),
				result.getAttempt().getTest().getTitle(),
				result.getAttempt().getTest().getType(),
				result.getRawScore(),
				result.getMaxScore(),
				result.getEstimatedBand(),
				result.getCorrectCount(),
				result.getIncorrectCount(),
				result.getAttempt().getStartedAt(),
				result.getAttempt().getSubmittedAt(),
				result.getAttempt().getTimeSpentSeconds(),
				sectionResults,
				answers,
				weakAreas);
	}

	private ResultAnswerReviewRDO toAnswerReview(
			Question question,
			StudentAnswer studentAnswer,
			List<QuestionAnswer> answerKeys
	) {
		QuestionAnswer primaryAnswer = answerKeys.stream()
				.filter(answer -> Boolean.TRUE.equals(answer.isPrimary()))
				.findFirst()
				.orElse(answerKeys.getFirst());
		boolean unanswered = studentAnswer == null;
		return new ResultAnswerReviewRDO(
				question.getId(),
				question.getSection().getId(),
				question.getSection().getTitle(),
				question.getQuestionOrder(),
				question.getQuestionType(),
				question.getQuestionText(),
				question.getQuestionRenderedHtml(),
				studentAnswer == null ? null : studentAnswer.getAnswerText(),
				studentAnswer == null || studentAnswer.getSelectedOption() == null
						? null
						: studentAnswer.getSelectedOption().getId(),
				studentAnswer == null || studentAnswer.getSelectedOption() == null
						? null
						: studentAnswer.getSelectedOption().getOptionLabel(),
				answerKeys.stream().map(QuestionAnswer::getAnswerValue).toList(),
				studentAnswer == null ? Boolean.FALSE : Boolean.TRUE.equals(studentAnswer.getCorrect()),
				studentAnswer == null ? BigDecimal.ZERO : zeroIfNull(studentAnswer.getScoreAwarded()),
				question.getScore(),
				unanswered,
				primaryAnswer.getExplanation(),
				toRichTextContent(primaryAnswer));
	}

	private ResultSectionRDO toSection(TestSection section, List<ResultAnswerReviewRDO> answers) {
		List<ResultAnswerReviewRDO> sectionAnswers = answers.stream()
				.filter(answer -> Objects.equals(answer.sectionId(), section.getId()))
				.toList();
		BigDecimal rawScore = sectionAnswers.stream()
				.map(ResultAnswerReviewRDO::scoreAwarded)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal maxScore = sectionAnswers.stream()
				.map(ResultAnswerReviewRDO::maxScore)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		int correctCount = Math.toIntExact(sectionAnswers.stream().filter(ResultAnswerReviewRDO::correct).count());
		return new ResultSectionRDO(
				section.getId(),
				section.getTitle(),
				section.getSectionOrder(),
				rawScore,
				maxScore,
				correctCount,
				sectionAnswers.size() - correctCount);
	}

	private List<ResultWeakAreaRDO> weakAreas(List<ResultAnswerReviewRDO> answers) {
		Map<WeakAreaKey, List<ResultAnswerReviewRDO>> grouped = answers.stream()
				.filter(answer -> !Boolean.TRUE.equals(answer.correct()))
				.collect(Collectors.groupingBy(
						answer -> new WeakAreaKey(
								answer.questionType(),
								answer.sectionId(),
								answer.sectionTitle()),
						LinkedHashMap::new,
						Collectors.toList()));
		List<ResultWeakAreaRDO> weakAreas = new ArrayList<>();
		for (Map.Entry<WeakAreaKey, List<ResultAnswerReviewRDO>> entry : grouped.entrySet()) {
			WeakAreaKey key = entry.getKey();
			int totalCount = Math.toIntExact(answers.stream()
					.filter(answer -> Objects.equals(answer.questionType(), key.questionType()))
					.filter(answer -> Objects.equals(answer.sectionId(), key.sectionId()))
					.count());
			weakAreas.add(new ResultWeakAreaRDO(
					key.questionType(),
					key.sectionId(),
					key.sectionTitle(),
					entry.getValue().size(),
					totalCount));
		}
		return weakAreas;
	}

	private RichTextContentRDO toRichTextContent(QuestionAnswer answer) {
		if (answer.getExplanation() == null
				&& answer.getExplanationDocument() == null
				&& answer.getExplanationRenderedHtml() == null) {
			return null;
		}
		return new RichTextContentRDO(
				readDocument(answer.getExplanationDocument()).orElse(null),
				answer.getExplanation(),
				answer.getExplanationRenderedHtml());
	}

	private Optional<JsonNode> readDocument(String document) {
		if (document == null || document.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readTree(document));
		}
		catch (Exception ignored) {
			return Optional.empty();
		}
	}

	private BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private record WeakAreaKey(String questionType, Long sectionId, String sectionTitle) {
	}
}
