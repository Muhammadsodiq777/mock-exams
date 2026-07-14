package com.mockexams.backend.service;

import java.time.OffsetDateTime;
import java.util.Map;

import com.mockexams.backend.dto.AdminDashboardRDO;

public record AdminDashboard(
		AdminDashboardRDO data,
		OffsetDateTime asOf,
		Map<String, Object> window
) {
}
