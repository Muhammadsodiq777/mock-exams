package com.mockexams.backend.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import com.mockexams.backend.dto.PlatformSettingRDO;
import com.mockexams.backend.entity.FilePurpose;
import com.mockexams.backend.users.entity.User;

public interface PlatformSettingService {

	List<PlatformSettingRDO> listSettings();

	PlatformSettingRDO getSetting(String key);

	PlatformSettingRDO updateSetting(User admin, String key, JsonNode value);

	BigDecimal marketplaceCommissionPercent();

	BigDecimal platformCommissionPercent();

	BigDecimal liveSpeakingPlatformCommissionPercent();

	int aiFreeMonthlyRequestsLimit();

	Integer aiHardMonthlyRequestsLimit();

	String aiDefaultModel();

	String aiWritingRubricVersion();

	String aiSpeakingRubricVersion();

	BigDecimal withdrawalMinimumAmount();

	int richTextMaxDocumentBytes();

	Set<FilePurpose> richTextAllowedEmbedPurposes();

	Duration liveSpeakingStudentCancellationWindow();

	boolean googleIntegrationEnabled();

	String googleIntegrationMode();
}
