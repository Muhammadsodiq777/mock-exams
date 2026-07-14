package com.mockexams.backend.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.RichTextContentRDO;
import com.mockexams.backend.dto.TeacherListeningSegmentRDO;
import com.mockexams.backend.dto.TeacherQuestionAnswerRDO;
import com.mockexams.backend.dto.TeacherQuestionOptionRDO;
import com.mockexams.backend.dto.TeacherQuestionRDO;
import com.mockexams.backend.dto.TeacherReadingParagraphRDO;
import com.mockexams.backend.dto.TeacherTestRDO;
import com.mockexams.backend.dto.TeacherTestSectionRDO;
import com.mockexams.backend.dto.TeacherTestSummaryRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.ListeningSegment;
import com.mockexams.backend.entity.Question;
import com.mockexams.backend.entity.QuestionAnswer;
import com.mockexams.backend.entity.QuestionOption;
import com.mockexams.backend.entity.ReadingParagraph;
import com.mockexams.backend.entity.TestSection;
import com.mockexams.backend.service.RichTextService;
import com.mockexams.backend.service.RichTextService.SanitizedRichText;

@Component
public class TeacherTestBuilderMapper {

	private final RichTextService richTextService;
	private final ObjectMapper objectMapper;

	public TeacherTestBuilderMapper(RichTextService richTextService, ObjectMapper objectMapper) {
		this.richTextService = richTextService;
		this.objectMapper = objectMapper;
	}

	public TeacherTestRDO toRDO(
			CatalogTest test,
			List<TestSection> sections,
			Map<Long, List<ReadingParagraph>> paragraphsBySectionId,
			Map<Long, List<ListeningSegment>> segmentsBySectionId,
			Map<Long, List<Question>> questionsBySectionId,
			Map<Long, List<QuestionOption>> optionsByQuestionId,
			Map<Long, List<QuestionAnswer>> answersByQuestionId
	) {
		Long teacherProfileId = test.getTeacherProfile() == null ? null : test.getTeacherProfile().getId();
		return new TeacherTestRDO(
				test.getId(),
				teacherProfileId,
				test.getCreatedByUserId(),
				test.getTitle(),
				test.getDescription(),
				test.getType(),
				test.getDifficulty(),
				test.getEstimatedBandMin(),
				test.getEstimatedBandMax(),
				test.getDurationMinutes(),
				test.getPriceAmount(),
				test.getCurrency(),
				test.isFree(),
				test.getLanguage(),
				test.getVisibility(),
				test.getModerationNote(),
				test.getPublishedAt(),
				test.getCreatedAt(),
				test.getUpdatedAt(),
				toSectionRDOs(
						sections,
						paragraphsBySectionId,
						segmentsBySectionId,
						questionsBySectionId,
						optionsByQuestionId,
						answersByQuestionId));
	}

	public TeacherTestSummaryRDO toSummaryRDO(CatalogTest test) {
		Long teacherProfileId = test.getTeacherProfile() == null ? null : test.getTeacherProfile().getId();
		return new TeacherTestSummaryRDO(
				test.getId(),
				teacherProfileId,
				test.getCreatedByUserId(),
				test.getTitle(),
				test.getDescription(),
				test.getType(),
				test.getDifficulty(),
				test.getEstimatedBandMin(),
				test.getEstimatedBandMax(),
				test.getDurationMinutes(),
				test.getPriceAmount(),
				test.getCurrency(),
				test.isFree(),
				test.getLanguage(),
				test.getVisibility(),
				test.getModerationNote(),
				test.getPublishedAt(),
				test.getCreatedAt(),
				test.getUpdatedAt());
	}

	private List<TeacherTestSectionRDO> toSectionRDOs(
			List<TestSection> sections,
			Map<Long, List<ReadingParagraph>> paragraphsBySectionId,
			Map<Long, List<ListeningSegment>> segmentsBySectionId,
			Map<Long, List<Question>> questionsBySectionId,
			Map<Long, List<QuestionOption>> optionsByQuestionId,
			Map<Long, List<QuestionAnswer>> answersByQuestionId
	) {
		return sections.stream()
				.sorted(Comparator.comparing(TestSection::getSectionOrder).thenComparing(TestSection::getId))
				.map(section -> toSectionRDO(
						section,
						paragraphsBySectionId.getOrDefault(section.getId(), List.of()),
						segmentsBySectionId.getOrDefault(section.getId(), List.of()),
						questionsBySectionId.getOrDefault(section.getId(), List.of()),
						optionsByQuestionId,
						answersByQuestionId))
				.toList();
	}

	private TeacherTestSectionRDO toSectionRDO(
			TestSection section,
			List<ReadingParagraph> paragraphs,
			List<ListeningSegment> segments,
			List<Question> questions,
			Map<Long, List<QuestionOption>> optionsByQuestionId,
			Map<Long, List<QuestionAnswer>> answersByQuestionId
	) {
		return new TeacherTestSectionRDO(
				section.getId(),
				section.getTitle(),
				section.getInstructions(),
				toRichTextContent(section.getInstructionsDocument(),
						section.getInstructionsText(),
						section.getInstructionsRenderedHtml(),
						section.getInstructions()),
				section.getPassageText(),
				toRichTextContent(section.getPassageDocument(),
						section.getPassageText(),
						section.getPassageTextRenderedHtml(),
						section.getPassageText()),
				section.getAudioFileId(),
				section.getSectionOrder(),
				section.getDurationMinutes(),
				toParagraphRDOs(paragraphs),
				toSegmentRDOs(segments),
				toQuestionRDOs(questions, optionsByQuestionId, answersByQuestionId),
				section.getCreatedAt(),
				section.getUpdatedAt());
	}

	private List<TeacherReadingParagraphRDO> toParagraphRDOs(List<ReadingParagraph> paragraphs) {
		return paragraphs.stream()
				.sorted(Comparator.comparing(ReadingParagraph::getParagraphOrder).thenComparing(ReadingParagraph::getId))
				.map(this::toParagraphRDO)
				.toList();
	}

	private TeacherReadingParagraphRDO toParagraphRDO(ReadingParagraph paragraph) {
		RichTextContentRDO content = toRichTextContent(
				paragraph.getContentDocument(),
				paragraph.getContent(),
				paragraph.getContentRenderedHtml(),
				paragraph.getContent());
		return new TeacherReadingParagraphRDO(
				paragraph.getId(),
				paragraph.getLabel(),
				paragraph.getContent(),
				content == null ? null : content.contentDocument(),
				content == null ? null : content.contentText(),
				content == null ? null : content.renderedHtml(),
				paragraph.getParagraphOrder());
	}

	private List<TeacherListeningSegmentRDO> toSegmentRDOs(List<ListeningSegment> segments) {
		return segments.stream()
				.sorted(Comparator.comparing(ListeningSegment::getSegmentOrder).thenComparing(ListeningSegment::getId))
				.map(segment -> new TeacherListeningSegmentRDO(
						segment.getId(),
						segment.getTitle(),
						segment.getStartSeconds(),
						segment.getEndSeconds(),
						segment.getTranscript(),
						segment.getSegmentOrder()))
				.toList();
	}

	private List<TeacherQuestionRDO> toQuestionRDOs(
			List<Question> questions,
			Map<Long, List<QuestionOption>> optionsByQuestionId,
			Map<Long, List<QuestionAnswer>> answersByQuestionId
	) {
		return questions.stream()
				.sorted(Comparator.comparing(Question::getQuestionOrder).thenComparing(Question::getId))
				.map(question -> new TeacherQuestionRDO(
						question.getId(),
						question.getQuestionType(),
						question.getQuestionText(),
						toRichTextContent(question.getQuestionDocument(),
								question.getQuestionText(),
								question.getQuestionRenderedHtml(),
								question.getQuestionText()),
						question.getInstructions(),
						toRichTextContent(question.getInstructionsDocument(),
								question.getInstructionsText(),
								question.getInstructionsRenderedHtml(),
								question.getInstructions()),
						question.getScore(),
						question.getQuestionOrder(),
						question.getRelatedParagraphLabel(),
						question.getListeningSegmentId(),
						toQuestionOptionRDOs(optionsByQuestionId.getOrDefault(question.getId(), List.of())),
						toQuestionAnswerRDOs(answersByQuestionId.getOrDefault(question.getId(), List.of()))))
				.toList();
	}

	private List<TeacherQuestionOptionRDO> toQuestionOptionRDOs(List<QuestionOption> options) {
		return options.stream()
				.sorted(Comparator.comparing(QuestionOption::getOptionOrder).thenComparing(QuestionOption::getId))
				.map(option -> new TeacherQuestionOptionRDO(
						option.getId(),
						option.getOptionLabel(),
						option.getOptionText(),
						toRichTextContent(
								option.getOptionDocument(),
								option.getOptionText(),
								option.getOptionRenderedHtml(),
								option.getOptionText()),
						option.getOptionOrder()))
				.toList();
	}

	private List<TeacherQuestionAnswerRDO> toQuestionAnswerRDOs(List<QuestionAnswer> answers) {
		return answers.stream()
				.sorted(Comparator.comparing(QuestionAnswer::getAnswerOrder).thenComparing(QuestionAnswer::getId))
				.map(answer -> new TeacherQuestionAnswerRDO(
						answer.getId(),
						answer.getAnswerValue(),
						answer.getNormalizedValue(),
						answer.isPrimary(),
						answer.getAnswerOrder(),
						answer.getExplanation(),
						toRichTextContent(
								answer.getExplanationDocument(),
								answer.getExplanation(),
								answer.getExplanationRenderedHtml(),
								answer.getExplanation())))
				.toList();
	}

	private RichTextContentRDO toRichTextContent(
			String document,
			String text,
			String renderedHtml,
			String legacyText
	) {
		SanitizedRichText content = richTextService.fromStored(document, text, renderedHtml, legacyText);
		if (content == null) {
			return null;
		}
		return new RichTextContentRDO(readDocument(content.document()), content.text(), content.renderedHtml());
	}

	private JsonNode readDocument(String document) {
		if (document == null) {
			return null;
		}
		try {
			return objectMapper.readTree(document);
		}
		catch (JsonProcessingException exception) {
			return null;
		}
	}
}
