package com.mockexams.backend.service.schedules;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.schedules.GoogleCalendarProviderFailureRDO;
import com.mockexams.backend.dto.schedules.ScheduleBookingRDO;
import com.mockexams.backend.dto.schedules.ScheduleSlotCDO;
import com.mockexams.backend.dto.schedules.ScheduleSlotRDO;
import com.mockexams.backend.dto.schedules.ScheduleSlotUDO;
import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.users.entity.User;

public interface SpeakingScheduleService {

	Page<ScheduleSlotRDO> listPublicSlots(
			Long teacherProfileId,
			String status,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable);

	Page<ScheduleSlotRDO> listTeacherSlots(
			User teacher,
			String status,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable);

	ScheduleSlotRDO createTeacherSlot(User teacher, ScheduleSlotCDO command);

	ScheduleSlotRDO updateTeacherSlot(User teacher, Long slotId, ScheduleSlotUDO command);

	ScheduleSlotRDO announceTeacherSlot(User teacher, Long slotId);

	ScheduleSlotRDO unannounceTeacherSlot(User teacher, Long slotId);

	Page<ScheduleBookingRDO> listStudentBookings(
			User student,
			String status,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable);

	ScheduleBookingRDO getStudentBooking(User student, Long bookingId);

	ScheduleBookingRDO reserveSlot(User student, Long slotId);

	LiveSpeakingPaymentTarget requirePaymentTarget(User student, Long bookingId);

	ScheduleBookingRDO attachPayment(User student, Long bookingId, Payment payment);

	ScheduleBookingRDO cancelStudentBooking(User student, Long bookingId);

	Page<ScheduleBookingRDO> listTeacherBookings(
			User teacher,
			String status,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable);

	ScheduleBookingRDO getTeacherBooking(User teacher, Long bookingId);

	ScheduleBookingRDO completeTeacherBooking(User teacher, Long bookingId);

	ScheduleBookingRDO markTeacherBookingNoShow(User teacher, Long bookingId);

	Page<ScheduleBookingRDO> listAdminBookings(
			String status,
			Long teacherProfileId,
			Long studentUserId,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable);

	ScheduleBookingRDO getAdminBooking(Long bookingId);

	Page<GoogleCalendarProviderFailureRDO> listProviderFailures(Pageable pageable);

	boolean confirmPaidBooking(Payment payment);
}
