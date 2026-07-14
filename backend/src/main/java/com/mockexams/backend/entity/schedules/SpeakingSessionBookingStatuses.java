package com.mockexams.backend.entity.schedules;

import java.util.Set;

public final class SpeakingSessionBookingStatuses {

	public static final String RESERVED = "reserved";
	public static final String BOOKED = "booked";
	public static final String CANCELLED = "cancelled";
	public static final String EXPIRED = "expired";
	public static final String COMPLETED = "completed";
	public static final String NO_SHOW = "no_show";

	private static final Set<String> SUPPORTED = Set.of(RESERVED, BOOKED, CANCELLED, EXPIRED, COMPLETED, NO_SHOW);

	private SpeakingSessionBookingStatuses() {
	}

	public static boolean isSupported(String status) {
		return SUPPORTED.contains(status);
	}
}
