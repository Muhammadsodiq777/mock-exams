package com.mockexams.backend.entity;

import java.util.Set;

public final class PaymentTargetTypes {

	public static final String MARKETPLACE_LISTING = "marketplace_listing";
	public static final String SUBSCRIPTION = "subscription";
	public static final String LIVE_SPEAKING_BOOKING = "live_speaking_booking";
	public static final String LEGACY = "legacy";

	private static final Set<String> SUPPORTED = Set.of(
			MARKETPLACE_LISTING,
			SUBSCRIPTION,
			LIVE_SPEAKING_BOOKING,
			LEGACY);

	private PaymentTargetTypes() {
	}

	public static boolean isSupported(String targetType) {
		return SUPPORTED.contains(targetType);
	}
}
