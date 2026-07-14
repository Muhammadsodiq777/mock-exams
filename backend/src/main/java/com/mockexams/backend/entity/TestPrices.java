package com.mockexams.backend.entity;

import java.util.Set;

public final class TestPrices {

	public static final String FREE = "free";
	public static final String PAID = "paid";

	private static final Set<String> SUPPORTED = Set.of(FREE, PAID);

	private TestPrices() {
	}

	public static boolean isSupported(String price) {
		return SUPPORTED.contains(price);
	}
}
