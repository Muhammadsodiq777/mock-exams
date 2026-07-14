package com.mockexams.backend.entity.schedules;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.users.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "speaking_session_bookings")
public class SpeakingSessionBooking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "slot_id")
	private SpeakingSessionSlot slot;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_user_id")
	private User student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_profile_id")
	private TeacherProfile teacherProfile;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_id")
	private Payment payment;

	@Column(nullable = false, length = 24)
	private String status;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal priceAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	private OffsetDateTime holdExpiresAt;

	private OffsetDateTime bookedAt;

	private OffsetDateTime cancelledAt;

	private OffsetDateTime completedAt;

	private OffsetDateTime noShowAt;

	@Column(length = 500)
	private String meetUrl;

	@Column(length = 160)
	private String calendarEventId;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected SpeakingSessionBooking() {
	}

	public static SpeakingSessionBooking reserved(SpeakingSessionSlot slot, User student, OffsetDateTime holdExpiresAt) {
		SpeakingSessionBooking booking = new SpeakingSessionBooking();
		booking.slot = slot;
		booking.student = student;
		booking.teacherProfile = slot.getTeacherProfile();
		booking.status = SpeakingSessionBookingStatuses.RESERVED;
		booking.priceAmount = slot.getPriceAmount();
		booking.currency = slot.getCurrency();
		booking.holdExpiresAt = holdExpiresAt;
		return booking;
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
		if (status == null) {
			status = SpeakingSessionBookingStatuses.RESERVED;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public void attachPayment(Payment payment) {
		this.payment = payment;
	}

	public void confirmBooked(Payment payment, OffsetDateTime bookedAt, String meetUrl, String calendarEventId) {
		this.payment = payment;
		status = SpeakingSessionBookingStatuses.BOOKED;
		this.bookedAt = bookedAt;
		holdExpiresAt = null;
		this.meetUrl = meetUrl;
		this.calendarEventId = calendarEventId;
	}

	public void cancel(OffsetDateTime cancelledAt) {
		status = SpeakingSessionBookingStatuses.CANCELLED;
		this.cancelledAt = cancelledAt;
		holdExpiresAt = null;
	}

	public void expire() {
		status = SpeakingSessionBookingStatuses.EXPIRED;
		holdExpiresAt = null;
	}

	public void complete(OffsetDateTime completedAt) {
		status = SpeakingSessionBookingStatuses.COMPLETED;
		this.completedAt = completedAt;
	}

	public void noShow(OffsetDateTime noShowAt) {
		status = SpeakingSessionBookingStatuses.NO_SHOW;
		this.noShowAt = noShowAt;
	}

	public Long getId() {
		return id;
	}

	public SpeakingSessionSlot getSlot() {
		return slot;
	}

	public User getStudent() {
		return student;
	}

	public TeacherProfile getTeacherProfile() {
		return teacherProfile;
	}

	public Payment getPayment() {
		return payment;
	}

	public String getStatus() {
		return status;
	}

	public BigDecimal getPriceAmount() {
		return priceAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public OffsetDateTime getHoldExpiresAt() {
		return holdExpiresAt;
	}

	public OffsetDateTime getBookedAt() {
		return bookedAt;
	}

	public OffsetDateTime getCancelledAt() {
		return cancelledAt;
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}

	public OffsetDateTime getNoShowAt() {
		return noShowAt;
	}

	public String getMeetUrl() {
		return meetUrl;
	}

	public String getCalendarEventId() {
		return calendarEventId;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
