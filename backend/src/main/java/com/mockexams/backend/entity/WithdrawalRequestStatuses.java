package com.mockexams.backend.entity;

import java.util.Set;

public final class WithdrawalRequestStatuses {

	public static final String REQUESTED = "requested";
	public static final String APPROVED = "approved";
	public static final String REJECTED = "rejected";
	public static final String PAID = "paid";
	public static final String CANCELLED = "cancelled";

	private static final Set<String> SUPPORTED_STATUSES = Set.of(
			REQUESTED,
			APPROVED,
			REJECTED,
			PAID,
			CANCELLED);

	private WithdrawalRequestStatuses() {
	}

	public static boolean isSupported(String status) {
		return SUPPORTED_STATUSES.contains(status);
	}
}
