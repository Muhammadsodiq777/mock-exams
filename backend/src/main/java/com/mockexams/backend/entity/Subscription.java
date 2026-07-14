package com.mockexams.backend.entity;

import java.time.OffsetDateTime;

import com.mockexams.backend.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscriptions")
public class Subscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_user_id")
	private User student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "listing_id")
	private MarketplaceListing listing;

	private Long paymentId;

	@Column(nullable = false, length = 24)
	private String status;

	@Column(nullable = false)
	private OffsetDateTime startsAt;

	@Column(nullable = false)
	private OffsetDateTime endsAt;

	private OffsetDateTime cancelledAt;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected Subscription() {
	}

	public boolean grantsAccessAt(OffsetDateTime now) {
		return SubscriptionStatuses.ACTIVE.equals(status)
				&& !startsAt.isAfter(now)
				&& endsAt.isAfter(now);
	}

	public boolean isExpiredAt(OffsetDateTime now) {
		return SubscriptionStatuses.ACTIVE.equals(status) && !endsAt.isAfter(now);
	}

	public Long getId() {
		return id;
	}

	public User getStudent() {
		return student;
	}

	public MarketplaceListing getListing() {
		return listing;
	}

	public Long getPaymentId() {
		return paymentId;
	}

	public String getStatus() {
		return status;
	}

	public OffsetDateTime getStartsAt() {
		return startsAt;
	}

	public OffsetDateTime getEndsAt() {
		return endsAt;
	}

	public OffsetDateTime getCancelledAt() {
		return cancelledAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
