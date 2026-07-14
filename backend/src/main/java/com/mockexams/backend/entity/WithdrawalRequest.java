package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.mockexams.backend.teachers.entity.TeacherProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "withdrawal_requests")
public class WithdrawalRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_profile_id")
	private TeacherProfile teacherProfile;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal requestedAmount;

	@Column(nullable = false, length = 3)
	private String currency = "UZS";

	@Column(nullable = false, length = 24)
	private String status = WithdrawalRequestStatuses.REQUESTED;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> payoutDetails;

	private Long reviewedByUserId;

	private OffsetDateTime reviewedAt;

	private String rejectionReason;

	private OffsetDateTime paidAt;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected WithdrawalRequest() {
	}

	public static WithdrawalRequest create(
			TeacherProfile teacherProfile,
			BigDecimal requestedAmount,
			String currency,
			Map<String, Object> payoutDetails
	) {
		WithdrawalRequest request = new WithdrawalRequest();
		request.teacherProfile = teacherProfile;
		request.requestedAmount = requestedAmount;
		request.currency = currency;
		request.payoutDetails = copyPayoutDetails(payoutDetails);
		return request;
	}

	@PrePersist
	void prePersist() {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public void markApproved(Long reviewerUserId, OffsetDateTime reviewedAt) {
		this.status = WithdrawalRequestStatuses.APPROVED;
		this.reviewedByUserId = reviewerUserId;
		this.reviewedAt = reviewedAt;
	}

	public void markRejected(Long reviewerUserId, OffsetDateTime reviewedAt, String rejectionReason) {
		this.status = WithdrawalRequestStatuses.REJECTED;
		this.reviewedByUserId = reviewerUserId;
		this.reviewedAt = reviewedAt;
		this.rejectionReason = rejectionReason;
	}

	public void markCancelled() {
		this.status = WithdrawalRequestStatuses.CANCELLED;
	}

	public void markPaid(OffsetDateTime paidAt) {
		this.status = WithdrawalRequestStatuses.PAID;
		this.paidAt = paidAt;
	}

	public Long getId() {
		return id;
	}

	public TeacherProfile getTeacherProfile() {
		return teacherProfile;
	}

	public BigDecimal getRequestedAmount() {
		return requestedAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getStatus() {
		return status;
	}

	public Map<String, Object> getPayoutDetails() {
		return payoutDetails;
	}

	public Long getReviewedByUserId() {
		return reviewedByUserId;
	}

	public OffsetDateTime getReviewedAt() {
		return reviewedAt;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public OffsetDateTime getPaidAt() {
		return paidAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	private static Map<String, Object> copyPayoutDetails(Map<String, Object> payoutDetails) {
		if (payoutDetails == null || payoutDetails.isEmpty()) {
			return Map.of();
		}
		return new LinkedHashMap<>(payoutDetails);
	}
}
