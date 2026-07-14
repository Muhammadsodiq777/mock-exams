package com.mockexams.backend.entity;

public final class TranscriptionStatuses {

	public static final String QUEUED = "queued";
	public static final String PROCESSING = "processing";
	public static final String COMPLETED = "completed";
	public static final String FAILED = "failed";

	private TranscriptionStatuses() {
	}

	public static boolean isActive(String status) {
		return QUEUED.equals(status) || PROCESSING.equals(status);
	}
}
