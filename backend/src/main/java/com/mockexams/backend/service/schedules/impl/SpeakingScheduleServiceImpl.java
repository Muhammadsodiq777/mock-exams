package com.mockexams.backend.service.schedules.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.NotificationCDO;
import com.mockexams.backend.dto.schedules.GoogleCalendarProviderFailureRDO;
import com.mockexams.backend.dto.schedules.ScheduleBookingRDO;
import com.mockexams.backend.dto.schedules.ScheduleSlotCDO;
import com.mockexams.backend.dto.schedules.ScheduleSlotRDO;
import com.mockexams.backend.dto.schedules.ScheduleSlotUDO;
import com.mockexams.backend.entity.NotificationTypes;
import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.entity.schedules.GoogleCalendarSyncJob;
import com.mockexams.backend.entity.schedules.GoogleCalendarSyncJobStatuses;
import com.mockexams.backend.entity.schedules.SpeakingSessionBooking;
import com.mockexams.backend.entity.schedules.SpeakingSessionBookingStatuses;
import com.mockexams.backend.entity.schedules.SpeakingSessionSlot;
import com.mockexams.backend.entity.schedules.SpeakingSessionSlotStatuses;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.schedules.SpeakingScheduleMapper;
import com.mockexams.backend.repository.schedules.GoogleCalendarSyncJobRepository;
import com.mockexams.backend.repository.schedules.SpeakingSessionBookingRepository;
import com.mockexams.backend.repository.schedules.SpeakingSessionSlotRepository;
import com.mockexams.backend.service.NotificationService;
import com.mockexams.backend.service.schedules.LiveSpeakingPaymentTarget;
import com.mockexams.backend.service.schedules.SpeakingScheduleService;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.users.entity.User;

@Service
class SpeakingScheduleServiceImpl implements SpeakingScheduleService {

	private static final int HOLD_MINUTES = 10;
	private static final String DEFAULT_PUBLIC_STATUS = SpeakingSessionSlotStatuses.ANNOUNCED;

	private final SpeakingSessionSlotRepository slotRepository;
	private final SpeakingSessionBookingRepository bookingRepository;
	private final GoogleCalendarSyncJobRepository syncJobRepository;
	private final TeacherProfileRepository teacherProfileRepository;
	private final NotificationService notificationService;
	private final SpeakingScheduleMapper mapper;

	SpeakingScheduleServiceImpl(
			SpeakingSessionSlotRepository slotRepository,
			SpeakingSessionBookingRepository bookingRepository,
			GoogleCalendarSyncJobRepository syncJobRepository,
			TeacherProfileRepository teacherProfileRepository,
			NotificationService notificationService,
			SpeakingScheduleMapper mapper
	) {
		this.slotRepository = slotRepository;
		this.bookingRepository = bookingRepository;
		this.syncJobRepository = syncJobRepository;
		this.teacherProfileRepository = teacherProfileRepository;
		this.notificationService = notificationService;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ScheduleSlotRDO> listPublicSlots(
			Long teacherProfileId,
			String status,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable
	) {
		String normalizedStatus = normalizeSlotStatus(status);
		if (normalizedStatus != null && !DEFAULT_PUBLIC_STATUS.equals(normalizedStatus)) {
			return Page.empty(pageable);
		}
		return slotRepository.findStudentVisibleSlots(
						teacherProfileId,
						DEFAULT_PUBLIC_STATUS,
						from,
						to,
						pageable)
				.map(mapper::toSlotRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ScheduleSlotRDO> listTeacherSlots(
			User teacher,
			String status,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable
	) {
		TeacherProfile profile = requireApprovedTeacherProfile(teacher);
		return slotRepository.findTeacherSlots(profile.getId(), normalizeSlotStatus(status), from, to, pageable)
				.map(mapper::toSlotRDO);
	}

	@Override
	@Transactional
	public ScheduleSlotRDO createTeacherSlot(User teacher, ScheduleSlotCDO command) {
		TeacherProfile profile = requireApprovedTeacherProfile(teacher);
		SlotValues values = validateSlotValues(
				command.startsAt(),
				command.endsAt(),
				command.timezone(),
				command.priceAmount(),
				command.currency());
		SpeakingSessionSlot slot = SpeakingSessionSlot.draft(
				profile,
				values.startsAt(),
				values.endsAt(),
				values.timezone(),
				values.priceAmount(),
				values.currency());
		return mapper.toSlotRDO(slotRepository.save(slot));
	}

	@Override
	@Transactional
	public ScheduleSlotRDO updateTeacherSlot(User teacher, Long slotId, ScheduleSlotUDO command) {
		TeacherProfile profile = requireApprovedTeacherProfile(teacher);
		SpeakingSessionSlot slot = slotRepository.findByIdAndTeacherProfile_Id(requireId(slotId, "Slot id"), profile.getId())
				.orElseThrow(this::slotNotFound);
		requireMutableSlot(slot);
		SlotValues values = validateSlotValues(
				command.startsAt() == null ? slot.getStartsAt() : command.startsAt(),
				command.endsAt() == null ? slot.getEndsAt() : command.endsAt(),
				command.timezone() == null ? slot.getTimezone() : command.timezone(),
				command.priceAmount() == null ? slot.getPriceAmount() : command.priceAmount(),
				command.currency() == null ? slot.getCurrency() : command.currency());
		slot.update(values.startsAt(), values.endsAt(), values.timezone(), values.priceAmount(), values.currency());
		if (SpeakingSessionSlotStatuses.ANNOUNCED.equals(slot.getStatus())) {
			requireNoTeacherOverlap(slot);
		}
		return mapper.toSlotRDO(slot);
	}

	@Override
	@Transactional
	public ScheduleSlotRDO announceTeacherSlot(User teacher, Long slotId) {
		TeacherProfile profile = requireApprovedTeacherProfile(teacher);
		SpeakingSessionSlot slot = slotRepository.findByIdAndTeacherProfile_Id(requireId(slotId, "Slot id"), profile.getId())
				.orElseThrow(this::slotNotFound);
		if (!SpeakingSessionSlotStatuses.DRAFT.equals(slot.getStatus())
				&& !SpeakingSessionSlotStatuses.ANNOUNCED.equals(slot.getStatus())) {
			throw slotUnavailable("Only draft slots can be announced.");
		}
		if (slot.getPriceAmount().compareTo(BigDecimal.ZERO) < 0) {
			throw validationFailed("Slot price cannot be negative.");
		}
		requireNoTeacherOverlap(slot);
		slot.announce(now());
		return mapper.toSlotRDO(slot);
	}

	@Override
	@Transactional
	public ScheduleSlotRDO unannounceTeacherSlot(User teacher, Long slotId) {
		TeacherProfile profile = requireApprovedTeacherProfile(teacher);
		SpeakingSessionSlot slot = slotRepository.findByIdAndTeacherProfile_Id(requireId(slotId, "Slot id"), profile.getId())
				.orElseThrow(this::slotNotFound);
		if (!SpeakingSessionSlotStatuses.ANNOUNCED.equals(slot.getStatus())
				&& !SpeakingSessionSlotStatuses.DRAFT.equals(slot.getStatus())) {
			throw slotUnavailable("Only unbooked announced slots can be unannounced.");
		}
		slot.unannounce();
		return mapper.toSlotRDO(slot);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ScheduleBookingRDO> listStudentBookings(
			User student,
			String status,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable
	) {
		return bookingRepository.findStudentBookings(student.getId(), normalizeBookingStatus(status), from, to, pageable)
				.map(this::toBookingRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public ScheduleBookingRDO getStudentBooking(User student, Long bookingId) {
		return bookingRepository.findByIdAndStudent_Id(requireId(bookingId, "Booking id"), student.getId())
				.map(this::toBookingRDO)
				.orElseThrow(this::bookingNotFound);
	}

	@Override
	@Transactional
	public ScheduleBookingRDO reserveSlot(User student, Long slotId) {
		SpeakingSessionSlot slot = slotRepository.findByIdForUpdate(requireId(slotId, "Slot id"))
				.orElseThrow(this::slotNotFound);
		if (!SpeakingSessionSlotStatuses.ANNOUNCED.equals(slot.getStatus()) || slot.getStartsAt().isBefore(now())) {
			throw slotUnavailable("Schedule slot is not available for reservation.");
		}
		if (!TeacherProfileStatuses.APPROVED.equals(slot.getTeacherProfile().getStatus())) {
			throw teacherApprovalRequired();
		}
		if (bookingRepository.existsActiveStudentOverlap(student.getId(), slot.getStartsAt(), slot.getEndsAt())) {
			throw bookingStatusConflict("Student already has an overlapping live speaking booking.");
		}
		OffsetDateTime holdExpiresAt = now().plusMinutes(HOLD_MINUTES);
		slot.reserveUntil(holdExpiresAt);
		SpeakingSessionBooking booking = bookingRepository.save(SpeakingSessionBooking.reserved(slot, student, holdExpiresAt));
		notifyUser(
				student.getId(),
				"Speaking slot reserved",
				"Your live speaking slot is reserved while you complete payment.",
				Map.of("bookingId", booking.getId(), "slotId", slot.getId(), "targetPath", "/speaking"));
		return toBookingRDO(booking);
	}

	@Override
	@Transactional
	public LiveSpeakingPaymentTarget requirePaymentTarget(User student, Long bookingId) {
		SpeakingSessionBooking booking = bookingRepository.findByIdAndStudentIdForUpdate(
						requireId(bookingId, "Booking id"),
						student.getId())
				.orElseThrow(this::bookingNotFound);
		requireReservedAndActive(booking);
		return new LiveSpeakingPaymentTarget(
				booking.getId(),
				money(booking.getPriceAmount()),
				normalizeCurrency(booking.getCurrency()),
				"Live speaking session with " + booking.getTeacherProfile().getDisplayName());
	}

	@Override
	@Transactional
	public ScheduleBookingRDO attachPayment(User student, Long bookingId, Payment payment) {
		SpeakingSessionBooking booking = bookingRepository.findByIdAndStudentIdForUpdate(
						requireId(bookingId, "Booking id"),
						student.getId())
				.orElseThrow(this::bookingNotFound);
		requireReservedAndActive(booking);
		booking.attachPayment(payment);
		return toBookingRDO(booking);
	}

	@Override
	@Transactional
	public ScheduleBookingRDO cancelStudentBooking(User student, Long bookingId) {
		SpeakingSessionBooking booking = bookingRepository.findByIdAndStudentIdForUpdate(
						requireId(bookingId, "Booking id"),
						student.getId())
				.orElseThrow(this::bookingNotFound);
		if (!SpeakingSessionBookingStatuses.RESERVED.equals(booking.getStatus())
				&& !SpeakingSessionBookingStatuses.BOOKED.equals(booking.getStatus())) {
			throw bookingStatusConflict("Only reserved or booked sessions can be cancelled.");
		}
		boolean wasReserved = SpeakingSessionBookingStatuses.RESERVED.equals(booking.getStatus());
		booking.cancel(now());
		if (wasReserved) {
			booking.getSlot().releaseReservation();
		}
		else {
			booking.getSlot().markCancelled();
		}
		notifyUser(
				booking.getTeacherProfile().getUser().getId(),
				"Speaking booking cancelled",
				"A student cancelled a live speaking booking.",
				Map.of("bookingId", booking.getId(), "targetPath", "/teacher/schedule"));
		return toBookingRDO(booking);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ScheduleBookingRDO> listTeacherBookings(
			User teacher,
			String status,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable
	) {
		TeacherProfile profile = requireApprovedTeacherProfile(teacher);
		return bookingRepository.findTeacherBookings(profile.getId(), normalizeBookingStatus(status), from, to, pageable)
				.map(this::toBookingRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public ScheduleBookingRDO getTeacherBooking(User teacher, Long bookingId) {
		TeacherProfile profile = requireApprovedTeacherProfile(teacher);
		return bookingRepository.findByIdAndTeacherProfile_Id(requireId(bookingId, "Booking id"), profile.getId())
				.map(this::toBookingRDO)
				.orElseThrow(this::bookingNotFound);
	}

	@Override
	@Transactional
	public ScheduleBookingRDO completeTeacherBooking(User teacher, Long bookingId) {
		TeacherProfile profile = requireApprovedTeacherProfile(teacher);
		SpeakingSessionBooking booking = bookingRepository.findByIdForUpdate(requireId(bookingId, "Booking id"))
				.filter(candidate -> candidate.getTeacherProfile().getId().equals(profile.getId()))
				.orElseThrow(this::bookingNotFound);
		if (!SpeakingSessionBookingStatuses.BOOKED.equals(booking.getStatus())) {
			throw bookingStatusConflict("Only booked sessions can be completed.");
		}
		booking.complete(now());
		booking.getSlot().markCompleted();
		return toBookingRDO(booking);
	}

	@Override
	@Transactional
	public ScheduleBookingRDO markTeacherBookingNoShow(User teacher, Long bookingId) {
		TeacherProfile profile = requireApprovedTeacherProfile(teacher);
		SpeakingSessionBooking booking = bookingRepository.findByIdForUpdate(requireId(bookingId, "Booking id"))
				.filter(candidate -> candidate.getTeacherProfile().getId().equals(profile.getId()))
				.orElseThrow(this::bookingNotFound);
		if (!SpeakingSessionBookingStatuses.BOOKED.equals(booking.getStatus())) {
			throw bookingStatusConflict("Only booked sessions can be marked no-show.");
		}
		booking.noShow(now());
		booking.getSlot().markCompleted();
		return toBookingRDO(booking);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ScheduleBookingRDO> listAdminBookings(
			String status,
			Long teacherProfileId,
			Long studentUserId,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable
	) {
		return bookingRepository.findAdminBookings(
						normalizeBookingStatus(status),
						teacherProfileId,
						studentUserId,
						from,
						to,
						pageable)
				.map(this::toBookingRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public ScheduleBookingRDO getAdminBooking(Long bookingId) {
		return bookingRepository.findById(requireId(bookingId, "Booking id"))
				.map(this::toBookingRDO)
				.orElseThrow(this::bookingNotFound);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<GoogleCalendarProviderFailureRDO> listProviderFailures(Pageable pageable) {
		return syncJobRepository.findByStatus(GoogleCalendarSyncJobStatuses.FAILED, pageable)
				.map(mapper::toProviderFailureRDO);
	}

	@Override
	@Transactional
	public boolean confirmPaidBooking(Payment payment) {
		Long bookingId = payment.getTargetLiveSpeakingBookingId();
		if (bookingId == null) {
			return false;
		}
		SpeakingSessionBooking booking = bookingRepository.findByIdForUpdate(bookingId)
				.orElseThrow(this::bookingNotFound);
		if (SpeakingSessionBookingStatuses.BOOKED.equals(booking.getStatus())) {
			ensureCompletedMockSync(booking);
			return true;
		}
		if (!SpeakingSessionBookingStatuses.RESERVED.equals(booking.getStatus())) {
			return false;
		}
		if (isHoldExpired(booking)) {
			booking.expire();
			booking.getSlot().releaseReservation();
			return false;
		}

		OffsetDateTime bookedAt = now();
		String providerEventId = "mock-live-speaking-" + booking.getId();
		String meetUrl = "https://meet.google.com/mock-" + booking.getId();
		booking.confirmBooked(payment, bookedAt, meetUrl, providerEventId);
		booking.getSlot().markBooked();
		ensureCompletedMockSync(booking);
		notifyUser(
				booking.getStudent().getId(),
				"Speaking booking confirmed",
				"Your live speaking booking is confirmed and the meeting link is ready.",
				Map.of("bookingId", booking.getId(), "targetPath", "/speaking"));
		notifyUser(
				booking.getTeacherProfile().getUser().getId(),
				"New live speaking booking",
				"A student booked one of your live speaking slots.",
				Map.of("bookingId", booking.getId(), "targetPath", "/teacher/schedule"));
		return true;
	}

	private ScheduleBookingRDO toBookingRDO(SpeakingSessionBooking booking) {
		GoogleCalendarSyncJob syncJob = syncJobRepository
				.findFirstByBooking_IdAndOperationOrderByCreatedAtDesc(
						booking.getId(),
						GoogleCalendarSyncJob.OPERATION_CREATE_EVENT)
				.orElse(null);
		return mapper.toBookingRDO(booking, syncJob);
	}

	private void ensureCompletedMockSync(SpeakingSessionBooking booking) {
		if (syncJobRepository.existsByBooking_IdAndOperation(
				booking.getId(),
				GoogleCalendarSyncJob.OPERATION_CREATE_EVENT)) {
			return;
		}
		String providerEventId = booking.getCalendarEventId() == null
				? "mock-live-speaking-" + booking.getId()
				: booking.getCalendarEventId();
		String meetUrl = booking.getMeetUrl() == null
				? "https://meet.google.com/mock-" + booking.getId()
				: booking.getMeetUrl();
		syncJobRepository.save(GoogleCalendarSyncJob.completedCreateEvent(booking, providerEventId, meetUrl, now()));
	}

	private void requireReservedAndActive(SpeakingSessionBooking booking) {
		if (!SpeakingSessionBookingStatuses.RESERVED.equals(booking.getStatus())) {
			throw bookingStatusConflict("Booking is not ready for payment.");
		}
		if (isHoldExpired(booking)) {
			booking.expire();
			booking.getSlot().releaseReservation();
			throw bookingStatusConflict("Booking hold has expired.");
		}
	}

	private boolean isHoldExpired(SpeakingSessionBooking booking) {
		return booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isBefore(now());
	}

	private void requireMutableSlot(SpeakingSessionSlot slot) {
		if (!SpeakingSessionSlotStatuses.DRAFT.equals(slot.getStatus())
				&& !SpeakingSessionSlotStatuses.ANNOUNCED.equals(slot.getStatus())) {
			throw slotUnavailable("Only draft or announced slots can be changed.");
		}
	}

	private void requireNoTeacherOverlap(SpeakingSessionSlot slot) {
		Long slotId = slot.getId() == null ? -1L : slot.getId();
		if (slotRepository.existsTeacherOverlap(
				slot.getTeacherProfile().getId(),
				slotId,
				slot.getStartsAt(),
				slot.getEndsAt())) {
			throw slotUnavailable("Teacher has an overlapping announced, reserved, or booked slot.");
		}
	}

	private TeacherProfile requireApprovedTeacherProfile(User teacher) {
		TeacherProfile profile = teacherProfileRepository.findByUserId(teacher.getId())
				.orElseThrow(this::teacherProfileNotFound);
		if (!TeacherProfileStatuses.APPROVED.equals(profile.getStatus())) {
			throw teacherApprovalRequired();
		}
		return profile;
	}

	private SlotValues validateSlotValues(
			OffsetDateTime startsAt,
			OffsetDateTime endsAt,
			String timezone,
			BigDecimal priceAmount,
			String currency
	) {
		if (startsAt == null || endsAt == null) {
			throw validationFailed("Slot start and end are required.");
		}
		if (!endsAt.isAfter(startsAt)) {
			throw validationFailed("Slot end must be after start.");
		}
		String normalizedTimezone = trimToNull(timezone);
		if (normalizedTimezone == null || normalizedTimezone.length() > 80) {
			throw validationFailed("Timezone is required.");
		}
		if (priceAmount == null || priceAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw validationFailed("Price must be non-negative.");
		}
		return new SlotValues(
				startsAt.withOffsetSameInstant(ZoneOffset.UTC),
				endsAt.withOffsetSameInstant(ZoneOffset.UTC),
				normalizedTimezone,
				money(priceAmount),
				normalizeCurrency(currency));
	}

	private String normalizeSlotStatus(String status) {
		String normalized = lowerTrim(status);
		if (normalized != null && !SpeakingSessionSlotStatuses.isSupported(normalized)) {
			throw validationFailed("Unsupported slot status.");
		}
		return normalized;
	}

	private String normalizeBookingStatus(String status) {
		String normalized = lowerTrim(status);
		if (normalized != null && !SpeakingSessionBookingStatuses.isSupported(normalized)) {
			throw validationFailed("Unsupported booking status.");
		}
		return normalized;
	}

	private String normalizeCurrency(String currency) {
		String normalized = trimToNull(currency);
		if (normalized == null || normalized.length() != 3) {
			throw validationFailed("Currency is invalid.");
		}
		return normalized.toUpperCase(Locale.ROOT);
	}

	private BigDecimal money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private Long requireId(Long id, String label) {
		if (id == null || id < 1) {
			throw validationFailed(label + " is required.");
		}
		return id;
	}

	private void notifyUser(Long userId, String title, String body, Map<String, Object> payload) {
		Map<String, Object> safePayload = new LinkedHashMap<>(payload);
		notificationService.create(new NotificationCDO(userId, NotificationTypes.PAYMENT, title, body, safePayload));
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private String lowerTrim(String value) {
		String trimmed = trimToNull(value);
		return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private BusinessException slotNotFound() {
		return new BusinessException("SCHEDULE_SLOT_NOT_FOUND", "Schedule slot was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException slotUnavailable(String message) {
		return new BusinessException("SCHEDULE_SLOT_UNAVAILABLE", message, HttpStatus.CONFLICT);
	}

	private BusinessException bookingNotFound() {
		return new BusinessException("SCHEDULE_BOOKING_NOT_FOUND", "Schedule booking was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException bookingStatusConflict(String message) {
		return new BusinessException("SCHEDULE_BOOKING_STATUS_CONFLICT", message, HttpStatus.CONFLICT);
	}

	private BusinessException teacherProfileNotFound() {
		return new BusinessException("TEACHER_PROFILE_NOT_FOUND", "Teacher profile was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException teacherApprovalRequired() {
		return new BusinessException(
				"TEACHER_APPROVAL_REQUIRED",
				"Teacher profile must be approved before managing live speaking schedules.",
				HttpStatus.FORBIDDEN);
	}

	private record SlotValues(
			OffsetDateTime startsAt,
			OffsetDateTime endsAt,
			String timezone,
			BigDecimal priceAmount,
			String currency
	) {
	}
}
