package com.mockexams.backend.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.TestCardRDO;
import com.mockexams.backend.dto.TestCategoryRDO;
import com.mockexams.backend.dto.TestDetailRDO;
import com.mockexams.backend.dto.TestTagRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.ListeningSegment;
import com.mockexams.backend.entity.Question;
import com.mockexams.backend.entity.QuestionOption;
import com.mockexams.backend.entity.ReadingParagraph;
import com.mockexams.backend.entity.TestSection;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.TestCatalogMapper;
import com.mockexams.backend.repository.ListeningSegmentRepository;
import com.mockexams.backend.repository.QuestionOptionRepository;
import com.mockexams.backend.repository.QuestionRepository;
import com.mockexams.backend.repository.ReadingParagraphRepository;
import com.mockexams.backend.repository.TestCatalogRepository;
import com.mockexams.backend.repository.TestCatalogSpecifications;
import com.mockexams.backend.repository.TestCategoryRepository;
import com.mockexams.backend.repository.TestTagRepository;
import com.mockexams.backend.repository.TestSectionRepository;
import com.mockexams.backend.service.TestCatalogSearchCriteria;
import com.mockexams.backend.service.TestCatalogService;

@Service
class TestCatalogServiceImpl implements TestCatalogService {

	private final TestCatalogRepository testCatalogRepository;
	private final TestCategoryRepository testCategoryRepository;
	private final TestTagRepository testTagRepository;
	private final TestSectionRepository testSectionRepository;
	private final ReadingParagraphRepository readingParagraphRepository;
	private final ListeningSegmentRepository listeningSegmentRepository;
	private final QuestionRepository questionRepository;
	private final QuestionOptionRepository questionOptionRepository;
	private final TestCatalogMapper testCatalogMapper;

	TestCatalogServiceImpl(
			TestCatalogRepository testCatalogRepository,
			TestCategoryRepository testCategoryRepository,
			TestTagRepository testTagRepository,
			TestSectionRepository testSectionRepository,
			ReadingParagraphRepository readingParagraphRepository,
			ListeningSegmentRepository listeningSegmentRepository,
			QuestionRepository questionRepository,
			QuestionOptionRepository questionOptionRepository,
			TestCatalogMapper testCatalogMapper
	) {
		this.testCatalogRepository = testCatalogRepository;
		this.testCategoryRepository = testCategoryRepository;
		this.testTagRepository = testTagRepository;
		this.testSectionRepository = testSectionRepository;
		this.readingParagraphRepository = readingParagraphRepository;
		this.listeningSegmentRepository = listeningSegmentRepository;
		this.questionRepository = questionRepository;
		this.questionOptionRepository = questionOptionRepository;
		this.testCatalogMapper = testCatalogMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TestCardRDO> listTests(TestCatalogSearchCriteria criteria, Pageable pageable) {
		Page<CatalogTest> tests = testCatalogRepository.findAll(
				TestCatalogSpecifications.publicCatalog(criteria),
				pageable);
		if (tests.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, tests.getTotalElements());
		}
		List<Long> testIds = tests.getContent().stream()
				.map(CatalogTest::getId)
				.toList();
		Map<Long, CatalogTest> readModels = testCatalogRepository.findReadModelsByIdIn(testIds)
				.stream()
				.collect(Collectors.toMap(CatalogTest::getId, Function.identity()));
		List<TestCardRDO> content = testIds.stream()
				.map(readModels::get)
				.filter(Objects::nonNull)
				.map(testCatalogMapper::toCardRDO)
				.toList();
		return new PageImpl<>(content, pageable, tests.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public TestDetailRDO testDetail(Long testId) {
		return testCatalogRepository.findVisibleDetailById(testId)
				.map(this::toDetailReadModel)
				.orElseThrow(this::notFound);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TestCategoryRDO> listCategories() {
		Sort sort = Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.ASC, "name"));
		return testCategoryRepository.findByActiveTrue(sort).stream()
				.map(testCatalogMapper::toCategoryRDO)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TestTagRDO> listTags() {
		return testTagRepository.findAllBy(Sort.by(Sort.Direction.ASC, "name")).stream()
				.map(testCatalogMapper::toTagRDO)
				.toList();
	}

	private TestDetailRDO toDetailReadModel(CatalogTest test) {
		List<TestSection> sections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(test.getId());
		List<Long> sectionIds = sections.stream().map(TestSection::getId).toList();
		Map<Long, List<ReadingParagraph>> paragraphsBySectionId = sectionIds.isEmpty()
				? Map.of()
				: readingParagraphRepository.findBySectionIds(sectionIds).stream()
						.collect(Collectors.groupingBy(
								paragraph -> paragraph.getSection().getId(),
								Collectors.toList()));
		Map<Long, List<ListeningSegment>> segmentsBySectionId = sectionIds.isEmpty()
				? Map.of()
				: listeningSegmentRepository.findBySectionIds(sectionIds).stream()
						.collect(Collectors.groupingBy(
								segment -> segment.getSection().getId(),
								Collectors.toList()));
		Map<Long, List<Question>> questionsBySectionId = sectionIds.isEmpty()
				? Map.of()
				: questionRepository.findBySectionIds(sectionIds).stream()
						.collect(Collectors.groupingBy(
								question -> question.getSection().getId(),
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
								Collectors.toList()));
		return testCatalogMapper.toDetailRDO(
				test,
				sections,
				paragraphsBySectionId,
				segmentsBySectionId,
				questionsBySectionId,
				optionsByQuestionId);
	}

	private BusinessException notFound() {
		return new BusinessException("TEST_NOT_FOUND", "Test was not found.", HttpStatus.NOT_FOUND);
	}
}
