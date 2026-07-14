package com.mockexams.backend.entity;

import java.util.Set;

public final class TestTypes {

	public static final String LISTENING = "listening";
	public static final String READING = "reading";
	public static final String WRITING_TASK_1 = "writing_task_1";
	public static final String WRITING_TASK_2 = "writing_task_2";
	public static final String SPEAKING_PART_1 = "speaking_part_1";
	public static final String SPEAKING_PART_2 = "speaking_part_2";
	public static final String SPEAKING_PART_3 = "speaking_part_3";
	public static final String FULL_MOCK = "full_mock";

	public static final Set<String> ALL = Set.of(
			LISTENING,
			READING,
			WRITING_TASK_1,
			WRITING_TASK_2,
			SPEAKING_PART_1,
			SPEAKING_PART_2,
			SPEAKING_PART_3,
			FULL_MOCK);

	private TestTypes() {
	}

	public static boolean isSupported(String type) {
		return ALL.contains(type);
	}
}
