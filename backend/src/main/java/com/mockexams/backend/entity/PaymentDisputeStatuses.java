package com.mockexams.backend.entity;

import java.util.Set;

public final class PaymentDisputeStatuses {

	public static final String OPEN = "open";
	public static final String UNDER_REVIEW = "under_review";
	public static final String REFUNDED = "refunded";
	public static final String REJECTED = "rejected";
	public static final String RESOLVED = "resolved";

	private static final Set<String> SUPPORTED_STATUSES = Set.of(
			OPEN,
			UNDER_REVIEW,
			REFUNDED,
			REJECTED,
			RESOLVED);

	private static final Set<String> TERMINAL_STATUSES = Set.of(REFUNDED, REJECTED, RESOLVED);

	private static final Set<String> ADMIN_DECISION_STATUSES = Set.of(UNDER_REVIEW, REFUNDED, REJECTED, RESOLVED);

	private PaymentDisputeStatuses() {
	}

	public static boolean isSupported(String status) {
		return SUPPORTED_STATUSES.contains(status);
	}

	public static boolean isTerminal(String status) {
		return TERMINAL_STATUSES.contains(status);
	}

	/**
	 * Statuses an admin decision can move a dispute into. {@code open} is deliberately excluded: it is
	 * only the initial state produced by student creation, never a decision target.
	 */
	public static boolean isValidDecisionTarget(String status) {
		return ADMIN_DECISION_STATUSES.contains(status);
	}
}
