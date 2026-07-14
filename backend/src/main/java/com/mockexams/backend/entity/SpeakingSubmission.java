package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.mockexams.backend.users.entity.User;

@Entity
@Table(name = "speaking_submissions")
public class SpeakingSubmission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_user_id", nullable = false)
	private User studentUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "test_attempt_id")
	private TestAttempt testAttempt;

	@Column(nullable = false, length = 32)
	private String partType;

	@Column(nullable = false)
	private String prompt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "audio_file_id", nullable = false)
	private FileMetadata audioFile;

	private String transcript;

	private Integer durationSeconds;

	@Column(nullable = false, length = 24)
	private String transcriptionStatus = TranscriptionStatuses.QUEUED;

	@Column(length = 80)
	private String transcriptionProvider;

	@Column(length = 120)
	private String transcriptionModel;

	@Column(length = 64)
	private String transcriptionFailureCode;

	private String transcriptionErrorMessage;

	@Column(nullable = false, precision = 12, scale = 6)
	private BigDecimal transcriptionEstimatedCost = BigDecimal.ZERO;

	private OffsetDateTime transcriptionStartedAt;

	private OffsetDateTime transcriptionCompletedAt;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	protected SpeakingSubmission() {
	}

	private SpeakingSubmission(
			User studentUser,
			TestAttempt testAttempt,
			String partType,
			String prompt,
			FileMetadata audioFile,
			Integer durationSeconds
	) {
		this.studentUser = studentUser;
		this.testAttempt = testAttempt;
		this.partType = partType;
		this.prompt = prompt;
		this.audioFile = audioFile;
		this.durationSeconds = durationSeconds;
	}

	public static SpeakingSubmission create(
			User studentUser,
			TestAttempt testAttempt,
			String partType,
			String prompt,
			FileMetadata audioFile,
			Integer durationSeconds
	) {
		return new SpeakingSubmission(studentUser, testAttempt, partType, prompt, audioFile, durationSeconds);
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now(ZoneOffset.UTC);
		}
	}

	public void markProcessing(OffsetDateTime now) {
		this.transcriptionStatus = TranscriptionStatuses.PROCESSING;
		this.transcriptionStartedAt = now;
		this.transcriptionCompletedAt = null;
		this.transcriptionFailureCode = null;
		this.transcriptionErrorMessage = null;
	}

	public void markCompleted(String transcript, String provider, String model, BigDecimal estimatedCost,
			OffsetDateTime now) {
		this.transcriptionStatus = TranscriptionStatuses.COMPLETED;
		this.transcript = transcript;
		this.transcriptionProvider = provider;
		this.transcriptionModel = model;
		this.transcriptionFailureCode = null;
		this.transcriptionErrorMessage = null;
		this.transcriptionEstimatedCost = estimatedCost == null ? BigDecimal.ZERO : estimatedCost;
		this.transcriptionCompletedAt = now;
	}

	public void markFailed(String failureCode, String errorMessage, String provider, String model, OffsetDateTime now) {
		this.transcriptionStatus = TranscriptionStatuses.FAILED;
		this.transcriptionFailureCode = failureCode;
		this.transcriptionErrorMessage = errorMessage;
		this.transcriptionProvider = provider;
		this.transcriptionModel = model;
		this.transcriptionCompletedAt = now;
	}

	public boolean isCompleted() {
		return TranscriptionStatuses.COMPLETED.equals(transcriptionStatus);
	}

	public boolean isActive() {
		return TranscriptionStatuses.isActive(transcriptionStatus);
	}

	public Long getId() {
		return id;
	}

	public User getStudentUser() {
		return studentUser;
	}

	public TestAttempt getTestAttempt() {
		return testAttempt;
	}

	public String getPartType() {
		return partType;
	}

	public String getPrompt() {
		return prompt;
	}

	public FileMetadata getAudioFile() {
		return audioFile;
	}

	public String getTranscript() {
		return transcript;
	}

	public Integer getDurationSeconds() {
		return durationSeconds;
	}

	public String getTranscriptionStatus() {
		return transcriptionStatus;
	}

	public String getTranscriptionProvider() {
		return transcriptionProvider;
	}

	public String getTranscriptionModel() {
		return transcriptionModel;
	}

	public String getTranscriptionFailureCode() {
		return transcriptionFailureCode;
	}

	public String getTranscriptionErrorMessage() {
		return transcriptionErrorMessage;
	}

	public BigDecimal getTranscriptionEstimatedCost() {
		return transcriptionEstimatedCost;
	}

	public OffsetDateTime getTranscriptionStartedAt() {
		return transcriptionStartedAt;
	}

	public OffsetDateTime getTranscriptionCompletedAt() {
		return transcriptionCompletedAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
