package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "promo_code_redemptions")
public class PromoCodeRedemption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "promo_code_id")
	private PromoCode promoCode;

	@Column(name = "student_user_id", nullable = false)
	private Long studentUserId;

	private Long purchaseId;

	private Long paymentId;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal discountAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(nullable = false)
	private OffsetDateTime redeemedAt;

	protected PromoCodeRedemption() {
	}

	public static PromoCodeRedemption forPayment(
			PromoCode promoCode,
			Long studentUserId,
			Long paymentId,
			BigDecimal discountAmount,
			String currency,
			OffsetDateTime redeemedAt
	) {
		PromoCodeRedemption redemption = new PromoCodeRedemption();
		redemption.promoCode = promoCode;
		redemption.studentUserId = studentUserId;
		redemption.paymentId = paymentId;
		redemption.discountAmount = discountAmount;
		redemption.currency = currency;
		redemption.redeemedAt = redeemedAt;
		return redemption;
	}

	@PrePersist
	void prePersist() {
		if (redeemedAt == null) {
			redeemedAt = OffsetDateTime.now(ZoneOffset.UTC);
		}
	}

	public Long getId() {
		return id;
	}

	public PromoCode getPromoCode() {
		return promoCode;
	}

	public Long getStudentUserId() {
		return studentUserId;
	}

	public Long getPurchaseId() {
		return purchaseId;
	}

	public Long getPaymentId() {
		return paymentId;
	}

	public BigDecimal getDiscountAmount() {
		return discountAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public OffsetDateTime getRedeemedAt() {
		return redeemedAt;
	}
}
