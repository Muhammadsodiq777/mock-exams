package com.mockexams.backend.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

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
@Table(name = "test_attempts")
public class TestAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "test_id", nullable = false)
	private CatalogTest test;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_user_id", nullable = false)
	private User student;

	@Column(nullable = false, length = 24)
	private String status = TestAttemptStatuses.IN_PROGRESS;

	@Column(nullable = false)
	private OffsetDateTime startedAt;

	private OffsetDateTime submittedAt;

	private OffsetDateTime expiresAt;

	@Column(nullable = false)
	private Integer timeSpentSeconds = 0;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected TestAttempt() {
	}

	public static TestAttempt start(CatalogTest test, User student, OffsetDateTime now) {
		TestAttempt attempt = new TestAttempt();
		attempt.test = test;
		attempt.student = student;
		attempt.status = TestAttemptStatuses.IN_PROGRESS;
		attempt.startedAt = now;
		attempt.expiresAt = now.plusMinutes(test.getDurationMinutes());
		attempt.timeSpentSeconds = 0;
		return attempt;
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

	public void submit(OffsetDateTime now) {
		status = TestAttemptStatuses.SUBMITTED;
		submittedAt = now;
		timeSpentSeconds = elapsedSeconds(now);
	}

	public void expire(OffsetDateTime now) {
		status = TestAttemptStatuses.EXPIRED;
		timeSpentSeconds = elapsedSeconds(now);
	}

	public boolean isInProgress() {
		return TestAttemptStatuses.IN_PROGRESS.equals(status);
	}

	public boolean isSubmitted() {
		return TestAttemptStatuses.SUBMITTED.equals(status);
	}

	public boolean isExpired() {
		return TestAttemptStatuses.EXPIRED.equals(status);
	}

	public boolean isPastExpiry(OffsetDateTime now) {
		return expiresAt != null && !now.isBefore(expiresAt);
	}

	public long remainingSeconds(OffsetDateTime now) {
		if (expiresAt == null || !isInProgress()) {
			return 0;
		}
		return Math.max(0, ChronoUnit.SECONDS.between(now, expiresAt));
	}

	private int elapsedSeconds(OffsetDateTime now) {
		if (startedAt == null) {
			return 0;
		}
		long boundedNow = expiresAt == null ? ChronoUnit.SECONDS.between(startedAt, now)
				: ChronoUnit.SECONDS.between(startedAt, now.isAfter(expiresAt) ? expiresAt : now);
		return Math.toIntExact(Math.max(0, boundedNow));
	}

	public Long getId() {
		return id;
	}

	public CatalogTest getTest() {
		return test;
	}

	public User getStudent() {
		return student;
	}

	public String getStatus() {
		return status;
	}

	public OffsetDateTime getStartedAt() {
		return startedAt;
	}

	public OffsetDateTime getSubmittedAt() {
		return submittedAt;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public Integer getTimeSpentSeconds() {
		return timeSpentSeconds;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
