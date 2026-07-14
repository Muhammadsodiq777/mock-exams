package com.mockexams.backend.admin.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
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
import com.mockexams.backend.api.ApiSortMeta;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.teachers.dto.AdminTeacherProfileDetailRDO;
import com.mockexams.backend.teachers.dto.TeacherProfileModerationUDO;
import com.mockexams.backend.teachers.dto.TeacherProfileRDO;
import com.mockexams.backend.teachers.dto.TeacherProfileSuspensionUDO;
import com.mockexams.backend.teachers.service.TeacherProfileService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/admin/teacher-profiles")
class AdminTeacherProfileController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "createdAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final AuthorizationService authorizationService;
	private final TeacherProfileService teacherProfileService;

	AdminTeacherProfileController(
			AuthorizationService authorizationService,
			TeacherProfileService teacherProfileService
	) {
		this.authorizationService = authorizationService;
		this.teacherProfileService = teacherProfileService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<TeacherProfileRDO>>> listProfiles(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@RequestParam(required = false) String status
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(SORT_DIRECTION, SORT_FIELD));
		Page<TeacherProfileRDO> profiles = teacherProfileService.listAdminProfiles(status, pageRequest);
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(profiles),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				status == null || status.isBlank() ? Map.of() : Map.of("status", status));
		return ResponseEntity.ok(ApiEnvelope.of(profiles.getContent(), meta));
	}

	@GetMapping("/{profileId}")
	ResponseEntity<ApiEnvelope<AdminTeacherProfileDetailRDO>> profile(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long profileId
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(teacherProfileService.adminProfileDetail(profileId)));
	}

	@PostMapping("/{profileId}/approve")
	ResponseEntity<ApiEnvelope<TeacherProfileRDO>> approve(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long profileId
	) {
		User reviewer = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(teacherProfileService.approve(profileId, reviewer)));
	}

	@PostMapping("/{profileId}/reject")
	ResponseEntity<ApiEnvelope<TeacherProfileRDO>> reject(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long profileId,
			@Valid @RequestBody TeacherProfileModerationUDO command
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(teacherProfileService.reject(profileId, command)));
	}

	@PostMapping("/{profileId}/suspend")
	ResponseEntity<ApiEnvelope<TeacherProfileRDO>> suspend(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long profileId,
			@Valid @RequestBody(required = false) TeacherProfileSuspensionUDO command
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(teacherProfileService.suspend(profileId, command)));
	}

	@PostMapping("/{profileId}/reactivate")
	ResponseEntity<ApiEnvelope<TeacherProfileRDO>> reactivate(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long profileId
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(teacherProfileService.reactivate(profileId)));
	}
}
