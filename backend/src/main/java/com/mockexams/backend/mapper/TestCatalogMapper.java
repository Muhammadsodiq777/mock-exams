package com.mockexams.backend.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.RichTextContentRDO;
import com.mockexams.backend.dto.StudentListeningSegmentRDO;
import com.mockexams.backend.dto.StudentQuestionOptionRDO;
import com.mockexams.backend.dto.StudentQuestionRDO;
import com.mockexams.backend.dto.TestCardRDO;
import com.mockexams.backend.dto.TestCategoryRDO;
import com.mockexams.backend.dto.TestDetailRDO;
import com.mockexams.backend.dto.TestDetailSectionRDO;
import com.mockexams.backend.dto.TestTagRDO;
import com.mockexams.backend.dto.TestTeacherRDO;
import com.mockexams.backend.dto.TeacherReadingParagraphRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.ListeningSegment;
import com.mockexams.backend.entity.Question;
import com.mockexams.backend.entity.QuestionOption;
import com.mockexams.backend.entity.ReadingParagraph;
import com.mockexams.backend.entity.TestCategory;
import com.mockexams.backend.entity.TestSection;
import com.mockexams.backend.entity.TestTag;
import com.mockexams.backend.service.RichTextService;
import com.mockexams.backend.service.RichTextService.SanitizedRichText;
import com.mockexams.backend.teachers.entity.TeacherProfile;

@Component
public class TestCatalogMapper {

	private final RichTextService richTextService;
	private final ObjectMapper objectMapper;

	public TestCatalogMapper(RichTextService richTextService, ObjectMapper objectMapper) {
		this.richTextService = richTextService;
		this.objectMapper = objectMapper;
	}

	public TestCardRDO toCardRDO(CatalogTest test) {
		return new TestCardRDO(
				test.getId(),
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
				test.getPublishedAt(),
				toTeacherRDO(test.getTeacherProfile()),
				toCategoryRDOs(test),
				toTagRDOs(test));
	}

	public TestDetailRDO toDetailRDO(
			CatalogTest test,
			List<TestSection> sections,
			Map<Long, List<ReadingParagraph>> paragraphsBySectionId,
			Map<Long, List<ListeningSegment>> segmentsBySectionId,
			Map<Long, List<Question>> questionsBySectionId,
			Map<Long, List<QuestionOption>> optionsByQuestionId
	) {
		return new TestDetailRDO(
				test.getId(),
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
				test.getPublishedAt(),
				toTeacherRDO(test.getTeacherProfile()),
				toCategoryRDOs(test),
				toTagRDOs(test),
				toDetailSectionRDOs(
						sections,
						paragraphsBySectionId,
						segmentsBySectionId,
						questionsBySectionId,
						optionsByQuestionId));
	}

	public TestCategoryRDO toCategoryRDO(TestCategory category) {
		Long parentId = category.getParent() == null ? null : category.getParent().getId();
		return new TestCategoryRDO(
				category.getId(),
				parentId,
				category.getName(),
				category.getSlug(),
				category.getSortOrder());
	}

	public TestTagRDO toTagRDO(TestTag tag) {
		return new TestTagRDO(tag.getId(), tag.getName(), tag.getSlug());
	}

	private TestTeacherRDO toTeacherRDO(TeacherProfile teacherProfile) {
		if (teacherProfile == null) {
			return null;
		}
		return new TestTeacherRDO(
				teacherProfile.getId(),
				teacherProfile.getDisplayName(),
				teacherProfile.getHeadline(),
				teacherProfile.getRatingAverage(),
				teacherProfile.getRatingCount());
	}

	private List<TestCategoryRDO> toCategoryRDOs(CatalogTest test) {
		return test.getCategories().stream()
				.filter(category -> Boolean.TRUE.equals(category.isActive()))
				.sorted(Comparator.comparing(TestCategory::getSortOrder).thenComparing(TestCategory::getName))
				.map(this::toCategoryRDO)
				.toList();
	}

	private List<TestTagRDO> toTagRDOs(CatalogTest test) {
		return test.getTags().stream()
				.sorted(Comparator.comparing(TestTag::getName))
				.map(this::toTagRDO)
				.toList();
	}

	private List<TestDetailSectionRDO> toDetailSectionRDOs(
			List<TestSection> sections,
			Map<Long, List<ReadingParagraph>> paragraphsBySectionId,
			Map<Long, List<ListeningSegment>> segmentsBySectionId,
			Map<Long, List<Question>> questionsBySectionId,
			Map<Long, List<QuestionOption>> optionsByQuestionId
	) {
		return sections.stream()
				.sorted(Comparator.comparing(TestSection::getSectionOrder).thenComparing(TestSection::getId))
				.map(section -> new TestDetailSectionRDO(
						section.getId(),
						section.getTitle(),
						section.getInstructions(),
						toRichTextContent(
								section.getInstructionsDocument(),
								section.getInstructionsText(),
								section.getInstructionsRenderedHtml(),
								section.getInstructions()),
						section.getPassageText(),
						toRichTextContent(
								section.getPassageDocument(),
								section.getPassageText(),
								section.getPassageTextRenderedHtml(),
								section.getPassageText()),
						section.getAudioFileId(),
						section.getSectionOrder(),
						section.getDurationMinutes(),
						toParagraphRDOs(paragraphsBySectionId.getOrDefault(section.getId(), List.of())),
						toSegmentRDOs(segmentsBySectionId.getOrDefault(section.getId(), List.of())),
						toStudentQuestionRDOs(
								questionsBySectionId.getOrDefault(section.getId(), List.of()),
								optionsByQuestionId)))
				.toList();
	}

	private List<TeacherReadingParagraphRDO> toParagraphRDOs(List<ReadingParagraph> paragraphs) {
		return paragraphs.stream()
				.sorted(Comparator.comparing(ReadingParagraph::getParagraphOrder).thenComparing(ReadingParagraph::getId))
				.map(paragraph -> {
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
				})
				.toList();
	}

	private List<StudentListeningSegmentRDO> toSegmentRDOs(List<ListeningSegment> segments) {
		return segments.stream()
				.sorted(Comparator.comparing(ListeningSegment::getSegmentOrder).thenComparing(ListeningSegment::getId))
				.map(segment -> new StudentListeningSegmentRDO(
						segment.getId(),
						segment.getTitle(),
						segment.getStartSeconds(),
						segment.getEndSeconds(),
						segment.getSegmentOrder()))
				.toList();
	}

	private List<StudentQuestionRDO> toStudentQuestionRDOs(
			List<Question> questions,
			Map<Long, List<QuestionOption>> optionsByQuestionId
	) {
		return questions.stream()
				.sorted(Comparator.comparing(Question::getQuestionOrder).thenComparing(Question::getId))
				.map(question -> new StudentQuestionRDO(
						question.getId(),
						question.getQuestionType(),
						question.getQuestionText(),
						toRichTextContent(
								question.getQuestionDocument(),
								question.getQuestionText(),
								question.getQuestionRenderedHtml(),
								question.getQuestionText()),
						question.getInstructions(),
						toRichTextContent(
								question.getInstructionsDocument(),
								question.getInstructionsText(),
								question.getInstructionsRenderedHtml(),
								question.getInstructions()),
						question.getQuestionOrder(),
						question.getRelatedParagraphLabel(),
						question.getListeningSegmentId(),
						toStudentQuestionOptionRDOs(optionsByQuestionId.getOrDefault(question.getId(), List.of()))))
				.toList();
	}

	private List<StudentQuestionOptionRDO> toStudentQuestionOptionRDOs(List<QuestionOption> options) {
		return options.stream()
				.sorted(Comparator.comparing(QuestionOption::getOptionOrder).thenComparing(QuestionOption::getId))
				.map(option -> new StudentQuestionOptionRDO(
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
