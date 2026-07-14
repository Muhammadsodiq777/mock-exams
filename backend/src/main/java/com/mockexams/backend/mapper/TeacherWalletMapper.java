package com.mockexams.backend.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.TeacherWalletRDO;
import com.mockexams.backend.dto.TeacherWalletTransactionRDO;
import com.mockexams.backend.entity.TeacherTransaction;
import com.mockexams.backend.entity.TeacherWallet;

@Component
public class TeacherWalletMapper {

	public TeacherWalletRDO toRDO(TeacherWallet wallet) {
		return new TeacherWalletRDO(
				wallet.getId(),
				wallet.getTeacherProfile().getId(),
				wallet.getBalanceAmount(),
				wallet.getPendingAmount(),
				wallet.getBalanceAmount(),
				wallet.getCurrency(),
				wallet.getCreatedAt(),
				wallet.getUpdatedAt());
	}

	public TeacherWalletTransactionRDO toRDO(TeacherTransaction transaction) {
		TeacherWallet wallet = transaction.getWallet();
		return new TeacherWalletTransactionRDO(
				transaction.getId(),
				wallet.getId(),
				wallet.getTeacherProfile().getId(),
				transaction.getPurchaseId(),
				transaction.getPaymentId(),
				transaction.getWithdrawalRequestId(),
				transaction.getTransactionType(),
				transaction.getSourceType(),
				transaction.getSourceReference(),
				transaction.getGrossAmount(),
				transaction.getPlatformCommissionAmount(),
				transaction.getNetAmount(),
				transaction.getCurrency(),
				transaction.getDescription(),
				transaction.getCreatedAt());
	}
}
