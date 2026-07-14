package com.mockexams.backend.teachers.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.api.ApiPageMeta;
import com.mockexams.backend.api.ApiSortMeta;
import com.mockexams.backend.teachers.dto.TeacherProfilePublicRDO;
import com.mockexams.backend.teachers.service.TeacherProfileService;

@RestController
@Validated
@RequestMapping("/api/v1/public/teacher-profiles")
class PublicTeacherProfileController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "approvedAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final TeacherProfileService teacherProfileService;

	PublicTeacherProfileController(TeacherProfileService teacherProfileService) {
		this.teacherProfileService = teacherProfileService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<TeacherProfilePublicRDO>>> listApprovedProfiles(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(SORT_DIRECTION, SORT_FIELD));
		Page<TeacherProfilePublicRDO> profiles = teacherProfileService.listApprovedPublicProfiles(pageRequest);
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(profiles),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				Map.of("status", "approved"));
		return ResponseEntity.ok(ApiEnvelope.of(profiles.getContent(), meta));
	}

	@GetMapping("/{profileId}")
	ResponseEntity<ApiEnvelope<TeacherProfilePublicRDO>> publicProfile(@PathVariable Long profileId) {
		return ResponseEntity.ok(ApiEnvelope.of(teacherProfileService.publicProfile(profileId)));
	}
}
