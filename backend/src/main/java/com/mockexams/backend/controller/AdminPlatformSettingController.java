package com.mockexams.backend.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.PlatformSettingRDO;
import com.mockexams.backend.dto.PlatformSettingUDO;
import com.mockexams.backend.service.PlatformSettingService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/admin/settings")
class AdminPlatformSettingController {

	private final AuthorizationService authorizationService;
	private final PlatformSettingService platformSettingService;

	AdminPlatformSettingController(
			AuthorizationService authorizationService,
			PlatformSettingService platformSettingService
	) {
		this.authorizationService = authorizationService;
		this.platformSettingService = platformSettingService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<PlatformSettingRDO>>> settings(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(platformSettingService.listSettings()));
	}

	@GetMapping("/{key}")
	ResponseEntity<ApiEnvelope<PlatformSettingRDO>> setting(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable String key
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(platformSettingService.getSetting(key)));
	}

	@PatchMapping("/{key}")
	ResponseEntity<ApiEnvelope<PlatformSettingRDO>> update(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable String key,
			@Valid @RequestBody PlatformSettingUDO command
	) {
		User admin = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(platformSettingService.updateSetting(admin, key, command.value())));
	}
}
