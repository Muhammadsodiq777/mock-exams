package com.mockexams.backend.users;

import java.util.Set;

public final class RoleCodes {

	public static final String STUDENT = "student";
	public static final String TEACHER = "teacher";
	public static final String ADMIN = "admin";
	public static final String SCHOOL_MANAGER = "school_manager";
	public static final String MODERATOR = "moderator";

	private static final Set<String> ALL = Set.of(STUDENT, TEACHER, ADMIN, SCHOOL_MANAGER, MODERATOR);

	private RoleCodes() {
	}

	public static boolean isSupported(String roleCode) {
		return ALL.contains(roleCode);
	}
}
