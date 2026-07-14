package com.mockexams.backend.service;

import java.time.OffsetDateTime;
import java.util.Map;

import com.mockexams.backend.dto.StudentProgressDashboardRDO;

public record StudentProgressDashboard(
		StudentProgressDashboardRDO data,
		OffsetDateTime asOf,
		Map<String, Object> window
) {
}
