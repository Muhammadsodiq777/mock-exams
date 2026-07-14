package com.mockexams.backend.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.PaymentRDO;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.Payment;

@Component
public class PaymentMapper {

	public PaymentRDO toRDO(Payment payment) {
		MarketplaceListing listing = payment.getTargetListing();
		return new PaymentRDO(
				payment.getId(),
				payment.getStudent().getId(),
				payment.getProvider(),
				payment.getProviderPaymentId(),
				payment.getProviderCheckoutUrl(),
				payment.getStatus(),
				payment.getAmount(),
				payment.getCurrency(),
				payment.getTargetType(),
				listing == null ? null : listing.getId(),
				listing == null ? null : listing.getTitle(),
				payment.getTargetSubscriptionId(),
				payment.getTargetLiveSpeakingBookingId(),
				payment.getPromoCode(),
				payment.getPromoDiscountAmount() == null
						? null
						: payment.getAmount().add(payment.getPromoDiscountAmount()),
				payment.getPromoDiscountAmount(),
				payment.getProviderAmount(),
				payment.getProviderCurrency(),
				payment.getFailureCode(),
				payment.getFailureMessage(),
				payment.getCreatedAt(),
				payment.getPaidAt(),
				payment.getConfirmationDispatchedAt(),
				payment.getUpdatedAt());
	}
}
