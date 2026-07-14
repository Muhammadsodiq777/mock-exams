package com.mockexams.backend.entity;

import java.util.Set;

public final class PurchaseStatuses {

	public static final String GRANTED = "granted";
	public static final String REVOKED = "revoked";
	public static final String REFUNDED = "refunded";

	private static final Set<String> SUPPORTED = Set.of(GRANTED, REVOKED, REFUNDED);

	private PurchaseStatuses() {
	}

	public static boolean isSupported(String status) {
		return SUPPORTED.contains(status);
	}
}
