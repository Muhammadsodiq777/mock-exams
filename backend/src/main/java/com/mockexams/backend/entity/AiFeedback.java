package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "ai_feedbacks")
public class AiFeedback {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_user_id", nullable = false)
	private User studentUser;

	@Column(nullable = false, length = 24)
	private String feedbackType;

	private Long writingSubmissionId;

	private Long speakingSubmissionId;

	@Column(nullable = false, length = 24)
	private String status = AiFeedbackStatuses.QUEUED;

	@Column(length = 80)
	private String provider;

	@Column(length = 120)
	private String model;

	@Column(nullable = false)
	private int promptTokens;

	@Column(nullable = false)
	private int completionTokens;

	@Column(nullable = false, precision = 12, scale = 6)
	private BigDecimal estimatedCost = BigDecimal.ZERO;

	@Column(precision = 2, scale = 1)
	private BigDecimal overallBand;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private Map<String, Object> criteriaScores = new LinkedHashMap<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private List<Object> mistakes = List.of();

	@Column(columnDefinition = "text")
	private String improvedAnswer;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private List<Object> actionPlan = List.of();

	@Column(length = 64)
	private String failureCode;

	private String errorMessage;

	@Column(length = 80)
	private String rubricVersion;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private Map<String, Object> promptAudit = new LinkedHashMap<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private List<String> validationErrors = List.of();

	private Long retryOfFeedbackId;

	@Column(nullable = false)
	private int retryCount;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	private OffsetDateTime completedAt;

	protected AiFeedback() {
	}

	private AiFeedback(User studentUser, String feedbackType, Long writingSubmissionId, Long speakingSubmissionId) {
		this.studentUser = studentUser;
		this.feedbackType = feedbackType;
		this.writingSubmissionId = writingSubmissionId;
		this.speakingSubmissionId = speakingSubmissionId;
	}

	public static AiFeedback processing(User studentUser, String feedbackType, Long writingSubmissionId,
			Long speakingSubmissionId) {
		AiFeedback feedback = new AiFeedback(studentUser, feedbackType, writingSubmissionId, speakingSubmissionId);
		feedback.status = AiFeedbackStatuses.PROCESSING;
		return feedback;
	}

	public static AiFeedback failed(User studentUser, String feedbackType, Long writingSubmissionId,
			Long speakingSubmissionId, String failureCode, String errorMessage) {
		AiFeedback feedback = new AiFeedback(studentUser, feedbackType, writingSubmissionId, speakingSubmissionId);
		feedback.markFailed(failureCode, errorMessage, null, null, 0, 0, BigDecimal.ZERO);
		return feedback;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now(ZoneOffset.UTC);
		}
	}

	public void markCompleted(String provider, String model, int promptTokens, int completionTokens,
			BigDecimal estimatedCost) {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		this.status = AiFeedbackStatuses.COMPLETED;
		this.provider = provider;
		this.model = model;
		this.promptTokens = promptTokens;
		this.completionTokens = completionTokens;
		this.estimatedCost = estimatedCost == null ? BigDecimal.ZERO : estimatedCost;
		this.failureCode = null;
		this.errorMessage = null;
		this.validationErrors = List.of();
		this.completedAt = now;
	}

	public boolean isProcessing() {
		return AiFeedbackStatuses.PROCESSING.equals(status);
	}

	public boolean isCompleted() {
		return AiFeedbackStatuses.COMPLETED.equals(status);
	}

	public boolean isFailed() {
		return AiFeedbackStatuses.FAILED.equals(status);
	}

	public boolean isActive() {
		return AiFeedbackStatuses.QUEUED.equals(status) || AiFeedbackStatuses.PROCESSING.equals(status);
	}

	public void markProcessing() {
		this.status = AiFeedbackStatuses.PROCESSING;
		this.failureCode = null;
		this.errorMessage = null;
		this.completedAt = null;
		this.validationErrors = List.of();
	}

	public void markFailed(String failureCode, String errorMessage, String provider, String model, int promptTokens,
			int completionTokens, BigDecimal estimatedCost) {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		this.status = AiFeedbackStatuses.FAILED;
		this.failureCode = failureCode;
		this.errorMessage = errorMessage;
		this.provider = provider;
		this.model = model;
		this.promptTokens = promptTokens;
		this.completionTokens = completionTokens;
		this.estimatedCost = estimatedCost == null ? BigDecimal.ZERO : estimatedCost;
		this.completedAt = now;
	}

	public void applyWritingFeedback(
			BigDecimal overallBand,
			Map<String, Object> criteriaScores,
			List<Object> mistakes,
			String improvedAnswer,
			List<Object> actionPlan,
			String rubricVersion,
			Map<String, Object> promptAudit
	) {
		this.overallBand = overallBand;
		this.criteriaScores = copyMap(criteriaScores);
		this.mistakes = copyList(mistakes);
		this.improvedAnswer = improvedAnswer;
		this.actionPlan = copyList(actionPlan);
		this.rubricVersion = rubricVersion;
		this.promptAudit = copyMap(promptAudit);
		this.validationErrors = List.of();
	}

	public void applySpeakingFeedback(
			BigDecimal overallBand,
			Map<String, Object> criteriaScores,
			List<Object> repeatedMistakes,
			String betterAnswerExample,
			List<Object> actionPlan,
			String rubricVersion,
			Map<String, Object> promptAudit
	) {
		this.overallBand = overallBand;
		this.criteriaScores = copyMap(criteriaScores);
		this.mistakes = copyList(repeatedMistakes);
		this.improvedAnswer = betterAnswerExample;
		this.actionPlan = copyList(actionPlan);
		this.rubricVersion = rubricVersion;
		this.promptAudit = copyMap(promptAudit);
		this.validationErrors = List.of();
	}

	public void applyPromptAudit(String rubricVersion, Map<String, Object> promptAudit) {
		this.rubricVersion = rubricVersion;
		this.promptAudit = copyMap(promptAudit);
	}

	public void applyValidationErrors(List<String> validationErrors) {
		this.validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
		this.overallBand = null;
		this.criteriaScores = new LinkedHashMap<>();
		this.mistakes = List.of();
		this.improvedAnswer = null;
		this.actionPlan = List.of();
	}

	public void markRetryOf(Long retryOfFeedbackId, int retryCount) {
		this.retryOfFeedbackId = retryOfFeedbackId;
		this.retryCount = Math.max(0, retryCount);
	}

	private Map<String, Object> copyMap(Map<String, Object> value) {
		return value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
	}

	private List<Object> copyList(List<Object> value) {
		return value == null ? List.of() : List.copyOf(value);
	}

	public Long getId() {
		return id;
	}

	public User getStudentUser() {
		return studentUser;
	}

	public String getFeedbackType() {
		return feedbackType;
	}

	public Long getWritingSubmissionId() {
		return writingSubmissionId;
	}

	public Long getSpeakingSubmissionId() {
		return speakingSubmissionId;
	}

	public String getStatus() {
		return status;
	}

	public String getProvider() {
		return provider;
	}

	public String getModel() {
		return model;
	}

	public int getPromptTokens() {
		return promptTokens;
	}

	public int getCompletionTokens() {
		return completionTokens;
	}

	public BigDecimal getEstimatedCost() {
		return estimatedCost;
	}

	public BigDecimal getOverallBand() {
		return overallBand;
	}

	public Map<String, Object> getCriteriaScores() {
		return criteriaScores;
	}

	public List<Object> getMistakes() {
		return mistakes;
	}

	public String getImprovedAnswer() {
		return improvedAnswer;
	}

	public List<Object> getActionPlan() {
		return actionPlan;
	}

	public String getFailureCode() {
		return failureCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getRubricVersion() {
		return rubricVersion;
	}

	public Map<String, Object> getPromptAudit() {
		return promptAudit;
	}

	public List<String> getValidationErrors() {
		return validationErrors;
	}

	public Long getRetryOfFeedbackId() {
		return retryOfFeedbackId;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}
}
