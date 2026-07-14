package com.mockexams.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.FileDownloadUrlRDO;
import com.mockexams.backend.dto.FileRDO;
import com.mockexams.backend.service.FileMetadataService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1")
class FileController {

	private final AuthorizationService authorizationService;
	private final FileMetadataService fileMetadataService;

	FileController(AuthorizationService authorizationService, FileMetadataService fileMetadataService) {
		this.authorizationService = authorizationService;
		this.fileMetadataService = fileMetadataService;
	}

	@PostMapping("/teacher/files")
	ResponseEntity<ApiEnvelope<FileRDO>> uploadTeacherFile(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam String purpose,
			@RequestParam MultipartFile file
	) {
		User owner = authorizationService.requireRole(authorizationHeader, RoleCodes.TEACHER);
		return ResponseEntity.status(201)
				.body(ApiEnvelope.of(fileMetadataService.uploadForTeacher(owner, purpose, file)));
	}

	@PostMapping("/student/files")
	ResponseEntity<ApiEnvelope<FileRDO>> uploadStudentFile(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam String purpose,
			@RequestParam MultipartFile file
	) {
		User owner = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.status(201)
				.body(ApiEnvelope.of(fileMetadataService.uploadForStudent(owner, purpose, file)));
	}

	@PostMapping("/admin/files")
	ResponseEntity<ApiEnvelope<FileRDO>> uploadAdminFile(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam String purpose,
			@RequestParam MultipartFile file
	) {
		User owner = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.status(201)
				.body(ApiEnvelope.of(fileMetadataService.uploadForAdmin(owner, purpose, file)));
	}

	@GetMapping("/files/{fileId}/download-url")
	ResponseEntity<ApiEnvelope<FileDownloadUrlRDO>> downloadUrl(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable Long fileId
	) {
		User requester = authorizationService.requireActiveUser(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(fileMetadataService.createDownloadUrl(requester, fileId)));
	}
}
