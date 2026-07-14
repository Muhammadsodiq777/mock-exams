package com.mockexams.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.AdminModerationQueueMetaRDO;
import com.mockexams.backend.dto.AdminModerationTestDetailRDO;
import com.mockexams.backend.dto.AdminModerationTestRDO;
import com.mockexams.backend.users.entity.User;

public interface TestModerationService {

	Page<AdminModerationTestRDO> queue(AdminModerationSearchCriteria criteria, Pageable pageable);

	AdminModerationQueueMetaRDO queueMeta();

	AdminModerationTestDetailRDO preview(Long testId);

	AdminModerationTestDetailRDO approve(User admin, Long testId);

	AdminModerationTestDetailRDO reject(User admin, Long testId, String reason);
}
