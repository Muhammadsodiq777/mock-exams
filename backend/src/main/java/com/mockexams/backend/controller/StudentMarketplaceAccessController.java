package com.mockexams.backend.controller;

import java.util.List;

import jakarta.validation.constraints.Min;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.AccessStatusRDO;
import com.mockexams.backend.dto.MarketplaceListingItemRDO;
import com.mockexams.backend.dto.PurchaseRDO;
import com.mockexams.backend.service.AccessControlService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/student")
class StudentMarketplaceAccessController {

	private final AuthorizationService authorizationService;
	private final AccessControlService accessControlService;

	StudentMarketplaceAccessController(
			AuthorizationService authorizationService,
			AccessControlService accessControlService
	) {
		this.authorizationService = authorizationService;
		this.accessControlService = accessControlService;
	}

	@GetMapping("/purchases")
	ResponseEntity<ApiEnvelope<List<PurchaseRDO>>> purchaseHistory(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		User student = requireStudent(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(accessControlService.purchaseHistory(student)));
	}

	@GetMapping("/purchases/{purchaseId}")
	ResponseEntity<ApiEnvelope<PurchaseRDO>> purchaseDetail(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long purchaseId
	) {
		User student = requireStudent(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(accessControlService.purchaseDetail(student, purchaseId)));
	}

	@PostMapping("/marketplace/listings/{listingId}/free-access")
	ResponseEntity<ApiEnvelope<PurchaseRDO>> grantFreeAccess(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long listingId
	) {
		User student = requireStudent(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(accessControlService.grantFreeListingAccess(student, listingId)));
	}

	@GetMapping("/marketplace/listings/{listingId}/access")
	ResponseEntity<ApiEnvelope<AccessStatusRDO>> accessStatus(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long listingId
	) {
		User student = requireStudent(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(accessControlService.listingAccessStatus(student, listingId)));
	}

	@GetMapping("/marketplace/listings/{listingId}/items")
	ResponseEntity<ApiEnvelope<List<MarketplaceListingItemRDO>>> accessibleItems(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long listingId
	) {
		User student = requireStudent(authorizationHeader);
		return ResponseEntity.ok(ApiEnvelope.of(accessControlService.accessibleListingItems(student, listingId)));
	}

	private User requireStudent(String authorizationHeader) {
		return authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
	}
}
