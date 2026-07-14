package com.mockexams.backend.entity;

public final class AiFeedbackStatuses {

	public static final String QUEUED = "queued";
	public static final String PROCESSING = "processing";
	public static final String COMPLETED = "completed";
	public static final String FAILED = "failed";

	private AiFeedbackStatuses() {
	}

	public static boolean isSupported(String status) {
		return QUEUED.equals(status)
				|| PROCESSING.equals(status)
				|| COMPLETED.equals(status)
				|| FAILED.equals(status);
	}
}
