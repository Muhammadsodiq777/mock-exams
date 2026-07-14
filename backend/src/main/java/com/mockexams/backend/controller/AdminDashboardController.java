package com.mockexams.backend.controller;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.AdminDashboardRDO;
import com.mockexams.backend.service.AdminDashboard;
import com.mockexams.backend.service.AdminDashboardService;
import com.mockexams.backend.users.RoleCodes;

@RestController
@Validated
@RequestMapping("/api/v1/admin/dashboard")
class AdminDashboardController {

	private final AuthorizationService authorizationService;
	private final AdminDashboardService adminDashboardService;

	AdminDashboardController(
			AuthorizationService authorizationService,
			AdminDashboardService adminDashboardService
	) {
		this.authorizationService = authorizationService;
		this.adminDashboardService = adminDashboardService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<AdminDashboardRDO>> dashboard(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		AdminDashboard dashboard = adminDashboardService.dashboard(from, to);
		return ResponseEntity.ok(ApiEnvelope.of(
				dashboard.data(),
				ApiMeta.dashboard(dashboard.asOf(), dashboard.window())));
	}
}
