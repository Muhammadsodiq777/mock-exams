package com.mockexams.backend.admin.controller;

import java.util.LinkedHashMap;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.admin.dto.AdminUserRDO;
import com.mockexams.backend.admin.dto.AdminUserStatusUDO;
import com.mockexams.backend.admin.service.AdminUserService;
import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.api.ApiPageMeta;
import com.mockexams.backend.api.ApiSortMeta;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.users.RoleCodes;

@RestController
@Validated
@RequestMapping("/api/v1/admin/users")
class AdminUserController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "createdAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final AuthorizationService authorizationService;
	private final AdminUserService adminUserService;

	AdminUserController(AuthorizationService authorizationService, AdminUserService adminUserService) {
		this.authorizationService = authorizationService;
		this.adminUserService = adminUserService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<AdminUserRDO>>> listUsers(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String role,
			@RequestParam(required = false) String teacherStatus,
			@RequestParam(required = false, name = "q") String email
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(SORT_DIRECTION, SORT_FIELD));
		Page<AdminUserRDO> users = adminUserService.listUsers(status, role, teacherStatus, email, pageRequest);
		Map<String, Object> filters = new LinkedHashMap<>();
		if (status != null && !status.isBlank()) {
			filters.put("status", status);
		}
		if (role != null && !role.isBlank()) {
			filters.put("role", role);
		}
		if (teacherStatus != null && !teacherStatus.isBlank()) {
			filters.put("teacherStatus", teacherStatus);
		}
		if (email != null && !email.isBlank()) {
			filters.put("q", email);
		}
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(users),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				filters);
		return ResponseEntity.ok(ApiEnvelope.of(users.getContent(), meta));
	}

	@GetMapping("/{userId}")
	ResponseEntity<ApiEnvelope<AdminUserRDO>> getUser(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long userId
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(adminUserService.getUser(userId)));
	}

	@PatchMapping("/{userId}/status")
	ResponseEntity<ApiEnvelope<AdminUserRDO>> updateStatus(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long userId,
			@Valid @RequestBody AdminUserStatusUDO command
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(adminUserService.updateStatus(userId, command)));
	}
}
