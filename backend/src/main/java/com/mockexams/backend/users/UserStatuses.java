package com.mockexams.backend.users;

import java.util.Set;

public final class UserStatuses {

	public static final String ACTIVE = "active";
	public static final String SUSPENDED = "suspended";
	public static final String BANNED = "banned";
	public static final String DELETED = "deleted";

	public static final Set<String> ALL = Set.of(ACTIVE, SUSPENDED, BANNED, DELETED);

	private UserStatuses() {
	}

	public static boolean isSupported(String status) {
		return ALL.contains(status);
	}
}
