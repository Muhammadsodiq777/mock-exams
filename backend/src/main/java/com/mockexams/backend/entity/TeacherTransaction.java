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

import com.mockexams.backend.entity.schedules.SpeakingSessionBooking;

@Entity
@Table(name = "teacher_transactions")
public class TeacherTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "wallet_id")
	private TeacherWallet wallet;

	private Long purchaseId;

	private Long paymentId;

	private Long withdrawalRequestId;

	@Column(nullable = false, length = 24)
	private String transactionType;

	@Column(length = 40)
	private String sourceType;

	@Column(length = 160)
	private String sourceReference;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal grossAmount;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal platformCommissionAmount = BigDecimal.ZERO;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal netAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(length = 255)
	private String description;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	protected TeacherTransaction() {
	}

	public static TeacherTransaction earning(
			TeacherWallet wallet,
			Purchase purchase,
			BigDecimal commissionAmount,
			BigDecimal netAmount,
			String sourceReference,
			String description
	) {
		TeacherTransaction transaction = base(
				wallet,
				TeacherTransactionTypes.EARNING,
				TeacherTransactionSources.MARKETPLACE_PURCHASE,
				sourceReference,
				purchase.getAmount(),
				commissionAmount,
				netAmount,
				purchase.getCurrency(),
				description);
		transaction.purchaseId = purchase.getId();
		transaction.paymentId = purchase.getPaymentId();
		return transaction;
	}

	public static TeacherTransaction liveSpeakingEarning(
			TeacherWallet wallet,
			SpeakingSessionBooking booking,
			Long paymentId,
			BigDecimal commissionAmount,
			BigDecimal netAmount,
			String sourceReference,
			String description
	) {
		TeacherTransaction transaction = base(
				wallet,
				TeacherTransactionTypes.EARNING,
				TeacherTransactionSources.LIVE_SPEAKING_BOOKING,
				sourceReference,
				booking.getPriceAmount(),
				commissionAmount,
				netAmount,
				booking.getCurrency(),
				description);
		transaction.paymentId = paymentId;
		return transaction;
	}

	public static TeacherTransaction withdrawal(
			TeacherWallet wallet,
			BigDecimal amount,
			String currency,
			String sourceReference,
			String description
	) {
		return base(
				wallet,
				TeacherTransactionTypes.WITHDRAWAL,
				TeacherTransactionSources.WITHDRAWAL_REQUEST,
				sourceReference,
				amount,
				BigDecimal.ZERO,
				amount,
				currency,
				description);
	}

	public static TeacherTransaction refund(
			TeacherWallet wallet,
			Purchase purchase,
			TeacherTransaction earning,
			String sourceReference,
			String description
	) {
		TeacherTransaction transaction = base(
				wallet,
				TeacherTransactionTypes.REFUND,
				TeacherTransactionSources.REFUND,
				sourceReference,
				earning.getGrossAmount(),
				earning.getPlatformCommissionAmount(),
				earning.getNetAmount(),
				purchase.getCurrency(),
				description);
		transaction.purchaseId = purchase.getId();
		transaction.paymentId = purchase.getPaymentId();
		return transaction;
	}

	public static TeacherTransaction adjustment(
			TeacherWallet wallet,
			BigDecimal amount,
			String currency,
			String sourceReference,
			String description
	) {
		return base(
				wallet,
				TeacherTransactionTypes.ADJUSTMENT,
				TeacherTransactionSources.ADJUSTMENT,
				sourceReference,
				amount,
				BigDecimal.ZERO,
				amount,
				currency,
				description);
	}

	private static TeacherTransaction base(
			TeacherWallet wallet,
			String transactionType,
			String sourceType,
			String sourceReference,
			BigDecimal grossAmount,
			BigDecimal commissionAmount,
			BigDecimal netAmount,
			String currency,
			String description
	) {
		TeacherTransaction transaction = new TeacherTransaction();
		transaction.wallet = wallet;
		transaction.transactionType = transactionType;
		transaction.sourceType = sourceType;
		transaction.sourceReference = sourceReference;
		transaction.grossAmount = grossAmount;
		transaction.platformCommissionAmount = commissionAmount;
		transaction.netAmount = netAmount;
		transaction.currency = currency;
		transaction.description = description;
		return transaction;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now(ZoneOffset.UTC);
		}
	}

	public Long getId() {
		return id;
	}

	public TeacherWallet getWallet() {
		return wallet;
	}

	public Long getPurchaseId() {
		return purchaseId;
	}

	public Long getPaymentId() {
		return paymentId;
	}

	public Long getWithdrawalRequestId() {
		return withdrawalRequestId;
	}

	public String getTransactionType() {
		return transactionType;
	}

	public String getSourceType() {
		return sourceType;
	}

	public String getSourceReference() {
		return sourceReference;
	}

	public BigDecimal getGrossAmount() {
		return grossAmount;
	}

	public BigDecimal getPlatformCommissionAmount() {
		return platformCommissionAmount;
	}

	public BigDecimal getNetAmount() {
		return netAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getDescription() {
		return description;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
