package com.mockexams.backend.entity;

import java.util.Set;

public final class PurchaseSources {

	public static final String FREE_LISTING = "free_listing";
	public static final String PAYMENT = "payment";
	public static final String MANUAL_ADMIN = "manual_admin";
	public static final String SUBSCRIPTION = "subscription";
	public static final String LEGACY = "legacy";

	private static final Set<String> SUPPORTED = Set.of(
			FREE_LISTING,
			PAYMENT,
			MANUAL_ADMIN,
			SUBSCRIPTION,
			LEGACY);

	private PurchaseSources() {
	}

	public static boolean isSupported(String source) {
		return SUPPORTED.contains(source);
	}
}
