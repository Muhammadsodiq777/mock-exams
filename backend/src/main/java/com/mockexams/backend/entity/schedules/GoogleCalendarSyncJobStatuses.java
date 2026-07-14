package com.mockexams.backend.entity.schedules;

import java.util.Set;

public final class GoogleCalendarSyncJobStatuses {

	public static final String PENDING = "pending";
	public static final String COMPLETED = "completed";
	public static final String FAILED = "failed";

	private static final Set<String> SUPPORTED = Set.of(PENDING, COMPLETED, FAILED);

	private GoogleCalendarSyncJobStatuses() {
	}

	public static boolean isSupported(String status) {
		return SUPPORTED.contains(status);
	}
}
