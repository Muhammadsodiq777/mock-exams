package com.mockexams.backend.entity.schedules;

import java.util.Set;

public final class SpeakingSessionSlotStatuses {

	public static final String DRAFT = "draft";
	public static final String ANNOUNCED = "announced";
	public static final String RESERVED = "reserved";
	public static final String BOOKED = "booked";
	public static final String COMPLETED = "completed";
	public static final String CANCELLED = "cancelled";

	private static final Set<String> SUPPORTED = Set.of(DRAFT, ANNOUNCED, RESERVED, BOOKED, COMPLETED, CANCELLED);

	private SpeakingSessionSlotStatuses() {
	}

	public static boolean isSupported(String status) {
		return SUPPORTED.contains(status);
	}
}
