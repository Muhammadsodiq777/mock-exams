package com.mockexams.backend.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.AdminModerationQueueMetaRDO;
import com.mockexams.backend.dto.AdminModerationTestDetailRDO;
import com.mockexams.backend.dto.AdminModerationTestRDO;
import com.mockexams.backend.dto.TeacherTestRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.AdminModerationMapper;
import com.mockexams.backend.repository.TestCatalogRepository;
import com.mockexams.backend.service.AdminModerationSearchCriteria;
import com.mockexams.backend.service.NotificationService;
import com.mockexams.backend.service.TestModerationService;
import com.mockexams.backend.users.entity.User;

@Service
class TestModerationServiceImpl implements TestModerationService {

	private final TestCatalogRepository testCatalogRepository;
	private final TeacherTestBuilderServiceImpl teacherTestBuilderServiceImpl;
	private final NotificationService notificationService;
	private final AdminModerationMapper adminModerationMapper;

	TestModerationServiceImpl(
			TestCatalogRepository testCatalogRepository,
			TeacherTestBuilderServiceImpl teacherTestBuilderServiceImpl,
			NotificationService notificationService,
			AdminModerationMapper adminModerationMapper
	) {
		this.testCatalogRepository = testCatalogRepository;
		this.teacherTestBuilderServiceImpl = teacherTestBuilderServiceImpl;
		this.notificationService = notificationService;
		this.adminModerationMapper = adminModerationMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AdminModerationTestRDO> queue(AdminModerationSearchCriteria criteria, Pageable pageable) {
		return testCatalogRepository.adminModerationQueue(
						criteria.type(),
						criteria.teacherProfileId(),
						criteria.difficulty(),
						criteria.submittedFrom(),
						criteria.submittedTo(),
						searchPattern(criteria.search()),
						pageable)
				.map(adminModerationMapper::toQueueRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminModerationQueueMetaRDO queueMeta() {
		long pendingCount = testCatalogRepository.countByVisibilityAndDeletedAtIsNull(TestVisibilities.SUBMITTED);
		OffsetDateTime oldestSubmittedAt = testCatalogRepository
				.findTopByVisibilityAndDeletedAtIsNullOrderByUpdatedAtAsc(TestVisibilities.SUBMITTED)
				.map(CatalogTest::getUpdatedAt)
				.orElse(null);
		return new AdminModerationQueueMetaRDO(pendingCount, oldestSubmittedAt);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminModerationTestDetailRDO preview(Long testId) {
		CatalogTest test = requireTest(testId);
		return toDetailRDO(test);
	}

	@Override
	@Transactional
	public AdminModerationTestDetailRDO approve(User admin, Long testId) {
		CatalogTest test = requireTest(testId);
		requireSubmitted(test);
		Map<String, String> failures = teacherTestBuilderServiceImpl.collectSubmissionFailures(test);
		if (!failures.isEmpty()) {
			throw new BusinessException(
					"TEST_MODERATION_NOT_READY",
					"Test content is no longer ready for publishing.",
					HttpStatus.BAD_REQUEST);
		}
		test.approve(admin.getId(), now());
		notificationService.notifyTestModerationDecision(
				ownerUserId(test),
				test.getId(),
				test.getTitle(),
				TestVisibilities.PUBLISHED,
				null);
		TeacherTestRDO teacherTestRDO = teacherTestBuilderServiceImpl.readModelFor(test);
		return new AdminModerationTestDetailRDO(teacherTestRDO, true, failures);
	}

	@Override
	@Transactional
	public AdminModerationTestDetailRDO reject(User admin, Long testId, String reason) {
		String normalizedReason = trimToNull(reason);
		if (normalizedReason == null) {
			throw validationFailed("Rejection reason is required.");
		}
		CatalogTest test = requireTest(testId);
		requireSubmitted(test);
		test.reject(admin.getId(), now(), normalizedReason);
		notificationService.notifyTestModerationDecision(
				ownerUserId(test),
				test.getId(),
				test.getTitle(),
				TestVisibilities.REJECTED,
				normalizedReason);
		return toDetailRDO(test);
	}

	private AdminModerationTestDetailRDO toDetailRDO(CatalogTest test) {
		Map<String, String> failures = teacherTestBuilderServiceImpl.collectSubmissionFailures(test);
		TeacherTestRDO teacherTestRDO = teacherTestBuilderServiceImpl.readModelFor(test);
		return new AdminModerationTestDetailRDO(teacherTestRDO, failures.isEmpty(), failures);
	}

	private Long ownerUserId(CatalogTest test) {
		if (test.getTeacherProfile() != null && test.getTeacherProfile().getUser() != null) {
			return test.getTeacherProfile().getUser().getId();
		}
		return test.getCreatedByUserId();
	}

	private CatalogTest requireTest(Long testId) {
		return testCatalogRepository.findById(testId).orElseThrow(this::notFound);
	}

	private void requireSubmitted(CatalogTest test) {
		if (!TestVisibilities.SUBMITTED.equals(test.getVisibility())) {
			throw statusConflict();
		}
	}

	private String searchPattern(String search) {
		String trimmed = trimToNull(search);
		return trimmed == null ? null : "%" + trimmed.toLowerCase(Locale.ROOT) + "%";
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

	private BusinessException notFound() {
		return new BusinessException("TEST_NOT_FOUND", "Test was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException statusConflict() {
		return new BusinessException(
				"TEST_MODERATION_STATUS_CONFLICT",
				"Only submitted tests can be approved or rejected.",
				HttpStatus.CONFLICT);
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}
}
