package com.mockexams.backend.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.mockexams.backend.dto.AdminPromoCodeCDO;
import com.mockexams.backend.dto.PromoCodeRDO;
import com.mockexams.backend.dto.PromoCodeUDO;
import com.mockexams.backend.service.PromoCodeService;
import com.mockexams.backend.users.RoleCodes;

@RestController
@Validated
@RequestMapping("/api/v1/admin/promo-codes")
class AdminPromoCodeController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "createdAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final AuthorizationService authorizationService;
	private final PromoCodeService promoCodeService;

	AdminPromoCodeController(AuthorizationService authorizationService, PromoCodeService promoCodeService) {
		this.authorizationService = authorizationService;
		this.promoCodeService = promoCodeService;
	}

	@PostMapping
	ResponseEntity<ApiEnvelope<PromoCodeRDO>> create(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@Valid @RequestBody AdminPromoCodeCDO command
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(promoCodeService.adminCreate(command)));
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<PromoCodeRDO>>> codes(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) @Min(1) Long teacherProfileId,
			@RequestParam(required = false) Boolean active,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		Page<PromoCodeRDO> codes = promoCodeService.adminCodes(
				teacherProfileId,
				active,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc(SORT_FIELD), Sort.Order.desc("id"))));
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(codes),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				filters(active, teacherProfileId));
		return ResponseEntity.ok(ApiEnvelope.of(codes.getContent(), meta));
	}

	private Map<String, Object> filters(Boolean active, Long teacherProfileId) {
		Map<String, Object> filters = new LinkedHashMap<>();
		if (active != null) {
			filters.put("active", active);
		}
		if (teacherProfileId != null) {
			filters.put("teacherProfileId", teacherProfileId);
		}
		return filters;
	}

	@GetMapping("/{promoCodeId}")
	ResponseEntity<ApiEnvelope<PromoCodeRDO>> code(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long promoCodeId
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(promoCodeService.adminCode(promoCodeId)));
	}

	@PatchMapping("/{promoCodeId}")
	ResponseEntity<ApiEnvelope<PromoCodeRDO>> update(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long promoCodeId,
			@Valid @RequestBody PromoCodeUDO command
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(promoCodeService.adminUpdate(promoCodeId, command)));
	}
}
