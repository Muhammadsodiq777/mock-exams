package com.mockexams.backend.service;

import java.time.OffsetDateTime;

public interface AdminDashboardService {

	AdminDashboard dashboard(OffsetDateTime from, OffsetDateTime to);
}
