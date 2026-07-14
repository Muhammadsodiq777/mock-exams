package com.mockexams.backend.teachers;

import java.util.Set;

public final class TeacherProfileStatuses {

	public static final String DRAFT = "draft";
	public static final String PENDING_APPROVAL = "pending_approval";
	public static final String APPROVED = "approved";
	public static final String REJECTED = "rejected";
	public static final String SUSPENDED = "suspended";

	private static final Set<String> SUPPORTED = Set.of(
			DRAFT,
			PENDING_APPROVAL,
			APPROVED,
			REJECTED,
			SUSPENDED);

	private TeacherProfileStatuses() {
	}

	public static boolean isSupported(String status) {
		return SUPPORTED.contains(status);
	}
}
