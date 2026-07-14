package com.mockexams.backend.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import com.mockexams.backend.api.ApiPageMeta;
import com.mockexams.backend.api.ApiSortMeta;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.TeacherWalletRDO;
import com.mockexams.backend.dto.TeacherWalletTransactionRDO;
import com.mockexams.backend.service.TeacherWalletService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/teacher/wallet")
class TeacherWalletController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "createdAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final AuthorizationService authorizationService;
	private final TeacherWalletService teacherWalletService;

	TeacherWalletController(AuthorizationService authorizationService, TeacherWalletService teacherWalletService) {
		this.authorizationService = authorizationService;
		this.teacherWalletService = teacherWalletService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<TeacherWalletRDO>> wallet(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		User teacher = authorizationService.requireRole(authorizationHeader, RoleCodes.TEACHER);
		return ResponseEntity.ok(ApiEnvelope.of(teacherWalletService.myWallet(teacher)));
	}

	@GetMapping("/transactions")
	ResponseEntity<ApiEnvelope<List<TeacherWalletTransactionRDO>>> transactions(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		User teacher = authorizationService.requireRole(authorizationHeader, RoleCodes.TEACHER);
		Page<TeacherWalletTransactionRDO> transactions = teacherWalletService.myTransactions(
				teacher,
				PageRequest.of(page, size, Sort.by(SORT_DIRECTION, SORT_FIELD)));
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(transactions),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				Map.of());
		return ResponseEntity.ok(ApiEnvelope.of(transactions.getContent(), meta));
	}
}
