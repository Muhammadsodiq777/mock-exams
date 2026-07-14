package com.mockexams.backend.controller.teacher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.api.ApiPageMeta;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.schedules.ScheduleBookingRDO;
import com.mockexams.backend.dto.schedules.ScheduleSlotCDO;
import com.mockexams.backend.dto.schedules.ScheduleSlotRDO;
import com.mockexams.backend.dto.schedules.ScheduleSlotUDO;
import com.mockexams.backend.service.schedules.SpeakingScheduleService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/teacher/schedule")
class TeacherScheduleController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;

	private final AuthorizationService authorizationService;
	private final SpeakingScheduleService scheduleService;

	TeacherScheduleController(AuthorizationService authorizationService, SpeakingScheduleService scheduleService) {
		this.authorizationService = authorizationService;
		this.scheduleService = scheduleService;
	}

	@GetMapping("/slots")
	ResponseEntity<ApiEnvelope<List<ScheduleSlotRDO>>> slots(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
	) {
		User teacher = requireTeacher(authorizationHeader);
		Page<ScheduleSlotRDO> slots = scheduleService.listTeacherSlots(
				teacher,
				status,
				from,
				to,
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("startsAt"), Sort.Order.asc("id"))));
		return ResponseEntity.ok(ApiEnvelope.of(slots.getContent(), meta(slots, filters(status, from, to))));
	}

	@PostMapping("/slots")
	ResponseEntity<ApiEnvelope<ScheduleSlotRDO>> createSlot(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@Valid @RequestBody ScheduleSlotCDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(scheduleService.createTeacherSlot(teacher, command)));
	}

	@PatchMapping("/slots/{slotId}")
	ResponseEntity<ApiEnvelope<ScheduleSlotRDO>> updateSlot(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long slotId,
			@Valid @RequestBody ScheduleSlotUDO command
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(scheduleService.updateTeacherSlot(teacher, slotId, command)));
	}

	@PostMapping("/slots/{slotId}/announce")
	ResponseEntity<ApiEnvelope<ScheduleSlotRDO>> announceSlot(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long slotId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(scheduleService.announceTeacherSlot(teacher, slotId)));
	}

	@PostMapping("/slots/{slotId}/unannounce")
	ResponseEntity<ApiEnvelope<ScheduleSlotRDO>> unannounceSlot(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long slotId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(scheduleService.unannounceTeacherSlot(teacher, slotId)));
	}

	@GetMapping("/bookings")
	ResponseEntity<ApiEnvelope<List<ScheduleBookingRDO>>> bookings(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
	) {
		User teacher = requireTeacher(authorizationHeader);
		Page<ScheduleBookingRDO> bookings = scheduleService.listTeacherBookings(
				teacher,
				status,
				from,
				to,
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("slot.startsAt"), Sort.Order.asc("id"))));
		return ResponseEntity.ok(ApiEnvelope.of(bookings.getContent(), meta(bookings, filters(status, from, to))));
	}

	@GetMapping("/bookings/{bookingId}")
	ResponseEntity<ApiEnvelope<ScheduleBookingRDO>> booking(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long bookingId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(scheduleService.getTeacherBooking(teacher, bookingId)));
	}

	@PostMapping("/bookings/{bookingId}/complete")
	ResponseEntity<ApiEnvelope<ScheduleBookingRDO>> completeBooking(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long bookingId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(scheduleService.completeTeacherBooking(teacher, bookingId)));
	}

	@PostMapping("/bookings/{bookingId}/no-show")
	ResponseEntity<ApiEnvelope<ScheduleBookingRDO>> noShowBooking(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long bookingId
	) {
		User teacher = requireTeacher(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(scheduleService.markTeacherBookingNoShow(teacher, bookingId)));
	}

	private User requireTeacher(String authorizationHeader) {
		return authorizationService.requireRole(authorizationHeader, RoleCodes.TEACHER);
	}

	private ApiMeta meta(Page<?> page, Map<String, Object> filters) {
		return ApiMeta.create(ApiPageMeta.from(page), List.of(), filters);
	}

	private Map<String, Object> filters(String status, OffsetDateTime from, OffsetDateTime to) {
		return Map.of(
				"status", status == null ? "" : status,
				"from", from == null ? "" : from,
				"to", to == null ? "" : to);
	}
}
