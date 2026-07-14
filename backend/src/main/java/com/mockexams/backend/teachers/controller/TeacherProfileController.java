package com.mockexams.backend.teachers.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.teachers.dto.TeacherProfileCDO;
import com.mockexams.backend.teachers.dto.TeacherProfileRDO;
import com.mockexams.backend.teachers.dto.TeacherProfileUDO;
import com.mockexams.backend.teachers.service.TeacherProfileService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/teacher/profile")
class TeacherProfileController {

	private final AuthorizationService authorizationService;
	private final TeacherProfileService teacherProfileService;

	TeacherProfileController(
			AuthorizationService authorizationService,
			TeacherProfileService teacherProfileService
	) {
		this.authorizationService = authorizationService;
		this.teacherProfileService = teacherProfileService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<TeacherProfileRDO>> myProfile(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		User teacher = authorizationService.requireRole(authorizationHeader, RoleCodes.TEACHER);
		return ResponseEntity.ok(ApiEnvelope.of(teacherProfileService.myProfile(teacher)));
	}

	@PostMapping
	ResponseEntity<ApiEnvelope<TeacherProfileRDO>> createMyProfile(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@Valid @RequestBody TeacherProfileCDO command
	) {
		User teacher = authorizationService.requireRole(authorizationHeader, RoleCodes.TEACHER);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(teacherProfileService.createMyProfile(teacher, command)));
	}

	@PatchMapping
	ResponseEntity<ApiEnvelope<TeacherProfileRDO>> updateMyProfile(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@Valid @RequestBody TeacherProfileUDO command
	) {
		User teacher = authorizationService.requireRole(authorizationHeader, RoleCodes.TEACHER);
		return ResponseEntity.ok(ApiEnvelope.of(teacherProfileService.updateMyProfile(teacher, command)));
	}

	@PostMapping("/submit")
	ResponseEntity<ApiEnvelope<TeacherProfileRDO>> submitMyProfile(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		User teacher = authorizationService.requireRole(authorizationHeader, RoleCodes.TEACHER);
		return ResponseEntity.ok(ApiEnvelope.of(teacherProfileService.submitMyProfile(teacher)));
	}
}
