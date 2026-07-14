package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.config.BackendEnvironmentProperties;
import com.mockexams.backend.dto.SpeakingFeedbackRDO;
import com.mockexams.backend.dto.SpeakingSubmissionCDO;
import com.mockexams.backend.dto.SpeakingSubmissionRDO;
import com.mockexams.backend.entity.AiFailureCodes;
import com.mockexams.backend.entity.AiFeedback;
import com.mockexams.backend.entity.AiFeedbackTypes;
import com.mockexams.backend.entity.FileMetadata;
import com.mockexams.backend.entity.FilePurpose;
import com.mockexams.backend.entity.SpeakingSubmission;
import com.mockexams.backend.entity.TestAttempt;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.SpeakingSubmissionMapper;
import com.mockexams.backend.repository.AiFeedbackRepository;
import com.mockexams.backend.repository.FileMetadataRepository;
import com.mockexams.backend.repository.SpeakingSubmissionRepository;
import com.mockexams.backend.repository.TestAttemptRepository;
import com.mockexams.backend.service.AiUsageAccounting;
import com.mockexams.backend.service.AiUsageRequest;
import com.mockexams.backend.service.AiUsageReservation;
import com.mockexams.backend.service.AiUsageService;
import com.mockexams.backend.service.FileStorageService;
import com.mockexams.backend.service.SpeakingSubmissionService;
import com.mockexams.backend.service.StoredFileContent;
import com.mockexams.backend.service.TranscriptionProviderClient;
import com.mockexams.backend.service.TranscriptionProviderException;
import com.mockexams.backend.service.TranscriptionRequest;
import com.mockexams.backend.service.TranscriptionResult;
import com.mockexams.backend.service.speaking.SpeakingFeedbackParser;
import com.mockexams.backend.service.speaking.SpeakingFeedbackPayload;
import com.mockexams.backend.service.speaking.SpeakingFeedbackPrompt;
import com.mockexams.backend.service.speaking.SpeakingFeedbackProvider;
import com.mockexams.backend.service.speaking.SpeakingFeedbackProviderException;
import com.mockexams.backend.service.speaking.SpeakingFeedbackProviderResponse;
import com.mockexams.backend.service.speaking.SpeakingFeedbackValidationException;
import com.mockexams.backend.service.speaking.SpeakingRubricService;
import com.mockexams.backend.users.entity.User;

@Service
class SpeakingSubmissionServiceImpl implements SpeakingSubmissionService {

	private static final Set<String> SPEAKING_PART_TYPES = Set.of(
			TestTypes.SPEAKING_PART_1,
			TestTypes.SPEAKING_PART_2,
			TestTypes.SPEAKING_PART_3);
	private static final BigDecimal SECONDS_PER_MINUTE = new BigDecimal("60");

	private final SpeakingSubmissionRepository speakingSubmissionRepository;
	private final AiFeedbackRepository aiFeedbackRepository;
	private final FileMetadataRepository fileMetadataRepository;
	private final TestAttemptRepository testAttemptRepository;
	private final FileStorageService fileStorageService;
	private final TranscriptionProviderClient transcriptionProviderClient;
	private final AiUsageService aiUsageService;
	private final SpeakingRubricService rubricService;
	private final SpeakingFeedbackProvider feedbackProvider;
	private final SpeakingFeedbackParser feedbackParser;
	private final SpeakingSubmissionMapper speakingSubmissionMapper;
	private final BackendEnvironmentProperties properties;

	SpeakingSubmissionServiceImpl(
			SpeakingSubmissionRepository speakingSubmissionRepository,
			AiFeedbackRepository aiFeedbackRepository,
			FileMetadataRepository fileMetadataRepository,
			TestAttemptRepository testAttemptRepository,
			FileStorageService fileStorageService,
			TranscriptionProviderClient transcriptionProviderClient,
			AiUsageService aiUsageService,
			SpeakingRubricService rubricService,
			SpeakingFeedbackProvider feedbackProvider,
			SpeakingFeedbackParser feedbackParser,
			SpeakingSubmissionMapper speakingSubmissionMapper,
			BackendEnvironmentProperties properties
	) {
		this.speakingSubmissionRepository = speakingSubmissionRepository;
		this.aiFeedbackRepository = aiFeedbackRepository;
		this.fileMetadataRepository = fileMetadataRepository;
		this.testAttemptRepository = testAttemptRepository;
		this.fileStorageService = fileStorageService;
		this.transcriptionProviderClient = transcriptionProviderClient;
		this.aiUsageService = aiUsageService;
		this.rubricService = rubricService;
		this.feedbackProvider = feedbackProvider;
		this.feedbackParser = feedbackParser;
		this.speakingSubmissionMapper = speakingSubmissionMapper;
		this.properties = properties;
	}

	@Override
	@Transactional
	public SubmissionResult submit(User student, SpeakingSubmissionCDO request) {
		SubmissionPayload payload = normalize(request);
		FileMetadata audioFile = requireOwnedSpeakingAudio(student, payload.audioFileId());
		Integer effectiveDurationSeconds = effectiveDurationSeconds(audioFile, payload.durationSeconds());
		validateDuration(effectiveDurationSeconds);
		TestAttempt testAttempt = payload.testAttemptId() == null
				? null
				: requireOwnedAttempt(student, payload.testAttemptId());
		SpeakingSubmission existing = speakingSubmissionRepository.findMatchingSubmission(
						student.getId(),
						audioFile.getId(),
						payload.partType(),
						payload.prompt(),
						payload.testAttemptId(),
						PageRequest.of(0, 1))
				.stream()
				.findFirst()
				.orElse(null);
		if (existing != null) {
			return new SubmissionResult(speakingSubmissionMapper.toRDO(existing), false);
		}

		SpeakingSubmission submission = speakingSubmissionRepository.save(SpeakingSubmission.create(
				student,
				testAttempt,
				payload.partType(),
				payload.prompt(),
				audioFile,
				effectiveDurationSeconds));
		return new SubmissionResult(speakingSubmissionMapper.toRDO(processTranscription(submission)), true);
	}

	@Override
	@Transactional(readOnly = true)
	public SpeakingSubmissionRDO get(User student, Long submissionId) {
		return speakingSubmissionRepository.findByIdAndStudentUserId(submissionId, student.getId())
				.map(speakingSubmissionMapper::toRDO)
				.orElseThrow(this::notFound);
	}

	@Override
	@Transactional
	public SpeakingSubmissionRDO startTranscription(User student, Long submissionId) {
		SpeakingSubmission submission = speakingSubmissionRepository
				.findByIdAndStudentUserIdForUpdate(submissionId, student.getId())
				.orElseThrow(this::notFound);
		if (submission.isActive() || submission.isCompleted()) {
			return speakingSubmissionMapper.toRDO(submission);
		}
		return speakingSubmissionMapper.toRDO(processTranscription(submission));
	}

	@Override
	@Transactional(readOnly = true)
	public SpeakingFeedbackRDO getFeedback(User student, Long submissionId) {
		SpeakingSubmission submission = requireSubmission(student, submissionId);
		AiFeedback feedback = latestFeedback(submission.getId());
		if (feedback == null) {
			throw new BusinessException("AI_FEEDBACK_NOT_FOUND", "Speaking feedback was not found.", HttpStatus.NOT_FOUND);
		}
		return speakingSubmissionMapper.toFeedbackRDO(feedback);
	}

	@Override
	@Transactional(noRollbackFor = BusinessException.class)
	public SpeakingFeedbackRDO startFeedback(User student, Long submissionId) {
		SpeakingSubmission submission = requireSubmission(student, submissionId);
		requireCompletedTranscript(submission);
		AiFeedback latest = latestFeedbackForUpdate(submission.getId());
		if (latest == null) {
			return speakingSubmissionMapper.toFeedbackRDO(processNewFeedback(student, submission, null));
		}
		if (latest.isCompleted() || latest.isActive()) {
			return speakingSubmissionMapper.toFeedbackRDO(latest);
		}
		return speakingSubmissionMapper.toFeedbackRDO(processNewFeedback(student, submission, latest));
	}

	@Override
	@Transactional(noRollbackFor = BusinessException.class)
	public SpeakingFeedbackRDO retryFeedback(User student, Long submissionId) {
		SpeakingSubmission submission = requireSubmission(student, submissionId);
		requireCompletedTranscript(submission);
		AiFeedback latest = latestFeedbackForUpdate(submission.getId());
		if (latest == null) {
			return speakingSubmissionMapper.toFeedbackRDO(processNewFeedback(student, submission, null));
		}
		if (latest.isCompleted() || latest.isActive()) {
			return speakingSubmissionMapper.toFeedbackRDO(latest);
		}
		return speakingSubmissionMapper.toFeedbackRDO(processNewFeedback(student, submission, latest));
	}

	private SpeakingSubmission processTranscription(SpeakingSubmission submission) {
		OffsetDateTime startedAt = now();
		submission.markProcessing(startedAt);
		speakingSubmissionRepository.save(submission);
		try {
			StoredFileContent audio = fileStorageService.read(submission.getAudioFile());
			TranscriptionResult result = transcriptionProviderClient.transcribe(new TranscriptionRequest(
					audio.filename(),
					audio.contentType(),
					audio.bytes(),
					submission.getPrompt()));
			String transcript = result.transcript() == null ? "" : result.transcript().trim();
			if (transcript.isBlank()) {
				throw new TranscriptionProviderException(AiFailureCodes.PROVIDER_FAILURE,
						"Transcription provider returned an empty transcript.");
			}
			submission.markCompleted(
					transcript,
					result.provider(),
					result.model(),
					estimatedCost(submission.getDurationSeconds(), result.estimatedCost()),
					now());
		}
		catch (TranscriptionProviderException ex) {
			submission.markFailed(
					failureCode(ex.getFailureCode()),
					ex.getMessage(),
					providerName(),
					properties.transcription().model(),
					now());
		}
		catch (RuntimeException ex) {
			submission.markFailed(
					AiFailureCodes.PROVIDER_FAILURE,
					"Transcription failed before a transcript was produced.",
					providerName(),
					properties.transcription().model(),
					now());
		}
		return speakingSubmissionRepository.save(submission);
	}

	private AiFeedback processNewFeedback(User student, SpeakingSubmission submission, AiFeedback retryOf) {
		AiFeedback completed = completedFeedback(submission.getId());
		if (completed != null) {
			return completed;
		}
		SpeakingFeedbackPrompt prompt = rubricService.buildPrompt(submission);
		AiUsageReservation reservation = aiUsageService.guardAndReserve(student.getId(),
				new AiUsageRequest(AiFeedbackTypes.SPEAKING, null, submission.getId()));
		AiFeedback feedback = aiFeedbackRepository.findForUpdateById(reservation.feedbackId())
				.orElseThrow(() -> new BusinessException("AI_FEEDBACK_NOT_FOUND", "AI feedback was not found.",
						HttpStatus.NOT_FOUND));
		feedback.applyPromptAudit(prompt.rubricVersion(), prompt.audit());
		if (retryOf != null) {
			feedback.markRetryOf(retryOf.getId(), retryOf.getRetryCount() + 1);
		}
		try {
			SpeakingFeedbackProviderResponse response = feedbackProvider.evaluate(prompt);
			SpeakingFeedbackPayload payload;
			try {
				payload = feedbackParser.parse(submission.getPartType(), response.content());
			}
			catch (SpeakingFeedbackValidationException ex) {
				feedback.applyValidationErrors(ex.getValidationErrors());
				aiUsageService.recordValidationFailure(reservation, accounting(response),
						"AI feedback JSON did not match the speaking feedback schema.");
				return aiFeedbackRepository.findById(reservation.feedbackId())
						.orElseThrow(() -> new BusinessException("AI_FEEDBACK_NOT_FOUND", "AI feedback was not found.",
								HttpStatus.NOT_FOUND));
			}
			feedback.applySpeakingFeedback(
					payload.overallBand(),
					speakingSubmissionMapper.criteriaToJson(payload.criteriaScores()),
					speakingSubmissionMapper.repeatedMistakesToJson(payload.repeatedMistakes()),
					payload.betterAnswerExample(),
					speakingSubmissionMapper.actionPlanToJson(payload.actionPlan()),
					prompt.rubricVersion(),
					prompt.audit());
			aiUsageService.recordSuccess(reservation, accounting(response));
		}
		catch (SpeakingFeedbackProviderException ex) {
			aiUsageService.recordProviderFailure(reservation, AiFailureCodes.PROVIDER_FAILURE,
					"AI speaking feedback provider failed.");
		}
		return aiFeedbackRepository.findById(reservation.feedbackId())
				.orElseThrow(() -> new BusinessException("AI_FEEDBACK_NOT_FOUND", "AI feedback was not found.",
						HttpStatus.NOT_FOUND));
	}

	private AiUsageAccounting accounting(SpeakingFeedbackProviderResponse response) {
		return new AiUsageAccounting(
				response.provider(),
				response.model(),
				response.promptTokens(),
				response.completionTokens(),
				response.estimatedCost());
	}

	private SubmissionPayload normalize(SpeakingSubmissionCDO request) {
		if (request == null) {
			throw validationFailed("Speaking submission request is required.");
		}
		String partType = trimToNull(request.partType());
		if (!SPEAKING_PART_TYPES.contains(partType)) {
			throw validationFailed("Speaking part type is not supported.");
		}
		String prompt = trimToNull(request.prompt());
		if (prompt == null) {
			throw validationFailed("Speaking prompt is required.");
		}
		if (request.audioFileId() == null) {
			throw validationFailed("Audio file id is required.");
		}
		if (request.durationSeconds() == null || request.durationSeconds() <= 0) {
			throw validationFailed("Audio duration is required.");
		}
		return new SubmissionPayload(partType, prompt, request.audioFileId(), request.testAttemptId(),
				request.durationSeconds());
	}

	private FileMetadata requireOwnedSpeakingAudio(User student, Long audioFileId) {
		FileMetadata audioFile = fileMetadataRepository.findById(audioFileId)
				.orElseThrow(this::fileNotFound);
		if (!isOwner(student, audioFile)) {
			throw fileNotFound();
		}
		if (!FilePurpose.SPEAKING_AUDIO.equals(audioFile.getPurpose())) {
			throw validationFailed("Audio file must be uploaded for speaking audio.");
		}
		return audioFile;
	}

	private TestAttempt requireOwnedAttempt(User student, Long testAttemptId) {
		return testAttemptRepository.findByIdAndStudentIdForUpdate(testAttemptId, student.getId())
				.orElseThrow(() -> new BusinessException("ATTEMPT_NOT_FOUND", "Attempt was not found.",
						HttpStatus.NOT_FOUND));
	}

	private SpeakingSubmission requireSubmission(User student, Long submissionId) {
		return speakingSubmissionRepository.findByIdAndStudentUserId(submissionId, student.getId())
				.orElseThrow(this::notFound);
	}

	private void requireCompletedTranscript(SpeakingSubmission submission) {
		if (!submission.isCompleted() || submission.getTranscript() == null || submission.getTranscript().isBlank()) {
			throw new BusinessException("TRANSCRIPT_REQUIRED",
					"Speaking feedback requires a completed transcript before evaluation.", HttpStatus.CONFLICT);
		}
	}

	private AiFeedback latestFeedback(Long submissionId) {
		List<AiFeedback> feedbacks = aiFeedbackRepository
				.findBySpeakingSubmissionIdAndFeedbackTypeOrderByCreatedAtDescIdDesc(submissionId, AiFeedbackTypes.SPEAKING);
		return feedbacks.isEmpty() ? null : feedbacks.getFirst();
	}

	private AiFeedback latestFeedbackForUpdate(Long submissionId) {
		List<AiFeedback> feedbacks = aiFeedbackRepository
				.findForUpdateBySpeakingSubmissionIdAndFeedbackTypeOrderByCreatedAtDescIdDesc(
						submissionId,
						AiFeedbackTypes.SPEAKING);
		return feedbacks.isEmpty() ? null : feedbacks.getFirst();
	}

	private AiFeedback completedFeedback(Long submissionId) {
		return aiFeedbackRepository
				.findBySpeakingSubmissionIdAndFeedbackTypeOrderByCreatedAtDescIdDesc(submissionId, AiFeedbackTypes.SPEAKING)
				.stream()
				.filter(AiFeedback::isCompleted)
				.findFirst()
				.orElse(null);
	}

	private void validateDuration(Integer durationSeconds) {
		Duration maxDuration = properties.transcription().maxAudioDuration();
		if (durationSeconds > maxDuration.toSeconds()) {
			throw validationFailed("Audio duration exceeds the maximum allowed transcription duration.");
		}
	}

	private Integer effectiveDurationSeconds(FileMetadata audioFile, Integer requestedDurationSeconds) {
		Integer storedDurationSeconds = audioFile.getDurationSeconds();
		if (storedDurationSeconds != null && storedDurationSeconds > 0) {
			return storedDurationSeconds;
		}
		return requestedDurationSeconds;
	}

	private BigDecimal estimatedCost(Integer durationSeconds, BigDecimal providerCost) {
		if (providerCost != null && providerCost.compareTo(BigDecimal.ZERO) > 0) {
			return providerCost.setScale(6, RoundingMode.HALF_UP);
		}
		return properties.transcription().costPerMinute()
				.multiply(BigDecimal.valueOf(durationSeconds))
				.divide(SECONDS_PER_MINUTE, 6, RoundingMode.HALF_UP);
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

	private String failureCode(String failureCode) {
		return failureCode == null || failureCode.isBlank() ? AiFailureCodes.PROVIDER_FAILURE : failureCode;
	}

	private String providerName() {
		return properties.transcription().provider();
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private BusinessException notFound() {
		return new BusinessException("NOT_FOUND", "Speaking submission was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException fileNotFound() {
		return new BusinessException("NOT_FOUND", "File was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private record SubmissionPayload(
			String partType,
			String prompt,
			Long audioFileId,
			Long testAttemptId,
			Integer durationSeconds
	) {
	}
}
