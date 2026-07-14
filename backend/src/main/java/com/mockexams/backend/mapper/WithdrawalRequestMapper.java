package com.mockexams.backend.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.AdminWithdrawalRequestRDO;
import com.mockexams.backend.dto.WithdrawalRequestRDO;
import com.mockexams.backend.entity.WithdrawalRequest;
import com.mockexams.backend.teachers.entity.TeacherProfile;

@Component
public class WithdrawalRequestMapper {

	private static final int VISIBLE_SUFFIX_LENGTH = 4;

	public WithdrawalRequestRDO toRDO(WithdrawalRequest request) {
		return new WithdrawalRequestRDO(
				request.getId(),
				request.getTeacherProfile().getId(),
				request.getRequestedAmount(),
				request.getCurrency(),
				request.getStatus(),
				maskedPayoutDetails(request.getPayoutDetails()),
				request.getReviewedByUserId(),
				request.getReviewedAt(),
				request.getRejectionReason(),
				request.getPaidAt(),
				request.getCreatedAt(),
				request.getUpdatedAt());
	}

	public AdminWithdrawalRequestRDO toAdminRDO(WithdrawalRequest request) {
		TeacherProfile teacherProfile = request.getTeacherProfile();
		return new AdminWithdrawalRequestRDO(
				request.getId(),
				teacherProfile.getId(),
				teacherProfile.getUser().getId(),
				teacherProfile.getDisplayName(),
				request.getRequestedAmount(),
				request.getCurrency(),
				request.getStatus(),
				maskedPayoutDetails(request.getPayoutDetails()),
				request.getReviewedByUserId(),
				request.getReviewedAt(),
				request.getRejectionReason(),
				request.getPaidAt(),
				request.getCreatedAt(),
				request.getUpdatedAt());
	}

	/**
	 * Masks sensitive payout values (card/account numbers) so RDOs never leak the full value stored
	 * in the persisted JSON. Only the last {@value #VISIBLE_SUFFIX_LENGTH} characters remain visible;
	 * method/holder/bank fields are left readable.
	 */
	private Map<String, Object> maskedPayoutDetails(Map<String, Object> payoutDetails) {
		if (payoutDetails == null || payoutDetails.isEmpty()) {
			return payoutDetails;
		}
		Map<String, Object> masked = new LinkedHashMap<>(payoutDetails);
		masked.computeIfPresent("cardNumber", (key, value) -> maskCardNumber(String.valueOf(value)));
		masked.computeIfPresent("accountNumber", (key, value) -> maskAccountNumber(String.valueOf(value)));
		return masked;
	}

	private String maskCardNumber(String cardNumber) {
		String suffix = lastCharacters(cardNumber, VISIBLE_SUFFIX_LENGTH);
		return "**** **** **** " + suffix;
	}

	private String maskAccountNumber(String accountNumber) {
		String suffix = lastCharacters(accountNumber, VISIBLE_SUFFIX_LENGTH);
		return "…" + suffix;
	}

	private String lastCharacters(String value, int count) {
		if (value.length() <= count) {
			return value;
		}
		return value.substring(value.length() - count);
	}
}
