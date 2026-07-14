package com.mockexams.backend.entity;

import java.util.Set;

public final class PaymentStatuses {

	public static final String PENDING = "pending";
	public static final String PAID = "paid";
	public static final String FAILED = "failed";
	public static final String CANCELLED = "cancelled";
	public static final String REFUNDED = "refunded";

	private static final Set<String> SUPPORTED = Set.of(PENDING, PAID, FAILED, CANCELLED, REFUNDED);

	private PaymentStatuses() {
	}

	public static boolean isSupported(String status) {
		return SUPPORTED.contains(status);
	}
}
