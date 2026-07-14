package com.mockexams.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.AdminWithdrawalRequestRDO;
import com.mockexams.backend.dto.WithdrawalRequestCDO;
import com.mockexams.backend.dto.WithdrawalRequestRDO;
import com.mockexams.backend.users.entity.User;

public interface WithdrawalService {

	WithdrawalRequestRDO create(User teacher, WithdrawalRequestCDO command);

	Page<WithdrawalRequestRDO> myWithdrawals(User teacher, String status, Pageable pageable);

	WithdrawalRequestRDO myWithdrawal(User teacher, Long withdrawalRequestId);

	WithdrawalRequestRDO cancel(User teacher, Long withdrawalRequestId);

	Page<AdminWithdrawalRequestRDO> adminQueue(String status, Long teacherProfileId, Pageable pageable);

	AdminWithdrawalRequestRDO adminWithdrawal(Long withdrawalRequestId);

	AdminWithdrawalRequestRDO approve(User admin, Long withdrawalRequestId);

	AdminWithdrawalRequestRDO reject(User admin, Long withdrawalRequestId, String reason);

	AdminWithdrawalRequestRDO markPaid(User admin, Long withdrawalRequestId);
}
