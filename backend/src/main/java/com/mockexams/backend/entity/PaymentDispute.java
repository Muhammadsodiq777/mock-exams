package com.mockexams.backend.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.mockexams.backend.users.entity.User;

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
@Table(name = "payment_disputes")
public class PaymentDispute {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id")
	private Payment payment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "opened_by_user_id")
	private User openedByUser;

	@Column(nullable = false, length = 24)
	private String status = PaymentDisputeStatuses.OPEN;

	@Column(nullable = false, length = 160)
	private String reason;

	@Column
	private String description;

	@Column
	private String resolutionNote;

	private Long reviewedByUserId;

	private OffsetDateTime reviewedAt;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected PaymentDispute() {
	}

	public static PaymentDispute open(Payment payment, User openedByUser, String reason, String description) {
		PaymentDispute dispute = new PaymentDispute();
		dispute.payment = payment;
		dispute.openedByUser = openedByUser;
		dispute.status = PaymentDisputeStatuses.OPEN;
		dispute.reason = reason;
		dispute.description = description;
		return dispute;
	}

	@PrePersist
	void prePersist() {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
		if (status == null) {
			status = PaymentDisputeStatuses.OPEN;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	/**
	 * Applies an admin decision. {@code resolutionNote} is only overwritten when a non-null value is
	 * supplied, so an {@code under_review} decision without a note does not erase one recorded earlier.
	 */
	public void applyDecision(String status, Long reviewerUserId, OffsetDateTime reviewedAt, String resolutionNote) {
		this.status = status;
		this.reviewedByUserId = reviewerUserId;
		this.reviewedAt = reviewedAt;
		if (resolutionNote != null) {
			this.resolutionNote = resolutionNote;
		}
	}

	public boolean isTerminal() {
		return PaymentDisputeStatuses.isTerminal(status);
	}

	public Long getId() {
		return id;
	}

	public Payment getPayment() {
		return payment;
	}

	public User getOpenedByUser() {
		return openedByUser;
	}

	public String getStatus() {
		return status;
	}

	public String getReason() {
		return reason;
	}

	public String getDescription() {
		return description;
	}

	public String getResolutionNote() {
		return resolutionNote;
	}

	public Long getReviewedByUserId() {
		return reviewedByUserId;
	}

	public OffsetDateTime getReviewedAt() {
		return reviewedAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
