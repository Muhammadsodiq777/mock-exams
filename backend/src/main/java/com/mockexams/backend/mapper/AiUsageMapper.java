package com.mockexams.backend.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.AdminAiUsageReportRDO;
import com.mockexams.backend.dto.AiUsageRemainingRDO;
import com.mockexams.backend.entity.AiFeedback;
import com.mockexams.backend.entity.AiUsageLimit;

@Component
public class AiUsageMapper {

	public AiUsageRemainingRDO toRemainingUsage(AiUsageLimit limit) {
		Integer hardLimit = limit.getHardLimitRequests();
		return new AiUsageRemainingRDO(
				limit.getPeriodStart(),
				limit.getPeriodEnd(),
				limit.getFreeRequestsLimit(),
				limit.getUsedRequests(),
				hardLimit,
				Math.max(limit.getFreeRequestsLimit() - limit.getUsedRequests(), 0),
				hardLimit == null ? null : Math.max(hardLimit - limit.getUsedRequests(), 0),
				limit.isHardLimitReached());
	}

	public AdminAiUsageReportRDO toAdminReport(AiFeedback feedback) {
		return new AdminAiUsageReportRDO(
				feedback.getId(),
				feedback.getStudentUser().getId(),
				feedback.getFeedbackType(),
				feedback.getStatus(),
				feedback.getProvider(),
				feedback.getModel(),
				feedback.getPromptTokens(),
				feedback.getCompletionTokens(),
				feedback.getEstimatedCost(),
				feedback.getFailureCode(),
				feedback.getErrorMessage(),
				feedback.getCreatedAt(),
				feedback.getCompletedAt());
	}
}
