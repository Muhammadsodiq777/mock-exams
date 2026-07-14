package com.mockexams.backend.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.TeacherWalletRDO;
import com.mockexams.backend.dto.TeacherWalletTransactionRDO;
import com.mockexams.backend.users.entity.User;

public interface TeacherWalletService {

	TeacherWalletRDO myWallet(User teacher);

	Page<TeacherWalletTransactionRDO> myTransactions(User teacher, Pageable pageable);

	Optional<TeacherWalletTransactionRDO> recordMarketplacePurchaseEarning(Long purchaseId);

	Optional<TeacherWalletTransactionRDO> recordLiveSpeakingBookingEarning(Long bookingId, Long paymentId);

	TeacherWalletRDO reserveWithdrawal(Long teacherProfileId, BigDecimal amount, String currency, String sourceReference);

	TeacherWalletRDO releaseWithdrawalReservation(Long teacherProfileId, BigDecimal amount, String currency);

	TeacherWalletTransactionRDO recordApprovedWithdrawal(
			Long teacherProfileId,
			BigDecimal amount,
			String currency,
			String sourceReference);

	Optional<TeacherWalletTransactionRDO> recordMarketplacePurchaseRefund(Long purchaseId, String sourceReference);
}
