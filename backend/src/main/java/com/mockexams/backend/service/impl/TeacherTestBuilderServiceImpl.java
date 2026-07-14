package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.TeacherListeningSegmentCDO;
import com.mockexams.backend.dto.TeacherListeningSegmentReorderUDO;
import com.mockexams.backend.dto.TeacherListeningSegmentUDO;
import com.mockexams.backend.dto.TeacherQuestionAnswerCDO;
import com.mockexams.backend.dto.TeacherQuestionCDO;
import com.mockexams.backend.dto.TeacherQuestionOptionCDO;
import com.mockexams.backend.dto.TeacherQuestionUDO;
import com.mockexams.backend.dto.TeacherReadingParagraphCDO;
import com.mockexams.backend.dto.TeacherReadingParagraphReorderUDO;
import com.mockexams.backend.dto.TeacherReadingParagraphUDO;
import com.mockexams.backend.dto.TeacherTestCDO;
import com.mockexams.backend.dto.TeacherTestRDO;
import com.mockexams.backend.dto.TeacherTestSectionCDO;
import com.mockexams.backend.dto.TeacherTestSectionReorderUDO;
import com.mockexams.backend.dto.TeacherTestSectionUDO;
import com.mockexams.backend.dto.TeacherTestSummaryRDO;
import com.mockexams.backend.dto.TeacherTestUDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.FileMetadata;
import com.mockexams.backend.entity.FilePurpose;
import com.mockexams.backend.entity.ListeningSegment;
import com.mockexams.backend.entity.Question;
import com.mockexams.backend.entity.QuestionAnswer;
import com.mockexams.backend.entity.QuestionOption;
import com.mockexams.backend.entity.ReadingParagraph;
import com.mockexams.backend.entity.TestSection;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.TeacherTestBuilderMapper;
import com.mockexams.backend.repository.FileMetadataRepository;
import com.mockexams.backend.repository.ListeningSegmentRepository;
import com.mockexams.backend.repository.QuestionAnswerRepository;
import com.mockexams.backend.repository.QuestionOptionRepository;
import com.mockexams.backend.repository.QuestionRepository;
import com.mockexams.backend.repository.ReadingParagraphRepository;
import com.mockexams.backend.repository.TestCatalogRepository;
import com.mockexams.backend.repository.TestSectionRepository;
import com.mockexams.backend.service.AnswerNormalizationService;
import com.mockexams.backend.service.RichTextService;
import com.mockexams.backend.service.RichTextService.SanitizedRichText;
import com.mockexams.backend.service.TeacherTestBuilderService;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.teachers.service.TeacherApprovalService;
import com.mockexams.backend.users.entity.User;

@Service
class TeacherTestBuilderServiceImpl implements TeacherTestBuilderService {

	private static final BigDecimal ZERO_PRICE = BigDecimal.ZERO.setScale(2);
	private static final String DEFAULT_TYPE = TestTypes.READING;
	private static final int DEFAULT_DURATION_MINUTES = 1;
	private static final String DEFAULT_CURRENCY = "UZS";
	private static final String DEFAULT_LANGUAGE = "en";
	private static final BigDecimal DEFAULT_QUESTION_SCORE = BigDecimal.ONE.setScale(2);
	private static final Set<String> SUPPORTED_QUESTION_TYPES = Set.of(
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
			"ordering",
			"essay_answer",
			"audio_answer");
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
	private static final Set<String> OPTION_QUESTION_TYPES = Set.of(
			"multiple_choice",
			"matching_headings",
			"matching_information",
			"ordering");

	private final TeacherApprovalService teacherApprovalService;
	private final TeacherProfileRepository teacherProfileRepository;
	private final TestCatalogRepository testCatalogRepository;
	private final TestSectionRepository testSectionRepository;
	private final ReadingParagraphRepository readingParagraphRepository;
	private final ListeningSegmentRepository listeningSegmentRepository;
	private final QuestionRepository questionRepository;
	private final QuestionOptionRepository questionOptionRepository;
	private final QuestionAnswerRepository questionAnswerRepository;
	private final FileMetadataRepository fileMetadataRepository;
	private final AnswerNormalizationService answerNormalizationService;
	private final RichTextService richTextService;
	private final TeacherTestBuilderMapper teacherTestBuilderMapper;

	TeacherTestBuilderServiceImpl(
			TeacherApprovalService teacherApprovalService,
			TeacherProfileRepository teacherProfileRepository,
			TestCatalogRepository testCatalogRepository,
			TestSectionRepository testSectionRepository,
			ReadingParagraphRepository readingParagraphRepository,
			ListeningSegmentRepository listeningSegmentRepository,
			QuestionRepository questionRepository,
			QuestionOptionRepository questionOptionRepository,
			QuestionAnswerRepository questionAnswerRepository,
			FileMetadataRepository fileMetadataRepository,
			AnswerNormalizationService answerNormalizationService,
			RichTextService richTextService,
			TeacherTestBuilderMapper teacherTestBuilderMapper
	) {
		this.teacherApprovalService = teacherApprovalService;
		this.teacherProfileRepository = teacherProfileRepository;
		this.testCatalogRepository = testCatalogRepository;
		this.testSectionRepository = testSectionRepository;
		this.readingParagraphRepository = readingParagraphRepository;
		this.listeningSegmentRepository = listeningSegmentRepository;
		this.questionRepository = questionRepository;
		this.questionOptionRepository = questionOptionRepository;
		this.questionAnswerRepository = questionAnswerRepository;
		this.fileMetadataRepository = fileMetadataRepository;
		this.answerNormalizationService = answerNormalizationService;
		this.richTextService = richTextService;
		this.teacherTestBuilderMapper = teacherTestBuilderMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TeacherTestSummaryRDO> listOwnedTests(
			User teacher,
			String visibility,
			String search,
			Pageable pageable
	) {
		Long teacherProfileId = teacherApprovalService.requireApprovedForPublishing(teacher);
		return testCatalogRepository.findTeacherOwnedTests(
						teacherProfileId,
						visibility,
						searchPattern(search),
						pageable)
				.map(teacherTestBuilderMapper::toSummaryRDO);
	}

	@Override
	@Transactional
	public TeacherTestRDO createDraft(User teacher, TeacherTestCDO command) {
		Long teacherProfileId = teacherApprovalService.requireApprovedForPublishing(teacher);
		TeacherProfile teacherProfile = teacherProfileRepository.findById(teacherProfileId)
				.orElseThrow(this::notFound);
		validateMetadataValues(command.estimatedBandMin(), command.estimatedBandMax(), command.type(),
				command.priceAmount(), command.currency(), command.free(), command.language());
		CatalogTest test = CatalogTest.createDraft(
				teacherProfile,
				teacher.getId(),
				textOrEmpty(command.title()),
				trimToNull(command.description()),
				normalizeType(command.type()),
				trimToNull(command.difficulty()),
				command.estimatedBandMin(),
				command.estimatedBandMax(),
				valueOrDefault(command.durationMinutes(), DEFAULT_DURATION_MINUTES),
				normalizePrice(command.priceAmount(), command.free()),
				normalizeCurrency(command.currency()),
				valueOrDefault(command.free(), true),
				normalizeLanguage(command.language()));
		return readModel(testCatalogRepository.save(test));
	}

	@Override
	@Transactional
	public TeacherTestRDO updateTest(User teacher, Long testId, TeacherTestUDO command) {
		Long teacherProfileId = teacherApprovalService.requireApprovedForPublishing(teacher);
		CatalogTest test = ownedTest(testId, teacherProfileId);
		requireEditable(test);
		String title = command.title() == null ? test.getTitle() : textOrEmpty(command.title());
		String description = command.description() == null ? test.getDescription() : trimToNull(command.description());
		String type = command.type() == null ? test.getType() : normalizeType(command.type());
		String difficulty = command.difficulty() == null ? test.getDifficulty() : trimToNull(command.difficulty());
		BigDecimal estimatedBandMin = valueOrDefault(command.estimatedBandMin(), test.getEstimatedBandMin());
		BigDecimal estimatedBandMax = valueOrDefault(command.estimatedBandMax(), test.getEstimatedBandMax());
		Integer durationMinutes = valueOrDefault(command.durationMinutes(), test.getDurationMinutes());
		Boolean free = valueOrDefault(command.free(), test.isFree());
		BigDecimal priceAmountForValidation = valueOrDefault(command.priceAmount(), test.getPriceAmount());
		BigDecimal priceAmount = command.priceAmount() == null
				? test.getPriceAmount()
				: normalizePrice(command.priceAmount(), free);
		String currency = command.currency() == null ? test.getCurrency() : normalizeCurrency(command.currency());
		String language = command.language() == null ? test.getLanguage() : normalizeLanguage(command.language());
		validateMetadataValues(
				estimatedBandMin,
				estimatedBandMax,
				command.type() == null ? type : command.type(),
				priceAmountForValidation,
				command.currency() == null ? currency : command.currency(),
				free,
				command.language() == null ? language : command.language());
		test.updateMetadata(
				title,
				description,
				type,
				difficulty,
				estimatedBandMin,
				estimatedBandMax,
				durationMinutes,
				priceAmount,
				currency,
				free,
				language);
		return readModel(test);
	}

	@Override
	@Transactional
	public TeacherTestRDO addSection(User teacher, Long testId, TeacherTestSectionCDO command) {
		Long teacherProfileId = teacherApprovalService.requireApprovedForPublishing(teacher);
		CatalogTest test = ownedTest(testId, teacherProfileId);
		requireEditable(test);
		markDraftIfRejected(test);
		int nextOrder = testSectionRepository.maxSectionOrder(test.getId()) + 1;
		SanitizedRichText instructions = command.instructionsDocument() == null
				? richTextService.fromPlainText(command.instructions(), false, "Section instructions")
				: richTextService.fromDocument(command.instructionsDocument(), false, "Section instructions");
		SanitizedRichText passage = command.passageDocument() == null
				? richTextService.fromPlainText(command.passageText(), false, "Passage text")
				: richTextService.fromDocument(command.passageDocument(), false, "Passage text");
		TestSection section = TestSection.create(
				test,
				textOrEmpty(command.title()),
				contentText(instructions),
				contentDocument(instructions),
				contentText(instructions),
				renderedHtml(instructions),
				contentText(passage),
				contentDocument(passage),
				renderedHtml(passage),
				normalizeListeningAudioFileId(command.audioFileId(), teacher.getId()),
				nextOrder,
				command.durationMinutes());
		testSectionRepository.save(section);
		return readModel(test);
	}

	@Override
	@Transactional
	public TeacherTestRDO updateSection(User teacher, Long testId, Long sectionId, TeacherTestSectionUDO command) {
		Long teacherProfileId = teacherApprovalService.requireApprovedForPublishing(teacher);
		CatalogTest test = ownedTest(testId, teacherProfileId);
		requireEditable(test);
		markDraftIfRejected(test);
		TestSection section = testSectionRepository.findByIdAndTestId(sectionId, test.getId())
				.orElseThrow(this::sectionNotFound);
		String title = command.title() == null ? section.getTitle() : textOrEmpty(command.title());
		SanitizedRichText instructions = sectionRichText(
				command.instructionsDocument(),
				command.instructions(),
				section.getInstructionsDocument(),
				section.getInstructionsText(),
				section.getInstructionsRenderedHtml(),
				section.getInstructions(),
				false,
				"Section instructions");
		SanitizedRichText passage = sectionRichText(
				command.passageDocument(),
				command.passageText(),
				section.getPassageDocument(),
				section.getPassageText(),
				section.getPassageTextRenderedHtml(),
				section.getPassageText(),
				false,
				"Passage text");
		Long audioFileId = command.audioFileId() == null
				? section.getAudioFileId()
				: normalizeListeningAudioFileId(command.audioFileId(), teacher.getId());
		Integer durationMinutes = valueOrDefault(command.durationMinutes(), section.getDurationMinutes());
		section.update(
				title,
				contentText(instructions),
				contentDocument(instructions),
				contentText(instructions),
				renderedHtml(instructions),
				contentText(passage),
				contentDocument(passage),
				renderedHtml(passage),
				audioFileId,
				durationMinutes);
		return readModel(test);
	}

	@Override
	@Transactional
	public TeacherTestRDO reorderSections(User teacher, Long testId, TeacherTestSectionReorderUDO command) {
		Long teacherProfileId = teacherApprovalService.requireApprovedForPublishing(teacher);
		CatalogTest test = ownedTest(testId, teacherProfileId);
		requireEditable(test);
		markDraftIfRejected(test);
		List<TestSection> currentSections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(test.getId());
		validateReorderList(currentSections, command.sectionIds());
		Map<Long, TestSection> sectionById = currentSections.stream()
				.collect(Collectors.toMap(TestSection::getId, Function.identity()));
		int temporaryOffset = currentSections.size() + 1;
		for (int index = 0; index < command.sectionIds().size(); index++) {
			sectionById.get(command.sectionIds().get(index)).setSectionOrder(temporaryOffset + index);
		}
		testSectionRepository.flush();
		for (int index = 0; index < command.sectionIds().size(); index++) {
			sectionById.get(command.sectionIds().get(index)).setSectionOrder(index + 1);
		}
		testSectionRepository.flush();
		return readModel(test);
	}

	@Override
	@Transactional
	public TeacherTestRDO deleteSection(User teacher, Long testId, Long sectionId) {
		Long teacherProfileId = teacherApprovalService.requireApprovedForPublishing(teacher);
		CatalogTest test = ownedTest(testId, teacherProfileId);
		requireEditable(test);
		markDraftIfRejected(test);
		TestSection section = testSectionRepository.findByIdAndTestId(sectionId, test.getId())
				.orElseThrow(this::sectionNotFound);
		testSectionRepository.delete(section);
		testSectionRepository.flush();
		renumberSections(test.getId());
		return readModel(test);
	}

	@Override
	@Transactional
	public TeacherTestRDO addParagraph(
			User teacher,
			Long testId,
			Long sectionId,
			TeacherReadingParagraphCDO command
	) {
		OwnedSection ownedSection = ownedEditableSection(teacher, testId, sectionId);
		String label = trimToNull(command.label());
		SanitizedRichText content = command.contentDocument() == null
				? richTextService.fromPlainText(command.content(), true, "Paragraph content")
				: richTextService.fromDocument(command.contentDocument(), true, "Paragraph content");
		validateParagraphLabelAvailable(sectionId, label, null);
		int nextOrder = readingParagraphRepository.maxParagraphOrder(sectionId) + 1;
		readingParagraphRepository.save(ReadingParagraph.create(
				ownedSection.section(),
				label,
				contentText(content),
				contentDocument(content),
				renderedHtml(content),
				nextOrder));
		return readModel(ownedSection.test());
	}

	@Override
	@Transactional
	public TeacherTestRDO updateParagraph(
			User teacher,
			Long testId,
			Long sectionId,
			Long paragraphId,
			TeacherReadingParagraphUDO command
	) {
		OwnedSection ownedSection = ownedEditableSection(teacher, testId, sectionId);
		ReadingParagraph paragraph = readingParagraphRepository.findByIdAndSectionId(paragraphId, sectionId)
				.orElseThrow(this::paragraphNotFound);
		String label = command.label() == null
				? paragraph.getLabel()
				: trimToNull(command.label());
		SanitizedRichText content = paragraphRichText(command, paragraph);
		validateParagraphLabelAvailable(sectionId, label, paragraph.getId());
		if (!Objects.equals(paragraph.getLabel(), label) && paragraph.getLabel() != null) {
			questionRepository.findBySectionIdAndRelatedParagraphLabel(sectionId, paragraph.getLabel())
					.forEach(question -> question.setRelatedParagraphLabel(label));
		}
		paragraph.update(label, contentText(content), contentDocument(content), renderedHtml(content));
		return readModel(ownedSection.test());
	}

	@Override
	@Transactional
	public TeacherTestRDO reorderParagraphs(
			User teacher,
			Long testId,
			Long sectionId,
			TeacherReadingParagraphReorderUDO command
	) {
		OwnedSection ownedSection = ownedEditableSection(teacher, testId, sectionId);
		List<ReadingParagraph> currentParagraphs = readingParagraphRepository.findBySectionIdOrderByParagraphOrderAsc(sectionId);
		validateParagraphReorderList(currentParagraphs, command.paragraphIds());
		Map<Long, ReadingParagraph> paragraphById = currentParagraphs.stream()
				.collect(Collectors.toMap(ReadingParagraph::getId, Function.identity()));
		int temporaryOffset = currentParagraphs.size() + 1;
		for (int index = 0; index < command.paragraphIds().size(); index++) {
			paragraphById.get(command.paragraphIds().get(index)).setParagraphOrder(temporaryOffset + index);
		}
		readingParagraphRepository.flush();
		for (int index = 0; index < command.paragraphIds().size(); index++) {
			paragraphById.get(command.paragraphIds().get(index)).setParagraphOrder(index + 1);
		}
		readingParagraphRepository.flush();
		return readModel(ownedSection.test());
	}

	@Override
	@Transactional
	public TeacherTestRDO deleteParagraph(User teacher, Long testId, Long sectionId, Long paragraphId) {
		OwnedSection ownedSection = ownedEditableSection(teacher, testId, sectionId);
		ReadingParagraph paragraph = readingParagraphRepository.findByIdAndSectionId(paragraphId, sectionId)
				.orElseThrow(this::paragraphNotFound);
		if (paragraph.getLabel() != null
				&& questionRepository.existsBySectionIdAndRelatedParagraphLabel(sectionId, paragraph.getLabel())) {
			throw validationFailed("Paragraph cannot be deleted while questions are linked to it.");
		}
		readingParagraphRepository.delete(paragraph);
		readingParagraphRepository.flush();
		renumberParagraphs(sectionId);
		return readModel(ownedSection.test());
	}

	@Override
	@Transactional
	public TeacherTestRDO addListeningSegment(
			User teacher,
			Long testId,
			Long sectionId,
			TeacherListeningSegmentCDO command
	) {
		OwnedSection ownedSection = ownedEditableSection(teacher, testId, sectionId);
		validateSegmentTimeRange(command.startSeconds(), command.endSeconds());
		int nextOrder = listeningSegmentRepository.maxSegmentOrder(sectionId) + 1;
		listeningSegmentRepository.save(ListeningSegment.create(
				ownedSection.section(),
				trimToNull(command.title()),
				command.startSeconds(),
				command.endSeconds(),
				trimToNull(command.transcript()),
				nextOrder));
		return readModel(ownedSection.test());
	}

	@Override
	@Transactional
	public TeacherTestRDO updateListeningSegment(
			User teacher,
			Long testId,
			Long sectionId,
			Long segmentId,
			TeacherListeningSegmentUDO command
	) {
		OwnedSection ownedSection = ownedEditableSection(teacher, testId, sectionId);
		ListeningSegment segment = listeningSegmentRepository.findByIdAndSectionId(segmentId, sectionId)
				.orElseThrow(this::listeningSegmentNotFound);
		Integer startSeconds = valueOrDefault(command.startSeconds(), segment.getStartSeconds());
		Integer endSeconds = valueOrDefault(command.endSeconds(), segment.getEndSeconds());
		validateSegmentTimeRange(startSeconds, endSeconds);
		String title = command.title() == null ? segment.getTitle() : trimToNull(command.title());
		String transcript = command.transcript() == null ? segment.getTranscript() : trimToNull(command.transcript());
		segment.update(title, startSeconds, endSeconds, transcript);
		return readModel(ownedSection.test());
	}

	@Override
	@Transactional
	public TeacherTestRDO reorderListeningSegments(
			User teacher,
			Long testId,
			Long sectionId,
			TeacherListeningSegmentReorderUDO command
	) {
		OwnedSection ownedSection = ownedEditableSection(teacher, testId, sectionId);
		List<ListeningSegment> currentSegments = listeningSegmentRepository.findBySectionIdOrderBySegmentOrderAsc(sectionId);
		validateSegmentReorderList(currentSegments, command.segmentIds());
		Map<Long, ListeningSegment> segmentById = currentSegments.stream()
				.collect(Collectors.toMap(ListeningSegment::getId, Function.identity()));
		int temporaryOffset = currentSegments.size() + 1;
		for (int index = 0; index < command.segmentIds().size(); index++) {
			segmentById.get(command.segmentIds().get(index)).setSegmentOrder(temporaryOffset + index);
		}
		listeningSegmentRepository.flush();
		for (int index = 0; index < command.segmentIds().size(); index++) {
			segmentById.get(command.segmentIds().get(index)).setSegmentOrder(index + 1);
		}
		listeningSegmentRepository.flush();
		return readModel(ownedSection.test());
	}

	@Override
	@Transactional
	public TeacherTestRDO deleteListeningSegment(User teacher, Long testId, Long sectionId, Long segmentId) {
		OwnedSection ownedSection = ownedEditableSection(teacher, testId, sectionId);
		ListeningSegment segment = listeningSegmentRepository.findByIdAndSectionId(segmentId, sectionId)
				.orElseThrow(this::listeningSegmentNotFound);
		listeningSegmentRepository.delete(segment);
		listeningSegmentRepository.flush();
		renumberListeningSegments(sectionId);
		return readModel(ownedSection.test());
	}

	@Override
	@Transactional
	public TeacherTestRDO addQuestion(User teacher, Long testId, Long sectionId, TeacherQuestionCDO command) {
		OwnedSection ownedSection = ownedEditableSection(teacher, testId, sectionId);
		String questionType = normalizeQuestionType(command.questionType());
		SanitizedRichText questionText = command.questionDocument() == null
				? richTextService.fromPlainText(command.questionText(), true, "Question text")
				: richTextService.fromDocument(command.questionDocument(), true, "Question text");
		SanitizedRichText instructions = command.instructionsDocument() == null
				? richTextService.fromPlainText(command.instructions(), false, "Question instructions")
				: richTextService.fromDocument(command.instructionsDocument(), false, "Question instructions");
		BigDecimal score = valueOrDefault(command.score(), DEFAULT_QUESTION_SCORE);
		String relatedParagraphLabel = normalizeRelatedParagraphLabel(sectionId, command.relatedParagraphLabel());
		Long listeningSegmentId = normalizeListeningSegmentId(sectionId, command.listeningSegmentId());
		List<PreparedQuestionOption> options = prepareQuestionOptions(questionType, command.options());
		List<PreparedQuestionAnswer> answers = prepareQuestionAnswers(questionType, command.answers());
		int nextOrder = questionRepository.maxQuestionOrder(sectionId) + 1;
		Question question = questionRepository.save(Question.create(
				ownedSection.section(),
				questionType,
				contentText(questionText),
				contentDocument(questionText),
				renderedHtml(questionText),
				contentText(instructions),
				contentDocument(instructions),
				contentText(instructions),
				renderedHtml(instructions),
				score,
				nextOrder,
				relatedParagraphLabel,
				listeningSegmentId));
		saveQuestionOptions(question, options);
		saveQuestionAnswers(question, answers);
		return readModel(ownedSection.test());
	}

	@Override
	@Transactional
	public TeacherTestRDO updateQuestion(
			User teacher,
			Long testId,
			Long sectionId,
			Long questionId,
			TeacherQuestionUDO command
	) {
		OwnedSection ownedSection = ownedEditableSection(teacher, testId, sectionId);
		Question question = questionRepository.findByIdAndSectionId(questionId, sectionId)
				.orElseThrow(this::questionNotFound);
		String questionType = command.questionType() == null
				? question.getQuestionType()
				: normalizeQuestionType(command.questionType());
		SanitizedRichText questionText = questionTextRichText(command, question);
		SanitizedRichText instructions = questionInstructionsRichText(command, question);
		BigDecimal score = valueOrDefault(command.score(), question.getScore());
		String relatedParagraphLabel = command.relatedParagraphLabel() == null
				? question.getRelatedParagraphLabel()
				: normalizeRelatedParagraphLabel(sectionId, command.relatedParagraphLabel());
		Long listeningSegmentId = command.hasListeningSegmentId()
				? normalizeListeningSegmentId(sectionId, command.listeningSegmentId())
				: question.getListeningSegmentId();
		List<PreparedQuestionOption> options = updateQuestionOptions(questionType, question.getQuestionType(), question.getId(),
				command.options());
		List<PreparedQuestionAnswer> answers = updateQuestionAnswers(questionType, question.getId(), command.answers());
		question.update(
				questionType,
				contentText(questionText),
				contentDocument(questionText),
				renderedHtml(questionText),
				contentText(instructions),
				contentDocument(instructions),
				contentText(instructions),
				renderedHtml(instructions),
				score,
				relatedParagraphLabel,
				listeningSegmentId);
		replaceQuestionOptions(question, options);
		replaceQuestionAnswers(question, answers);
		return readModel(ownedSection.test());
	}

	@Override
	@Transactional
	public TeacherTestRDO deleteQuestion(User teacher, Long testId, Long sectionId, Long questionId) {
		OwnedSection ownedSection = ownedEditableSection(teacher, testId, sectionId);
		Question question = questionRepository.findByIdAndSectionId(questionId, sectionId)
				.orElseThrow(this::questionNotFound);
		questionRepository.delete(question);
		questionRepository.flush();
		renumberQuestions(sectionId);
		return readModel(ownedSection.test());
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherTestRDO preview(User teacher, Long testId) {
		Long teacherProfileId = teacherApprovalService.requireApprovedForPublishing(teacher);
		return readModel(ownedTest(testId, teacherProfileId));
	}

	@Override
	@Transactional
	public TeacherTestRDO submit(User teacher, Long testId) {
		Long teacherProfileId = teacherApprovalService.requireApprovedForPublishing(teacher);
		CatalogTest test = ownedTest(testId, teacherProfileId);
		if (!isEditable(test)) {
			throw statusConflict();
		}
		if (!collectSubmissionFailures(test).isEmpty()) {
			throw validationFailed("Test draft is missing required submission metadata.");
		}
		test.submitForModeration();
		return readModel(test);
	}

	@Override
	@Transactional
	public TeacherTestRDO archive(User teacher, Long testId) {
		Long teacherProfileId = teacherApprovalService.requireApprovedForPublishing(teacher);
		CatalogTest test = ownedTest(testId, teacherProfileId);
		if (!isArchivable(test)) {
			throw statusConflict();
		}
		test.markArchived();
		return readModel(test);
	}

	/**
	 * Re-runs the same readiness checks {@link #submit} enforces, for reuse by moderation at
	 * decision time. Returns a field-to-message map of failures (empty when ready) instead of
	 * throwing, so callers can report validation findings without duplicating this logic.
	 */
	Map<String, String> collectSubmissionFailures(CatalogTest test) {
		return validateReadyForSubmission(test);
	}

	/**
	 * Builds the full read model for an already-loaded test, without teacher-ownership scoping.
	 * Reused by moderation, which operates across teachers under admin authorization instead of
	 * owner authorization.
	 */
	TeacherTestRDO readModelFor(CatalogTest test) {
		return readModel(test);
	}

	private CatalogTest ownedTest(Long testId, Long teacherProfileId) {
		return testCatalogRepository.findByIdAndTeacherProfileIdAndDeletedAtIsNull(testId, teacherProfileId)
				.orElseThrow(this::notFound);
	}

	private TeacherTestRDO readModel(CatalogTest test) {
		List<TestSection> sections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(test.getId());
		List<Long> sectionIds = sections.stream().map(TestSection::getId).toList();
		Map<Long, List<ReadingParagraph>> paragraphsBySectionId = sectionIds.isEmpty()
				? Map.of()
				: readingParagraphRepository.findBySectionIds(sectionIds).stream()
						.collect(Collectors.groupingBy(
								paragraph -> paragraph.getSection().getId(),
								LinkedHashMap::new,
								Collectors.toList()));
		Map<Long, List<ListeningSegment>> segmentsBySectionId = sectionIds.isEmpty()
				? Map.of()
				: listeningSegmentRepository.findBySectionIds(sectionIds).stream()
						.collect(Collectors.groupingBy(
								segment -> segment.getSection().getId(),
								LinkedHashMap::new,
								Collectors.toList()));
		Map<Long, List<Question>> questionsBySectionId = sectionIds.isEmpty()
				? Map.of()
				: questionRepository.findBySectionIds(sectionIds).stream()
						.collect(Collectors.groupingBy(
								question -> question.getSection().getId(),
								LinkedHashMap::new,
								Collectors.toList()));
		List<Long> questionIds = questionsBySectionId.values().stream()
				.flatMap(List::stream)
				.map(Question::getId)
				.toList();
		Map<Long, List<QuestionOption>> optionsByQuestionId = questionIds.isEmpty()
				? Map.of()
				: questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAscIdAsc(questionIds).stream()
						.collect(Collectors.groupingBy(
								option -> option.getQuestion().getId(),
								LinkedHashMap::new,
								Collectors.toList()));
		Map<Long, List<QuestionAnswer>> answersByQuestionId = questionIds.isEmpty()
				? Map.of()
				: questionAnswerRepository.findByQuestionIdInOrderByQuestionIdAscAnswerOrderAscIdAsc(questionIds).stream()
						.collect(Collectors.groupingBy(
								answer -> answer.getQuestion().getId(),
								LinkedHashMap::new,
								Collectors.toList()));
		return teacherTestBuilderMapper.toRDO(
				test,
				sections,
				paragraphsBySectionId,
				segmentsBySectionId,
				questionsBySectionId,
				optionsByQuestionId,
				answersByQuestionId);
	}

	private OwnedSection ownedEditableSection(User teacher, Long testId, Long sectionId) {
		Long teacherProfileId = teacherApprovalService.requireApprovedForPublishing(teacher);
		CatalogTest test = ownedTest(testId, teacherProfileId);
		requireEditable(test);
		markDraftIfRejected(test);
		TestSection section = testSectionRepository.findByIdAndTestId(sectionId, test.getId())
				.orElseThrow(this::sectionNotFound);
		return new OwnedSection(test, section);
	}

	private void requireEditable(CatalogTest test) {
		if (!isEditable(test)) {
			throw statusConflict();
		}
	}

	private boolean isEditable(CatalogTest test) {
		return TestVisibilities.DRAFT.equals(test.getVisibility()) || TestVisibilities.REJECTED.equals(test.getVisibility());
	}

	private boolean isArchivable(CatalogTest test) {
		return TestVisibilities.DRAFT.equals(test.getVisibility())
				|| TestVisibilities.REJECTED.equals(test.getVisibility())
				|| TestVisibilities.PUBLISHED.equals(test.getVisibility());
	}

	private void markDraftIfRejected(CatalogTest test) {
		if (TestVisibilities.REJECTED.equals(test.getVisibility())) {
			test.markDraft();
		}
	}

	private Map<String, String> validateReadyForSubmission(CatalogTest test) {
		Map<String, String> failures = new LinkedHashMap<>();
		List<TestSection> sections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(test.getId());
		if (isBlank(test.getTitle())) {
			failures.put("title", "Title is required.");
		}
		if (isBlank(test.getType()) || !TestTypes.isSupported(test.getType())) {
			failures.put("type", "Supported test type is required.");
		}
		if (test.getDurationMinutes() == null || test.getDurationMinutes() <= 0) {
			failures.put("durationMinutes", "Duration must be positive.");
		}
		if (isBlank(test.getCurrency()) || test.getCurrency().length() != 3) {
			failures.put("currency", "Three-letter currency is required.");
		}
		if (isBlank(test.getLanguage())) {
			failures.put("language", "Language is required.");
		}
		if (test.getPriceAmount() == null || test.getPriceAmount().compareTo(BigDecimal.ZERO) < 0) {
			failures.put("priceAmount", "Price must be non-negative.");
		}
		if (Boolean.TRUE.equals(test.isFree()) && test.getPriceAmount() != null
				&& test.getPriceAmount().compareTo(BigDecimal.ZERO) != 0) {
			failures.put("free", "Free tests must have a zero price.");
		}
		if (test.getEstimatedBandMin() != null && test.getEstimatedBandMax() != null
				&& test.getEstimatedBandMin().compareTo(test.getEstimatedBandMax()) > 0) {
			failures.put("estimatedBandMin", "Estimated band minimum must be less than or equal to maximum.");
		}
		if (sections.isEmpty()) {
			failures.put("sections", "At least one section is required.");
		}
		if (TestTypes.READING.equals(test.getType())) {
			if (sections.size() != 3) {
				failures.put("sections", "Reading tests must have exactly three sections.");
			}
			for (int index = 0; index < sections.size(); index++) {
				TestSection section = sections.get(index);
				if (isBlank(section.getPassageText())) {
					failures.put("sections[" + index + "].passageText", "Reading section passage text is required.");
				}
			}
		}
		if (TestTypes.LISTENING.equals(test.getType())) {
			for (int index = 0; index < sections.size(); index++) {
				TestSection section = sections.get(index);
				if (section.getAudioFileId() == null) {
					failures.put("sections[" + index + "].audioFileId", "Listening section audio is required.");
				}
			}
		}
		return failures;
	}

	private void validateMetadataValues(
			BigDecimal estimatedBandMin,
			BigDecimal estimatedBandMax,
			String type,
			BigDecimal priceAmount,
			String currency,
			Boolean free,
			String language
	) {
		String normalizedType = trimToNull(type);
		if (normalizedType != null && !TestTypes.isSupported(normalizedType)) {
			throw validationFailed("Unsupported test type.");
		}
		if (estimatedBandMin != null && estimatedBandMax != null && estimatedBandMin.compareTo(estimatedBandMax) > 0) {
			throw validationFailed("estimatedBandMin must be less than or equal to estimatedBandMax.");
		}
		if (Boolean.TRUE.equals(free) && priceAmount != null && priceAmount.compareTo(BigDecimal.ZERO) != 0) {
			throw validationFailed("Free tests must have a zero price.");
		}
		String normalizedCurrency = trimToNull(currency);
		if (currency != null && (normalizedCurrency == null || normalizedCurrency.length() != 3)) {
			throw validationFailed("Currency must be a three-letter code.");
		}
		if (language != null && trimToNull(language) == null) {
			throw validationFailed("Language must not be blank.");
		}
	}

	private void validateReorderList(List<TestSection> currentSections, List<Long> requestedIds) {
		if (requestedIds.size() != currentSections.size()) {
			throw validationFailed("Reorder must include every section exactly once.");
		}
		Set<Long> requestedUniqueIds = new HashSet<>(requestedIds);
		if (requestedUniqueIds.size() != requestedIds.size()) {
			throw validationFailed("Reorder must include every section exactly once.");
		}
		Set<Long> currentIds = currentSections.stream()
				.map(TestSection::getId)
				.collect(Collectors.toSet());
		if (!Objects.equals(currentIds, requestedUniqueIds)) {
			throw validationFailed("Reorder must include every section exactly once.");
		}
	}

	private void validateParagraphReorderList(List<ReadingParagraph> currentParagraphs, List<Long> requestedIds) {
		if (requestedIds.size() != currentParagraphs.size()) {
			throw validationFailed("Reorder must include every paragraph exactly once.");
		}
		Set<Long> requestedUniqueIds = new HashSet<>(requestedIds);
		if (requestedUniqueIds.size() != requestedIds.size()) {
			throw validationFailed("Reorder must include every paragraph exactly once.");
		}
		Set<Long> currentIds = currentParagraphs.stream()
				.map(ReadingParagraph::getId)
				.collect(Collectors.toSet());
		if (!Objects.equals(currentIds, requestedUniqueIds)) {
			throw validationFailed("Reorder must include every paragraph exactly once.");
		}
	}

	private void validateSegmentReorderList(List<ListeningSegment> currentSegments, List<Long> requestedIds) {
		if (requestedIds.size() != currentSegments.size()) {
			throw validationFailed("Reorder must include every listening segment exactly once.");
		}
		Set<Long> requestedUniqueIds = new HashSet<>(requestedIds);
		if (requestedUniqueIds.size() != requestedIds.size()) {
			throw validationFailed("Reorder must include every listening segment exactly once.");
		}
		Set<Long> currentIds = currentSegments.stream()
				.map(ListeningSegment::getId)
				.collect(Collectors.toSet());
		if (!Objects.equals(currentIds, requestedUniqueIds)) {
			throw validationFailed("Reorder must include every listening segment exactly once.");
		}
	}

	private void validateSegmentTimeRange(Integer startSeconds, Integer endSeconds) {
		if (startSeconds == null || endSeconds == null) {
			throw validationFailed("Listening segment startSeconds and endSeconds are required.");
		}
		if (startSeconds < 0 || endSeconds <= startSeconds) {
			throw validationFailed("Listening segment endSeconds must be greater than startSeconds.");
		}
	}

	private List<PreparedQuestionOption> prepareQuestionOptions(
			String questionType,
			List<TeacherQuestionOptionCDO> commands
	) {
		List<TeacherQuestionOptionCDO> optionCommands = commands == null ? List.of() : commands;
		if (requiresOptions(questionType) && optionCommands.isEmpty()) {
			throw validationFailed("Question type requires at least one option.");
		}
		if (!requiresOptions(questionType) && !optionCommands.isEmpty()) {
			throw validationFailed("Question type does not support options.");
		}
		if (!requiresOptions(questionType) && optionCommands.isEmpty()) {
			return List.of();
		}
		Set<String> labels = new HashSet<>();
		List<PreparedQuestionOption> options = new java.util.ArrayList<>();
		for (int index = 0; index < optionCommands.size(); index++) {
			TeacherQuestionOptionCDO command = optionCommands.get(index);
			if (command == null) {
				throw validationFailed("Option is required.");
			}
			String label = requiredText(command.optionLabel(), "Option label is required.");
			if (!labels.add(label.toLowerCase(Locale.ROOT))) {
				throw validationFailed("Option labels must be unique within a question.");
			}
			SanitizedRichText optionText = command.optionDocument() == null
					? richTextService.fromPlainText(command.optionText(), true, "Option text")
					: richTextService.fromDocument(command.optionDocument(), true, "Option text");
			options.add(new PreparedQuestionOption(label, optionText, index + 1));
		}
		return options;
	}

	private List<PreparedQuestionAnswer> prepareQuestionAnswers(
			String questionType,
			List<TeacherQuestionAnswerCDO> commands
	) {
		List<TeacherQuestionAnswerCDO> answerCommands = commands == null ? List.of() : commands;
		if (OBJECTIVE_QUESTION_TYPES.contains(questionType) && answerCommands.isEmpty()) {
			throw validationFailed("Objective questions require at least one answer key.");
		}
		if (!OBJECTIVE_QUESTION_TYPES.contains(questionType) && answerCommands.isEmpty()) {
			return List.of();
		}
		Set<String> normalizedValues = new HashSet<>();
		List<PreparedQuestionAnswer> answers = new java.util.ArrayList<>();
		for (int index = 0; index < answerCommands.size(); index++) {
			TeacherQuestionAnswerCDO command = answerCommands.get(index);
			if (command == null) {
				throw validationFailed("Answer key is required.");
			}
			String answerValue = requiredText(command.answerValue(), "Answer value is required.");
			String normalizedValue = answerNormalizationService.normalize(answerValue);
			if (normalizedValue == null) {
				throw validationFailed("Answer value is required.");
			}
			if (!normalizedValues.add(normalizedValue)) {
				throw validationFailed("Answer keys must be unique after normalization.");
			}
			SanitizedRichText explanation = command.explanationDocument() == null
					? richTextService.fromPlainText(command.explanation(), false, "Answer explanation")
					: richTextService.fromDocument(command.explanationDocument(), false, "Answer explanation");
			answers.add(new PreparedQuestionAnswer(
					answerValue,
					normalizedValue,
					valueOrDefault(command.primary(), true),
					index + 1,
					explanation));
		}
		return answers;
	}

	private List<PreparedQuestionOption> updateQuestionOptions(
			String nextQuestionType,
			String previousQuestionType,
			Long questionId,
			List<TeacherQuestionOptionCDO> commands
	) {
		if (commands != null) {
			return prepareQuestionOptions(nextQuestionType, commands);
		}
		if (!requiresOptions(nextQuestionType)) {
			return List.of();
		}
		if (!Objects.equals(previousQuestionType, nextQuestionType)) {
			List<QuestionOption> existingOptions = questionOptionRepository.findByQuestionIdOrderByOptionOrderAsc(questionId);
			if (existingOptions.isEmpty()) {
				throw validationFailed("Question type requires at least one option.");
			}
		}
		return null;
	}

	private List<PreparedQuestionAnswer> updateQuestionAnswers(
			String questionType,
			Long questionId,
			List<TeacherQuestionAnswerCDO> commands
	) {
		if (commands != null) {
			return prepareQuestionAnswers(questionType, commands);
		}
		if (OBJECTIVE_QUESTION_TYPES.contains(questionType)
				&& questionAnswerRepository.findByQuestionIdOrderByAnswerOrderAsc(questionId).isEmpty()) {
			throw validationFailed("Objective questions require at least one answer key.");
		}
		return null;
	}

	private void saveQuestionOptions(Question question, List<PreparedQuestionOption> options) {
		for (PreparedQuestionOption option : options) {
			questionOptionRepository.save(QuestionOption.create(
					question,
					option.label(),
					contentText(option.content()),
					contentDocument(option.content()),
					renderedHtml(option.content()),
					option.order()));
		}
	}

	private void saveQuestionAnswers(Question question, List<PreparedQuestionAnswer> answers) {
		for (PreparedQuestionAnswer answer : answers) {
			questionAnswerRepository.save(QuestionAnswer.create(
					question,
					answer.answerValue(),
					answer.normalizedValue(),
					answer.primary(),
					answer.order(),
					contentText(answer.explanation()),
					contentDocument(answer.explanation()),
					renderedHtml(answer.explanation())));
		}
	}

	private void replaceQuestionOptions(Question question, List<PreparedQuestionOption> options) {
		if (options == null) {
			return;
		}
		questionOptionRepository.deleteByQuestionId(question.getId());
		questionOptionRepository.flush();
		saveQuestionOptions(question, options);
	}

	private void replaceQuestionAnswers(Question question, List<PreparedQuestionAnswer> answers) {
		if (answers == null) {
			return;
		}
		questionAnswerRepository.deleteByQuestionId(question.getId());
		questionAnswerRepository.flush();
		saveQuestionAnswers(question, answers);
	}

	private boolean requiresOptions(String questionType) {
		return OPTION_QUESTION_TYPES.contains(questionType);
	}

	private void validateParagraphLabelAvailable(Long sectionId, String label, Long existingParagraphId) {
		if (label == null) {
			return;
		}
		boolean duplicate = existingParagraphId == null
				? readingParagraphRepository.existsBySectionIdAndLabel(sectionId, label)
				: readingParagraphRepository.existsBySectionIdAndLabelAndIdNot(sectionId, label, existingParagraphId);
		if (duplicate) {
			throw validationFailed("Paragraph label must be unique within the section.");
		}
	}

	private String normalizeRelatedParagraphLabel(Long sectionId, String value) {
		String label = trimToNull(value);
		if (label != null && !readingParagraphRepository.existsBySectionIdAndLabel(sectionId, label)) {
			throw validationFailed("Question relatedParagraphLabel must match a paragraph label in the same section.");
		}
		return label;
	}

	private Long normalizeListeningSegmentId(Long sectionId, Long listeningSegmentId) {
		if (listeningSegmentId == null) {
			return null;
		}
		if (!listeningSegmentRepository.existsByIdAndSectionId(listeningSegmentId, sectionId)) {
			throw validationFailed("Question listeningSegmentId must match a listening segment in the same section.");
		}
		return listeningSegmentId;
	}

	private Long normalizeListeningAudioFileId(Long audioFileId, Long ownerUserId) {
		if (audioFileId == null) {
			return null;
		}
		FileMetadata file = fileMetadataRepository.findById(audioFileId)
				.orElseThrow(() -> validationFailed("Section audioFileId must reference a listening audio file."));
		if (!FilePurpose.LISTENING_AUDIO.equals(file.getPurpose())
				|| file.getOwner() == null
				|| !Objects.equals(file.getOwner().getId(), ownerUserId)) {
			throw validationFailed("Section audioFileId must reference a listening audio file.");
		}
		return audioFileId;
	}

	private String normalizeQuestionType(String value) {
		String questionType = requiredText(value, "Question type is required.").toLowerCase(Locale.ROOT);
		if (!SUPPORTED_QUESTION_TYPES.contains(questionType)) {
			throw validationFailed("Unsupported question type.");
		}
		return questionType;
	}

	private void renumberSections(Long testId) {
		List<TestSection> sections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(testId);
		for (int index = 0; index < sections.size(); index++) {
			sections.get(index).setSectionOrder(index + 1);
		}
		testSectionRepository.flush();
	}

	private void renumberParagraphs(Long sectionId) {
		List<ReadingParagraph> paragraphs = readingParagraphRepository.findBySectionIdOrderByParagraphOrderAsc(sectionId);
		for (int index = 0; index < paragraphs.size(); index++) {
			paragraphs.get(index).setParagraphOrder(index + 1);
		}
		readingParagraphRepository.flush();
	}

	private void renumberListeningSegments(Long sectionId) {
		List<ListeningSegment> segments = listeningSegmentRepository.findBySectionIdOrderBySegmentOrderAsc(sectionId);
		for (int index = 0; index < segments.size(); index++) {
			segments.get(index).setSegmentOrder(index + 1);
		}
		listeningSegmentRepository.flush();
	}

	private void renumberQuestions(Long sectionId) {
		List<Question> questions = questionRepository.findBySectionIdOrderByQuestionOrderAsc(sectionId);
		for (int index = 0; index < questions.size(); index++) {
			questions.get(index).setQuestionOrder(index + 1);
		}
		questionRepository.flush();
	}

	private SanitizedRichText sectionRichText(
			JsonNode document,
			String legacyText,
			String currentDocument,
			String currentText,
			String currentRenderedHtml,
			String currentLegacyText,
			boolean required,
			String fieldName
	) {
		if (document != null) {
			return richTextService.fromDocument(document, required, fieldName);
		}
		if (legacyText != null) {
			return richTextService.fromPlainText(legacyText, required, fieldName);
		}
		return richTextService.fromStored(currentDocument, currentText, currentRenderedHtml, currentLegacyText);
	}

	private SanitizedRichText paragraphRichText(TeacherReadingParagraphUDO command, ReadingParagraph paragraph) {
		if (command.contentDocument() != null) {
			return richTextService.fromDocument(command.contentDocument(), true, "Paragraph content");
		}
		if (command.content() != null) {
			return richTextService.fromPlainText(command.content(), true, "Paragraph content");
		}
		return richTextService.fromStored(
				paragraph.getContentDocument(),
				paragraph.getContent(),
				paragraph.getContentRenderedHtml(),
				paragraph.getContent());
	}

	private SanitizedRichText questionTextRichText(TeacherQuestionUDO command, Question question) {
		if (command.questionDocument() != null) {
			return richTextService.fromDocument(command.questionDocument(), true, "Question text");
		}
		if (command.questionText() != null) {
			return richTextService.fromPlainText(command.questionText(), true, "Question text");
		}
		return richTextService.fromStored(
				question.getQuestionDocument(),
				question.getQuestionText(),
				question.getQuestionRenderedHtml(),
				question.getQuestionText());
	}

	private SanitizedRichText questionInstructionsRichText(TeacherQuestionUDO command, Question question) {
		if (command.instructionsDocument() != null) {
			return richTextService.fromDocument(command.instructionsDocument(), false, "Question instructions");
		}
		if (command.instructions() != null) {
			return richTextService.fromPlainText(command.instructions(), false, "Question instructions");
		}
		return richTextService.fromStored(
				question.getInstructionsDocument(),
				question.getInstructionsText(),
				question.getInstructionsRenderedHtml(),
				question.getInstructions());
	}

	private String contentText(SanitizedRichText content) {
		return content == null ? null : content.text();
	}

	private String contentDocument(SanitizedRichText content) {
		return content == null ? null : content.document();
	}

	private String renderedHtml(SanitizedRichText content) {
		return content == null ? null : content.renderedHtml();
	}

	private String normalizeType(String value) {
		return valueOrDefault(trimToNull(value), DEFAULT_TYPE);
	}

	private String normalizeCurrency(String value) {
		return valueOrDefault(trimToNull(value), DEFAULT_CURRENCY).toUpperCase();
	}

	private String normalizeLanguage(String value) {
		return valueOrDefault(trimToNull(value), DEFAULT_LANGUAGE);
	}

	private BigDecimal normalizePrice(BigDecimal value, Boolean free) {
		if (Boolean.TRUE.equals(valueOrDefault(free, true))) {
			return ZERO_PRICE;
		}
		return value == null ? ZERO_PRICE : value;
	}

	private String textOrEmpty(String value) {
		String trimmed = trimToNull(value);
		return trimmed == null ? "" : trimmed;
	}

	private String requiredText(String value, String message) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			throw validationFailed(message);
		}
		return trimmed;
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String searchPattern(String value) {
		String search = trimToNull(value);
		return search == null ? null : "%" + search.toLowerCase(Locale.ROOT) + "%";
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private <T> T valueOrDefault(T value, T defaultValue) {
		return value == null ? defaultValue : value;
	}

	private BusinessException notFound() {
		return new BusinessException("TEST_NOT_FOUND", "Test was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException sectionNotFound() {
		return new BusinessException("TEST_SECTION_NOT_FOUND", "Test section was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException paragraphNotFound() {
		return new BusinessException("READING_PARAGRAPH_NOT_FOUND", "Reading paragraph was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException questionNotFound() {
		return new BusinessException("QUESTION_NOT_FOUND", "Question was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException listeningSegmentNotFound() {
		return new BusinessException("LISTENING_SEGMENT_NOT_FOUND", "Listening segment was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException statusConflict() {
		return new BusinessException(
				"TEACHER_TEST_STATUS_CONFLICT",
				"Test cannot be edited in its current status.",
				HttpStatus.CONFLICT);
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private record OwnedSection(CatalogTest test, TestSection section) {
	}

	private record PreparedQuestionOption(String label, SanitizedRichText content, Integer order) {
	}

	private record PreparedQuestionAnswer(
			String answerValue,
			String normalizedValue,
			Boolean primary,
			Integer order,
			SanitizedRichText explanation
	) {
	}
}
