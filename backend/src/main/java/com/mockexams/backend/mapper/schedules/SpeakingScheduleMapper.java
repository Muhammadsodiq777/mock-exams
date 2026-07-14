package com.mockexams.backend.mapper.schedules;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.schedules.GoogleCalendarProviderFailureRDO;
import com.mockexams.backend.dto.schedules.ScheduleBookingRDO;
import com.mockexams.backend.dto.schedules.ScheduleSlotRDO;
import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.entity.schedules.GoogleCalendarSyncJob;
import com.mockexams.backend.entity.schedules.GoogleCalendarSyncJobStatuses;
import com.mockexams.backend.entity.schedules.SpeakingSessionBooking;
import com.mockexams.backend.entity.schedules.SpeakingSessionSlotStatuses;
import com.mockexams.backend.entity.schedules.SpeakingSessionSlot;
import com.mockexams.backend.teachers.entity.TeacherProfile;

@Component
public class SpeakingScheduleMapper {

	public ScheduleSlotRDO toSlotRDO(SpeakingSessionSlot slot) {
		TeacherProfile teacherProfile = slot.getTeacherProfile();
		return new ScheduleSlotRDO(
				slot.getId(),
				teacherProfile.getId(),
				teacherProfile.getDisplayName(),
				teacherProfile.getHeadline(),
				slot.getStartsAt(),
				slot.getEndsAt(),
				slot.getTimezone(),
				slot.getPriceAmount(),
				slot.getCurrency(),
				slot.getStatus(),
				SpeakingSessionSlotStatuses.ANNOUNCED.equals(slot.getStatus()),
				slot.getReservedUntil(),
				slot.getCreatedAt(),
				slot.getUpdatedAt());
	}

	public ScheduleBookingRDO toBookingRDO(SpeakingSessionBooking booking) {
		return toBookingRDO(booking, null);
	}

	public ScheduleBookingRDO toBookingRDO(SpeakingSessionBooking booking, GoogleCalendarSyncJob syncJob) {
		SpeakingSessionSlot slot = booking.getSlot();
		TeacherProfile teacherProfile = booking.getTeacherProfile();
		Payment payment = booking.getPayment();
		return new ScheduleBookingRDO(
				booking.getId(),
				slot.getId(),
				booking.getStudent().getId(),
				booking.getStudent().getFullName(),
				teacherProfile.getId(),
				teacherProfile.getDisplayName(),
				booking.getStatus(),
				slot.getStartsAt(),
				slot.getEndsAt(),
				slot.getTimezone(),
				booking.getPriceAmount(),
				booking.getCurrency(),
				payment == null ? null : payment.getId(),
				payment == null ? null : payment.getStatus(),
				booking.getMeetUrl(),
				booking.getHoldExpiresAt(),
				providerSyncStatus(booking, syncJob),
				syncJob == null ? null : syncJob.getFailureCode(),
				syncJob == null ? null : syncJob.getFailureMessage(),
				booking.getCreatedAt(),
				booking.getUpdatedAt());
	}

	private String providerSyncStatus(SpeakingSessionBooking booking, GoogleCalendarSyncJob syncJob) {
		if (booking.getMeetUrl() != null && !booking.getMeetUrl().isBlank()) {
			return "synced";
		}
		if (syncJob == null) {
			return "not_required";
		}
		if (GoogleCalendarSyncJobStatuses.COMPLETED.equals(syncJob.getStatus())) {
			return "synced";
		}
		return syncJob.getStatus();
	}

	public GoogleCalendarProviderFailureRDO toProviderFailureRDO(GoogleCalendarSyncJob job) {
		return new GoogleCalendarProviderFailureRDO(
				job.getId(),
				job.getBooking().getId(),
				job.getBooking().getSlot().getId(),
				job.getTeacherProfile().getId(),
				job.getTeacherProfile().getDisplayName(),
				job.getBooking().getStudent().getId(),
				job.getBooking().getStudent().getFullName(),
				job.getProvider(),
				job.getStatus(),
				job.getOperation(),
				job.getStatus(),
				job.getFailureCode(),
				job.getFailureMessage(),
				job.getAttempts(),
				job.getBooking().getSlot().getStartsAt(),
				job.getCreatedAt(),
				job.getUpdatedAt());
	}
}
