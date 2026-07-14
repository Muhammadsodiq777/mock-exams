package com.mockexams.backend.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.AdminPaymentDisputeRDO;
import com.mockexams.backend.dto.PaymentDisputeRDO;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.entity.PaymentDispute;
import com.mockexams.backend.teachers.entity.TeacherProfile;

@Component
public class PaymentDisputeMapper {

	public PaymentDisputeRDO toRDO(PaymentDispute dispute) {
		Payment payment = dispute.getPayment();
		return new PaymentDisputeRDO(
				dispute.getId(),
				payment.getId(),
				payment.getStatus(),
				payment.getTargetType(),
				payment.getTargetLiveSpeakingBookingId(),
				dispute.getStatus(),
				dispute.getReason(),
				dispute.getDescription(),
				dispute.getResolutionNote(),
				dispute.getReviewedByUserId(),
				dispute.getReviewedAt(),
				dispute.getCreatedAt(),
				dispute.getUpdatedAt());
	}

	public AdminPaymentDisputeRDO toAdminRDO(PaymentDispute dispute) {
		Payment payment = dispute.getPayment();
		MarketplaceListing listing = payment.getTargetListing();
		TeacherProfile teacherProfile = listing == null ? null : listing.getTeacherProfile();
		return new AdminPaymentDisputeRDO(
				dispute.getId(),
				payment.getId(),
				payment.getStatus(),
				payment.getAmount(),
				payment.getCurrency(),
				payment.getTargetType(),
				listing == null ? null : listing.getId(),
				payment.getTargetLiveSpeakingBookingId(),
				dispute.getOpenedByUser().getId(),
				dispute.getOpenedByUser().getFullName(),
				teacherProfile == null ? null : teacherProfile.getId(),
				teacherProfile == null ? null : teacherProfile.getDisplayName(),
				dispute.getStatus(),
				dispute.getReason(),
				dispute.getDescription(),
				dispute.getResolutionNote(),
				dispute.getReviewedByUserId(),
				dispute.getReviewedAt(),
				dispute.getCreatedAt(),
				dispute.getUpdatedAt());
	}
}
