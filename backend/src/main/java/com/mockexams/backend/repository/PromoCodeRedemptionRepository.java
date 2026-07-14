package com.mockexams.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.PromoCodeRedemption;

public interface PromoCodeRedemptionRepository extends JpaRepository<PromoCodeRedemption, Long> {

	Optional<PromoCodeRedemption> findByPromoCode_IdAndPaymentId(Long promoCodeId, Long paymentId);

	long deleteByPromoCode_IdAndPaymentId(Long promoCodeId, Long paymentId);
}
