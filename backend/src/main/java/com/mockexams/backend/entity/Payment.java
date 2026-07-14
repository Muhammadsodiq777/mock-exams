package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.mockexams.backend.users.entity.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

	public static final String FAILURE_CODE_AMOUNT_MISMATCH = "PAYMENT_AMOUNT_MISMATCH";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_user_id")
	private User student;

	@Column(nullable = false, length = 80)
	private String provider;

	@Column(length = 160)
	private String providerPaymentId;

	@Column(length = 500)
	private String providerCheckoutUrl;

	@Column(length = 160)
	private String providerEventId;

	@Column(nullable = false, length = 24)
	private String status;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(nullable = false, length = 40)
	private String targetType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "target_listing_id")
	private MarketplaceListing targetListing;

	private Long targetSubscriptionId;

	private Long targetLiveSpeakingBookingId;

	@Column(precision = 14, scale = 2)
	private BigDecimal providerAmount;

	@Column(length = 3)
	private String providerCurrency;

	@Column(length = 80)
	private String failureCode;

	@Column(length = 500)
	private String failureMessage;

	private Long promoCodeId;

	@Column(length = 60)
	private String promoCode;

	@Column(precision = 14, scale = 2)
	private BigDecimal promoDiscountAmount;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String metadata;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	private OffsetDateTime paidAt;

	private OffsetDateTime confirmationDispatchedAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected Payment() {
	}

	public static Payment pendingMarketplaceListing(
			User student,
			MarketplaceListing listing,
			String provider,
			String metadata,
			OffsetDateTime now
	) {
		Payment payment = new Payment();
		payment.student = student;
		payment.targetListing = listing;
		payment.targetType = PaymentTargetTypes.MARKETPLACE_LISTING;
		payment.provider = provider;
		payment.status = PaymentStatuses.PENDING;
		payment.amount = listing.getPriceAmount();
		payment.currency = listing.getCurrency();
		payment.metadata = metadata;
		payment.createdAt = now;
		payment.updatedAt = now;
		return payment;
	}

	public static Payment pendingLiveSpeakingBooking(
			User student,
			Long bookingId,
			BigDecimal amount,
			String currency,
			String provider,
			String metadata,
			OffsetDateTime now
	) {
		Payment payment = new Payment();
		payment.student = student;
		payment.targetLiveSpeakingBookingId = bookingId;
		payment.targetType = PaymentTargetTypes.LIVE_SPEAKING_BOOKING;
		payment.provider = provider;
		payment.status = PaymentStatuses.PENDING;
		payment.amount = amount;
		payment.currency = currency;
		payment.metadata = metadata;
		payment.createdAt = now;
		payment.updatedAt = now;
		return payment;
	}

	@PrePersist
	void prePersist() {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
		if (status == null) {
			status = PaymentStatuses.PENDING;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public boolean hasAmountMismatch(BigDecimal providerAmount, String providerCurrency) {
		return providerAmount == null
				|| providerCurrency == null
				|| amount.compareTo(providerAmount) != 0
				|| !currency.equalsIgnoreCase(providerCurrency);
	}

	public void applyPromoDiscount(Long promoCodeId, String promoCode, BigDecimal discountAmount) {
		this.promoCodeId = promoCodeId;
		this.promoCode = promoCode;
		this.promoDiscountAmount = discountAmount;
		this.amount = this.amount.subtract(discountAmount).max(BigDecimal.ZERO);
	}

	public void markPaidWithoutProvider(OffsetDateTime paidAt) {
		this.status = PaymentStatuses.PAID;
		this.paidAt = paidAt;
	}

	public void attachProviderPayment(String providerPaymentId, String checkoutUrl) {
		this.providerPaymentId = providerPaymentId;
		this.providerCheckoutUrl = checkoutUrl;
	}

	public void applyProviderEvent(
			String providerEventId,
			String status,
			BigDecimal providerAmount,
			String providerCurrency,
			OffsetDateTime occurredAt,
			String failureCode,
			String failureMessage
	) {
		this.providerEventId = providerEventId;
		this.providerAmount = providerAmount;
		this.providerCurrency = providerCurrency;
		if (PaymentStatuses.PENDING.equals(this.status)) {
			this.status = status;
			if (PaymentStatuses.PAID.equals(status)) {
				this.paidAt = occurredAt;
			}
			this.failureCode = failureCode;
			this.failureMessage = failureMessage;
			return;
		}
		if (PaymentStatuses.PAID.equals(this.status) && PaymentStatuses.REFUNDED.equals(status)) {
			this.status = PaymentStatuses.REFUNDED;
		}
	}

	public void flagAmountMismatch(BigDecimal providerAmount, String providerCurrency) {
		this.providerAmount = providerAmount;
		this.providerCurrency = providerCurrency;
		if (PaymentStatuses.PENDING.equals(status)) {
			status = PaymentStatuses.FAILED;
			failureCode = FAILURE_CODE_AMOUNT_MISMATCH;
			failureMessage = "Provider amount or currency did not match the payment snapshot.";
		}
	}

	public void markConfirmationDispatched(OffsetDateTime dispatchedAt) {
		this.confirmationDispatchedAt = dispatchedAt;
	}

	public Long getId() {
		return id;
	}

	public User getStudent() {
		return student;
	}

	public String getProvider() {
		return provider;
	}

	public String getProviderPaymentId() {
		return providerPaymentId;
	}

	public String getProviderCheckoutUrl() {
		return providerCheckoutUrl;
	}

	public String getProviderEventId() {
		return providerEventId;
	}

	public String getStatus() {
		return status;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getTargetType() {
		return targetType;
	}

	public MarketplaceListing getTargetListing() {
		return targetListing;
	}

	public Long getTargetSubscriptionId() {
		return targetSubscriptionId;
	}

	public Long getTargetLiveSpeakingBookingId() {
		return targetLiveSpeakingBookingId;
	}

	public BigDecimal getProviderAmount() {
		return providerAmount;
	}

	public String getProviderCurrency() {
		return providerCurrency;
	}

	public String getFailureCode() {
		return failureCode;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	public Long getPromoCodeId() {
		return promoCodeId;
	}

	public String getPromoCode() {
		return promoCode;
	}

	public BigDecimal getPromoDiscountAmount() {
		return promoDiscountAmount;
	}

	public String getMetadata() {
		return metadata;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getPaidAt() {
		return paidAt;
	}

	public OffsetDateTime getConfirmationDispatchedAt() {
		return confirmationDispatchedAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
