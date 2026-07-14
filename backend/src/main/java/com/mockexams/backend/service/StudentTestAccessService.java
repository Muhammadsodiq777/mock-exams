package com.mockexams.backend.service;

import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.users.entity.User;

public interface StudentTestAccessService {

	void requireStartAccess(User student, CatalogTest test);
}
