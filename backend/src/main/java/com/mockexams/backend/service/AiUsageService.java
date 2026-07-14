package com.mockexams.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.AdminAiUsageReportRDO;
import com.mockexams.backend.dto.AdminAiUsageReportTotalsRDO;
import com.mockexams.backend.dto.AiUsageRemainingRDO;

public interface AiUsageService {

	AiUsageReservation guardAndReserve(Long studentUserId, AiUsageRequest request);

	void recordSuccess(AiUsageReservation reservation, AiUsageAccounting accounting);

	void recordProviderFailure(AiUsageReservation reservation, String failureCode, String errorMessage);

	void recordValidationFailure(AiUsageReservation reservation, AiUsageAccounting accounting, String errorMessage);

	void recordValidationFailure(Long studentUserId, AiUsageRequest request, String errorMessage);

	BigDecimal estimateCost(int promptTokens, int completionTokens);

	AiUsageRemainingRDO remainingUsage(Long studentUserId);

	Page<AdminAiUsageReportRDO> listReport(AiUsageReportCriteria criteria, Pageable pageable);

	AdminAiUsageReportTotalsRDO summarizeReport(AiUsageReportCriteria criteria);

	List<String> supportedFeedbackTypes();
}
