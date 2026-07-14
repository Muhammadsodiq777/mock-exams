package com.mockexams.backend.entity;

import java.util.Arrays;

import org.springframework.http.HttpStatus;

import com.mockexams.backend.error.BusinessException;

public enum FilePurpose {

	LISTENING_AUDIO("listening_audio"),
	SPEAKING_AUDIO("speaking_audio"),
	TEACHER_AVATAR("teacher_avatar"),
	PASSAGE_IMAGE("passage_image"),
	DIAGRAM("diagram"),
	DOCUMENT("document"),
	COURSE_MATERIAL("course_material"),
	BANNER_IMAGE("banner_image");

	private final String value;

	FilePurpose(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static FilePurpose from(String value) {
		String normalizedValue = value == null ? null : value.trim().toLowerCase();
		return Arrays.stream(values())
				.filter(purpose -> purpose.value.equals(normalizedValue))
				.findFirst()
				.orElseThrow(() -> new BusinessException(
						"VALIDATION_FAILED",
						"Unsupported file purpose.",
						HttpStatus.BAD_REQUEST));
	}
}
