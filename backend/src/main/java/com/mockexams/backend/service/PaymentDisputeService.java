package com.mockexams.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.AdminPaymentDisputeRDO;
import com.mockexams.backend.dto.PaymentDisputeCDO;
import com.mockexams.backend.dto.PaymentDisputeDecisionCDO;
import com.mockexams.backend.dto.PaymentDisputeRDO;
import com.mockexams.backend.users.entity.User;

public interface PaymentDisputeService {

	PaymentDisputeRDO create(User student, PaymentDisputeCDO command);

	Page<PaymentDisputeRDO> myDisputes(User student, String status, Pageable pageable);

	PaymentDisputeRDO myDispute(User student, Long disputeId);

	Page<AdminPaymentDisputeRDO> adminQueue(AdminPaymentDisputeSearchCriteria criteria, Pageable pageable);

	AdminPaymentDisputeRDO adminDispute(Long disputeId);

	AdminPaymentDisputeRDO decide(User admin, Long disputeId, PaymentDisputeDecisionCDO command);
}
