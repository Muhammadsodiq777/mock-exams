package com.mockexams.backend.service;

import com.mockexams.backend.dto.AdminAiUsageAggregateReportRDO;
import com.mockexams.backend.dto.AdminProviderFailureReportRDO;
import com.mockexams.backend.dto.AdminRevenueReportRDO;
import com.mockexams.backend.dto.AdminSalesReportRDO;
import com.mockexams.backend.dto.AdminTestActivityReportRDO;

public interface AdminReportService {

	AdminAiUsageAggregateReportRDO aiUsage(AdminReportCriteria criteria);

	AdminSalesReportRDO sales(AdminReportCriteria criteria);

	AdminRevenueReportRDO revenue(AdminReportCriteria criteria);

	AdminTestActivityReportRDO testActivity(AdminReportCriteria criteria);

	AdminProviderFailureReportRDO providerFailures(AdminReportCriteria criteria);
}
