package com.mockexams.backend.service.impl;

import org.springframework.stereotype.Service;

import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.service.AccessControlService;
import com.mockexams.backend.service.StudentTestAccessService;
import com.mockexams.backend.users.entity.User;

@Service
class FreePublishedStudentTestAccessService implements StudentTestAccessService {

	private final AccessControlService accessControlService;

	FreePublishedStudentTestAccessService(AccessControlService accessControlService) {
		this.accessControlService = accessControlService;
	}

	@Override
	public void requireStartAccess(User student, CatalogTest test) {
		accessControlService.requireTestAccess(student, test);
	}
}
