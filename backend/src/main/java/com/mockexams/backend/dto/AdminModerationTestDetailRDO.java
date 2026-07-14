package com.mockexams.backend.dto;

import java.util.Map;

/**
 * Admin-only moderation preview. Wraps the full teacher/builder read model (sections, rich
 * content, audio metadata, and answer keys) plus a re-run readiness validation report. This DTO
 * must never be reused by the public/student catalog detail path, which uses {@link TestDetailRDO}
 * and never exposes answer keys.
 */
public record AdminModerationTestDetailRDO(
		TeacherTestRDO test,
		boolean readyForDecision,
		Map<String, String> validationFindings
) {
}
