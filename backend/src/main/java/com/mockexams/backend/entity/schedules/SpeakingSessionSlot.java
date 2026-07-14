package com.mockexams.backend.entity.schedules;

import java.math.BigDecimal;
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
@Table(name = "speaking_session_slots")
public class SpeakingSessionSlot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_profile_id")
	private TeacherProfile teacherProfile;

	@Column(nullable = false)
	private OffsetDateTime startsAt;

	@Column(nullable = false)
	private OffsetDateTime endsAt;

	@Column(nullable = false, length = 80)
	private String timezone;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal priceAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(nullable = false)
	private Integer capacity = 1;

	@Column(nullable = false, length = 24)
	private String status;

	private OffsetDateTime announcedAt;

	private OffsetDateTime reservedUntil;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected SpeakingSessionSlot() {
	}

	public static SpeakingSessionSlot draft(
			TeacherProfile teacherProfile,
			OffsetDateTime startsAt,
			OffsetDateTime endsAt,
			String timezone,
			BigDecimal priceAmount,
			String currency
	) {
		SpeakingSessionSlot slot = new SpeakingSessionSlot();
		slot.teacherProfile = teacherProfile;
		slot.startsAt = startsAt;
		slot.endsAt = endsAt;
		slot.timezone = timezone;
		slot.priceAmount = priceAmount;
		slot.currency = currency;
		slot.capacity = 1;
		slot.status = SpeakingSessionSlotStatuses.DRAFT;
		return slot;
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
		if (capacity == null) {
			capacity = 1;
		}
		if (status == null) {
			status = SpeakingSessionSlotStatuses.DRAFT;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public void update(
			OffsetDateTime startsAt,
			OffsetDateTime endsAt,
			String timezone,
			BigDecimal priceAmount,
			String currency
	) {
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.timezone = timezone;
		this.priceAmount = priceAmount;
		this.currency = currency;
	}

	public void announce(OffsetDateTime announcedAt) {
		status = SpeakingSessionSlotStatuses.ANNOUNCED;
		this.announcedAt = announcedAt;
	}

	public void unannounce() {
		status = SpeakingSessionSlotStatuses.DRAFT;
		announcedAt = null;
	}

	public void reserveUntil(OffsetDateTime reservedUntil) {
		status = SpeakingSessionSlotStatuses.RESERVED;
		this.reservedUntil = reservedUntil;
	}

	public void markBooked() {
		status = SpeakingSessionSlotStatuses.BOOKED;
		reservedUntil = null;
	}

	public void releaseReservation() {
		status = SpeakingSessionSlotStatuses.ANNOUNCED;
		reservedUntil = null;
	}

	public void markCompleted() {
		status = SpeakingSessionSlotStatuses.COMPLETED;
		reservedUntil = null;
	}

	public void markCancelled() {
		status = SpeakingSessionSlotStatuses.CANCELLED;
		reservedUntil = null;
	}

	public Long getId() {
		return id;
	}

	public TeacherProfile getTeacherProfile() {
		return teacherProfile;
	}

	public OffsetDateTime getStartsAt() {
		return startsAt;
	}

	public OffsetDateTime getEndsAt() {
		return endsAt;
	}

	public String getTimezone() {
		return timezone;
	}

	public BigDecimal getPriceAmount() {
		return priceAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public String getStatus() {
		return status;
	}

	public OffsetDateTime getAnnouncedAt() {
		return announcedAt;
	}

	public OffsetDateTime getReservedUntil() {
		return reservedUntil;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
