package com.mockexams.backend.service;

import com.mockexams.backend.users.entity.User;

public interface StudentProgressService {

	StudentProgressDashboard dashboard(User student, int days);
}
