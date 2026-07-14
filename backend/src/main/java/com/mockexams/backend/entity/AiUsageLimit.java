package com.mockexams.backend.entity;

import java.time.LocalDate;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.mockexams.backend.users.entity.User;

@Entity
@Table(name = "ai_usage_limits")
public class AiUsageLimit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private LocalDate periodStart;

	@Column(nullable = false)
	private LocalDate periodEnd;

	@Column(nullable = false)
	private int freeRequestsLimit;

	@Column(nullable = false)
	private int usedRequests;

	private Integer hardLimitRequests;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected AiUsageLimit() {
	}

	public AiUsageLimit(
			User user,
			LocalDate periodStart,
			LocalDate periodEnd,
			int freeRequestsLimit,
			Integer hardLimitRequests
	) {
		this.user = user;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.freeRequestsLimit = freeRequestsLimit;
		this.hardLimitRequests = hardLimitRequests;
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

	public boolean isHardLimitReached() {
		return hardLimitRequests != null && usedRequests >= hardLimitRequests;
	}

	public void reserveRequest() {
		usedRequests++;
	}

	public void releaseRequest() {
		if (usedRequests > 0) {
			usedRequests--;
		}
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public LocalDate getPeriodStart() {
		return periodStart;
	}

	public LocalDate getPeriodEnd() {
		return periodEnd;
	}

	public int getFreeRequestsLimit() {
		return freeRequestsLimit;
	}

	public int getUsedRequests() {
		return usedRequests;
	}

	public Integer getHardLimitRequests() {
		return hardLimitRequests;
	}
}
