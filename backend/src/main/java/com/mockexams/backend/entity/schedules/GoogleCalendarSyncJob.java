package com.mockexams.backend.entity.schedules;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
@Table(name = "google_calendar_sync_jobs")
public class GoogleCalendarSyncJob {

	public static final String OPERATION_CREATE_EVENT = "create_event";
	public static final String OPERATION_CANCEL_EVENT = "cancel_event";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id")
	private SpeakingSessionBooking booking;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_profile_id")
	private TeacherProfile teacherProfile;

	@Column(nullable = false, length = 40)
	private String provider;

	@Column(nullable = false, length = 40)
	private String operation;

	@Column(nullable = false, length = 24)
	private String status;

	@Column(length = 160)
	private String providerEventId;

	@Column(length = 500)
	private String meetUrl;

	@Column(length = 80)
	private String failureCode;

	@Column(length = 500)
	private String failureMessage;

	@Column(nullable = false)
	private Integer attempts = 0;

	@Column(nullable = false)
	private OffsetDateTime scheduledAt;

	private OffsetDateTime completedAt;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected GoogleCalendarSyncJob() {
	}

	public static GoogleCalendarSyncJob completedCreateEvent(
			SpeakingSessionBooking booking,
			String providerEventId,
			String meetUrl,
			OffsetDateTime completedAt
	) {
		GoogleCalendarSyncJob job = new GoogleCalendarSyncJob();
		job.booking = booking;
		job.teacherProfile = booking.getTeacherProfile();
		job.provider = "mock_google_calendar";
		job.operation = OPERATION_CREATE_EVENT;
		job.status = GoogleCalendarSyncJobStatuses.COMPLETED;
		job.providerEventId = providerEventId;
		job.meetUrl = meetUrl;
		job.attempts = 1;
		job.scheduledAt = completedAt;
		job.completedAt = completedAt;
		return job;
	}

	@PrePersist
	void prePersist() {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
		if (scheduledAt == null) {
			scheduledAt = now;
		}
		if (attempts == null) {
			attempts = 0;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public Long getId() {
		return id;
	}

	public SpeakingSessionBooking getBooking() {
		return booking;
	}

	public TeacherProfile getTeacherProfile() {
		return teacherProfile;
	}

	public String getProvider() {
		return provider;
	}

	public String getOperation() {
		return operation;
	}

	public String getStatus() {
		return status;
	}

	public String getProviderEventId() {
		return providerEventId;
	}

	public String getMeetUrl() {
		return meetUrl;
	}

	public String getFailureCode() {
		return failureCode;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	public Integer getAttempts() {
		return attempts;
	}

	public OffsetDateTime getScheduledAt() {
		return scheduledAt;
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
