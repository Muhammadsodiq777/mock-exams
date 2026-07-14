package com.mockexams.backend.entity;

import java.util.Set;

public final class NotificationTypes {

	public static final String SYSTEM = "system";
	public static final String TEST_MODERATION = "test_moderation";
	public static final String PAYMENT = "payment";
	public static final String WITHDRAWAL = "withdrawal";
	public static final String AI_FEEDBACK = "ai_feedback";
	public static final String ACCOUNT_STATUS = "account_status";

	private static final Set<String> SUPPORTED_TYPES = Set.of(
			SYSTEM,
			TEST_MODERATION,
			PAYMENT,
			WITHDRAWAL,
			AI_FEEDBACK,
			ACCOUNT_STATUS);

	private NotificationTypes() {
	}

	public static boolean isSupported(String type) {
		return SUPPORTED_TYPES.contains(type);
	}
}
