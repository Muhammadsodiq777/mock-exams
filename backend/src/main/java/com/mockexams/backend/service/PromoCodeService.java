package com.mockexams.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.AdminPromoCodeCDO;
import com.mockexams.backend.dto.PromoCodeCDO;
import com.mockexams.backend.dto.PromoCodeRDO;
import com.mockexams.backend.dto.PromoCodeUDO;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.users.entity.User;

public interface PromoCodeService {

	PromoCodeRDO create(User teacher, PromoCodeCDO command);

	Page<PromoCodeRDO> myCodes(User teacher, Boolean active, Pageable pageable);

	PromoCodeRDO myCode(User teacher, Long promoCodeId);

	PromoCodeRDO update(User teacher, Long promoCodeId, PromoCodeUDO command);

	PromoCodeRDO adminCreate(AdminPromoCodeCDO command);

	Page<PromoCodeRDO> adminCodes(Long teacherProfileId, Boolean active, Pageable pageable);

	PromoCodeRDO adminCode(Long promoCodeId);

	PromoCodeRDO adminUpdate(Long promoCodeId, PromoCodeUDO command);

	/**
	 * Validates a promo code against a payable listing and computes the discount.
	 * Throws a promo business error when the code cannot be applied. Does not consume usage.
	 */
	PromoApplication validateForListing(MarketplaceListing listing, String rawCode);

	/**
	 * Atomically consumes one usage of the code (guarded against the usage limit) and records
	 * one redemption row for the payment. Idempotent per promo code and payment.
	 */
	void redeemForPayment(Long promoCodeId, User student, Payment payment);

	/**
	 * Releases the usage previously consumed by {@link #redeemForPayment} for a payment that failed
	 * or was cancelled, so the usage limit and the redemption row no longer block a retry. Atomically
	 * decrements {@code usedCount} (never below zero) and removes the redemption row. Idempotent per
	 * promo code and payment: a second release for the same payment is a no-op.
	 */
	void releaseForPayment(Long promoCodeId, Payment payment);
}
