package com.mockexams.backend.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.AdminDashboardActivityRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.entity.WithdrawalRequest;
import com.mockexams.backend.users.entity.User;

@Component
public class AdminDashboardMapper {

	public AdminDashboardActivityRDO toUserRegisteredActivity(User user) {
		return new AdminDashboardActivityRDO(
				"user_registered",
				user.getId(),
				user.getFullName(),
				user.getCreatedAt());
	}

	public AdminDashboardActivityRDO toTestSubmittedActivity(CatalogTest test) {
		return new AdminDashboardActivityRDO(
				"test_submitted",
				test.getId(),
				test.getTitle(),
				test.getUpdatedAt());
	}

	public AdminDashboardActivityRDO toPaymentPaidActivity(Payment payment) {
		return new AdminDashboardActivityRDO(
				"payment_paid",
				payment.getId(),
				payment.getAmount() + " " + payment.getCurrency(),
				payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt());
	}

	public AdminDashboardActivityRDO toWithdrawalRequestedActivity(WithdrawalRequest request) {
		return new AdminDashboardActivityRDO(
				"withdrawal_requested",
				request.getId(),
				request.getRequestedAmount() + " " + request.getCurrency(),
				request.getCreatedAt());
	}
}
