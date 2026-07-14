package com.mockexams.backend.controller.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.api.ApiPageMeta;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.schedules.GoogleCalendarProviderFailureRDO;
import com.mockexams.backend.dto.schedules.ScheduleBookingRDO;
import com.mockexams.backend.service.schedules.SpeakingScheduleService;
import com.mockexams.backend.users.RoleCodes;

@RestController
@Validated
@RequestMapping("/api/v1/admin/schedule")
class AdminScheduleController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;

	private final AuthorizationService authorizationService;
	private final SpeakingScheduleService scheduleService;

	AdminScheduleController(AuthorizationService authorizationService, SpeakingScheduleService scheduleService) {
		this.authorizationService = authorizationService;
		this.scheduleService = scheduleService;
	}

	@GetMapping("/bookings")
	ResponseEntity<ApiEnvelope<List<ScheduleBookingRDO>>> bookings(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) @Min(1) Long teacherProfileId,
			@RequestParam(required = false) @Min(1) Long studentUserId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		Page<ScheduleBookingRDO> bookings = scheduleService.listAdminBookings(
				status,
				teacherProfileId,
				studentUserId,
				from,
				to,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		return ResponseEntity.ok(ApiEnvelope.of(bookings.getContent(), meta(bookings, filters(status, from, to))));
	}

	@GetMapping("/bookings/{bookingId}")
	ResponseEntity<ApiEnvelope<ScheduleBookingRDO>> booking(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long bookingId
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(scheduleService.getAdminBooking(bookingId)));
	}

	@GetMapping("/provider-failures")
	ResponseEntity<ApiEnvelope<List<GoogleCalendarProviderFailureRDO>>> providerFailures(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		Page<GoogleCalendarProviderFailureRDO> failures = scheduleService.listProviderFailures(
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		return ResponseEntity.ok(ApiEnvelope.of(failures.getContent(), meta(failures, Map.of("status", "failed"))));
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
