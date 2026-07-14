package com.mockexams.backend.controller.student;

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
import com.mockexams.backend.dto.PaymentCDO;
import com.mockexams.backend.dto.PaymentRDO;
import com.mockexams.backend.dto.schedules.ScheduleBookingPaymentCDO;
import com.mockexams.backend.dto.schedules.ScheduleBookingRDO;
import com.mockexams.backend.dto.schedules.ScheduleSlotRDO;
import com.mockexams.backend.entity.PaymentTargetTypes;
import com.mockexams.backend.service.PaymentService;
import com.mockexams.backend.service.schedules.SpeakingScheduleService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/student/schedule")
class StudentScheduleController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;

	private final AuthorizationService authorizationService;
	private final SpeakingScheduleService scheduleService;
	private final PaymentService paymentService;

	StudentScheduleController(
			AuthorizationService authorizationService,
			SpeakingScheduleService scheduleService,
			PaymentService paymentService
	) {
		this.authorizationService = authorizationService;
		this.scheduleService = scheduleService;
		this.paymentService = paymentService;
	}

	@GetMapping("/slots")
	ResponseEntity<ApiEnvelope<List<ScheduleSlotRDO>>> slots(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@RequestParam(required = false) @Min(1) Long teacherProfileId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		Page<ScheduleSlotRDO> slots = scheduleService.listPublicSlots(
				teacherProfileId,
				status,
				from,
				to,
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("startsAt"), Sort.Order.asc("id"))));
		return ResponseEntity.ok(ApiEnvelope.of(slots.getContent(), meta(slots, filters(status, from, to))));
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
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		Page<ScheduleBookingRDO> bookings = scheduleService.listStudentBookings(
				student,
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
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(scheduleService.getStudentBooking(student, bookingId)));
	}

	@PostMapping("/slots/{slotId}/reserve")
	ResponseEntity<ApiEnvelope<ScheduleBookingRDO>> reserveSlot(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long slotId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.of(scheduleService.reserveSlot(student, slotId)));
	}

	@PostMapping("/bookings/{bookingId}/payment")
	ResponseEntity<ApiEnvelope<PaymentRDO>> createBookingPayment(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long bookingId,
			@Valid @RequestBody(required = false) ScheduleBookingPaymentCDO command
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		PaymentCDO payment = new PaymentCDO(
				PaymentTargetTypes.LIVE_SPEAKING_BOOKING,
				null,
				null,
				bookingId,
				null,
				command == null ? null : command.returnUrl());
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.of(paymentService.createPayment(student, payment)));
	}

	@PostMapping("/bookings/{bookingId}/cancel")
	ResponseEntity<ApiEnvelope<ScheduleBookingRDO>> cancelBooking(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long bookingId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(scheduleService.cancelStudentBooking(student, bookingId)));
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
