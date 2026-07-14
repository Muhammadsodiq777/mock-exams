package com.mockexams.backend.service.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.WritingFeedbackRDO;
import com.mockexams.backend.dto.WritingSubmissionCDO;
import com.mockexams.backend.dto.WritingSubmissionRDO;
import com.mockexams.backend.entity.AiFailureCodes;
import com.mockexams.backend.entity.AiFeedback;
import com.mockexams.backend.entity.AiFeedbackTypes;
import com.mockexams.backend.entity.TestAttempt;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.entity.WritingSubmission;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.WritingSubmissionMapper;
import com.mockexams.backend.repository.AiFeedbackRepository;
import com.mockexams.backend.repository.TestAttemptRepository;
import com.mockexams.backend.repository.WritingSubmissionRepository;
import com.mockexams.backend.service.AiUsageAccounting;
import com.mockexams.backend.service.AiUsageRequest;
import com.mockexams.backend.service.AiUsageReservation;
import com.mockexams.backend.service.AiUsageService;
import com.mockexams.backend.service.WritingSubmissionService;
import com.mockexams.backend.service.writing.WritingFeedbackParser;
import com.mockexams.backend.service.writing.WritingFeedbackPayload;
import com.mockexams.backend.service.writing.WritingFeedbackPrompt;
import com.mockexams.backend.service.writing.WritingFeedbackProvider;
import com.mockexams.backend.service.writing.WritingFeedbackProviderException;
import com.mockexams.backend.service.writing.WritingFeedbackProviderResponse;
import com.mockexams.backend.service.writing.WritingFeedbackValidationException;
import com.mockexams.backend.service.writing.WritingRubricService;
import com.mockexams.backend.users.entity.User;

@Service
class WritingSubmissionServiceImpl implements WritingSubmissionService {

	private final WritingSubmissionRepository writingSubmissionRepository;
	private final TestAttemptRepository testAttemptRepository;
	private final AiFeedbackRepository aiFeedbackRepository;
	private final AiUsageService aiUsageService;
	private final WritingRubricService rubricService;
	private final WritingFeedbackProvider provider;
	private final WritingFeedbackParser parser;
	private final WritingSubmissionMapper mapper;

	WritingSubmissionServiceImpl(
			WritingSubmissionRepository writingSubmissionRepository,
			TestAttemptRepository testAttemptRepository,
			AiFeedbackRepository aiFeedbackRepository,
			AiUsageService aiUsageService,
			WritingRubricService rubricService,
			WritingFeedbackProvider provider,
			WritingFeedbackParser parser,
			WritingSubmissionMapper mapper
	) {
		this.writingSubmissionRepository = writingSubmissionRepository;
		this.testAttemptRepository = testAttemptRepository;
		this.aiFeedbackRepository = aiFeedbackRepository;
		this.aiUsageService = aiUsageService;
		this.rubricService = rubricService;
		this.provider = provider;
		this.parser = parser;
		this.mapper = mapper;
	}

	@Override
	@Transactional(noRollbackFor = BusinessException.class)
	public WritingSubmissionRDO submit(User student, WritingSubmissionCDO command) {
		SubmissionInput input = validate(command);
		TestAttempt attempt = input.testAttemptId() == null ? null : requireOwnedAttempt(student, input.testAttemptId());
		WritingSubmission submission = writingSubmissionRepository.save(WritingSubmission.create(
				student,
				attempt,
				input.taskType(),
				input.prompt(),
				input.answerText(),
				wordCount(input.answerText())));
		AiFeedback feedback = processNewFeedback(student, submission, null);
		return mapper.toSubmissionRDO(submission, feedback);
	}

	@Override
	@Transactional(readOnly = true)
	public WritingSubmissionRDO getSubmission(User student, Long submissionId) {
		WritingSubmission submission = requireSubmission(student, submissionId);
		return mapper.toSubmissionRDO(submission, latestFeedback(submission.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public WritingFeedbackRDO getFeedback(User student, Long submissionId) {
		WritingSubmission submission = requireSubmission(student, submissionId);
		AiFeedback feedback = latestFeedback(submission.getId());
		if (feedback == null) {
			throw new BusinessException("AI_FEEDBACK_NOT_FOUND", "Writing feedback was not found.", HttpStatus.NOT_FOUND);
		}
		return mapper.toFeedbackRDO(feedback);
	}

	@Override
	@Transactional(noRollbackFor = BusinessException.class)
	public WritingFeedbackRDO retryFeedback(User student, Long submissionId) {
		WritingSubmission submission = requireSubmission(student, submissionId);
		AiFeedback latest = latestFeedbackForUpdate(submission.getId());
		if (latest == null) {
			return mapper.toFeedbackRDO(processNewFeedback(student, submission, null));
		}
		if (latest.isCompleted() || latest.isActive()) {
			return mapper.toFeedbackRDO(latest);
		}
		return mapper.toFeedbackRDO(processNewFeedback(student, submission, latest));
	}

	private AiFeedback processNewFeedback(User student, WritingSubmission submission, AiFeedback retryOf) {
		AiFeedback completed = completedFeedback(submission.getId());
		if (completed != null) {
			return completed;
		}
		WritingFeedbackPrompt prompt = rubricService.buildPrompt(submission);
		AiUsageReservation reservation = aiUsageService.guardAndReserve(student.getId(),
				new AiUsageRequest(AiFeedbackTypes.WRITING, submission.getId(), null));
		AiFeedback feedback = aiFeedbackRepository.findForUpdateById(reservation.feedbackId())
				.orElseThrow(() -> new BusinessException("AI_FEEDBACK_NOT_FOUND", "AI feedback was not found.",
						HttpStatus.NOT_FOUND));
		feedback.applyPromptAudit(prompt.rubricVersion(), prompt.audit());
		if (retryOf != null) {
			feedback.markRetryOf(retryOf.getId(), retryOf.getRetryCount() + 1);
		}
		try {
			WritingFeedbackProviderResponse response = provider.evaluate(prompt);
			WritingFeedbackPayload payload;
			try {
				payload = parser.parse(submission.getTaskType(), response.content());
			}
			catch (WritingFeedbackValidationException ex) {
				feedback.applyValidationErrors(ex.getValidationErrors());
				aiUsageService.recordValidationFailure(reservation, accounting(response),
						"AI feedback JSON did not match the writing feedback schema.");
				return aiFeedbackRepository.findById(reservation.feedbackId())
						.orElseThrow(() -> new BusinessException("AI_FEEDBACK_NOT_FOUND", "AI feedback was not found.",
								HttpStatus.NOT_FOUND));
			}
			feedback.applyWritingFeedback(
					payload.overallBand(),
					mapper.criteriaToJson(payload.criteriaScores()),
					mapper.mistakesToJson(payload.mistakes()),
					payload.improvedAnswer(),
					mapper.actionPlanToJson(payload.actionPlan()),
					prompt.rubricVersion(),
					prompt.audit());
			aiUsageService.recordSuccess(reservation, accounting(response));
		}
		catch (WritingFeedbackProviderException ex) {
			aiUsageService.recordProviderFailure(reservation, AiFailureCodes.PROVIDER_FAILURE,
					"AI writing feedback provider failed.");
		}
		return aiFeedbackRepository.findById(reservation.feedbackId())
				.orElseThrow(() -> new BusinessException("AI_FEEDBACK_NOT_FOUND", "AI feedback was not found.",
						HttpStatus.NOT_FOUND));
	}

	private AiUsageAccounting accounting(WritingFeedbackProviderResponse response) {
		return new AiUsageAccounting(
				response.provider(),
				response.model(),
				response.promptTokens(),
				response.completionTokens(),
				response.estimatedCost());
	}

	private SubmissionInput validate(WritingSubmissionCDO command) {
		if (command == null) {
			throw validationFailed("Writing submission request is required.");
		}
		String taskType = trim(command.taskType());
		if (!TestTypes.WRITING_TASK_1.equals(taskType) && !TestTypes.WRITING_TASK_2.equals(taskType)) {
			throw new BusinessException("UNSUPPORTED_WRITING_TASK_TYPE", "Unsupported writing task type.",
					HttpStatus.BAD_REQUEST);
		}
		String prompt = trim(command.prompt());
		String answer = trim(command.answerText());
		if (prompt == null) {
			throw validationFailed("Writing prompt is required.");
		}
		if (answer == null) {
			throw validationFailed("Writing answer is required.");
		}
		return new SubmissionInput(taskType, command.testAttemptId(), prompt, answer);
	}

	private TestAttempt requireOwnedAttempt(User student, Long attemptId) {
		return testAttemptRepository.findByIdAndStudentIdForUpdate(attemptId, student.getId())
				.orElseThrow(() -> new BusinessException("ATTEMPT_NOT_FOUND", "Attempt was not found.",
						HttpStatus.NOT_FOUND));
	}

	private WritingSubmission requireSubmission(User student, Long submissionId) {
		return writingSubmissionRepository.findByIdAndStudentUserId(submissionId, student.getId())
				.orElseThrow(() -> new BusinessException("WRITING_SUBMISSION_NOT_FOUND",
						"Writing submission was not found.", HttpStatus.NOT_FOUND));
	}

	private AiFeedback latestFeedback(Long submissionId) {
		List<AiFeedback> feedbacks = aiFeedbackRepository
				.findByWritingSubmissionIdAndFeedbackTypeOrderByCreatedAtDescIdDesc(submissionId, AiFeedbackTypes.WRITING);
		return feedbacks.isEmpty() ? null : feedbacks.getFirst();
	}

	private AiFeedback latestFeedbackForUpdate(Long submissionId) {
		List<AiFeedback> feedbacks = aiFeedbackRepository
				.findForUpdateByWritingSubmissionIdAndFeedbackTypeOrderByCreatedAtDescIdDesc(
						submissionId,
						AiFeedbackTypes.WRITING);
		return feedbacks.isEmpty() ? null : feedbacks.getFirst();
	}

	private AiFeedback completedFeedback(Long submissionId) {
		return aiFeedbackRepository
				.findByWritingSubmissionIdAndFeedbackTypeOrderByCreatedAtDescIdDesc(submissionId, AiFeedbackTypes.WRITING)
				.stream()
				.filter(AiFeedback::isCompleted)
				.findFirst()
				.orElse(null);
	}

	private int wordCount(String answerText) {
		return answerText.trim().split("\\s+").length;
	}

	private String trim(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private record SubmissionInput(String taskType, Long testAttemptId, String prompt, String answerText) {
	}
}
