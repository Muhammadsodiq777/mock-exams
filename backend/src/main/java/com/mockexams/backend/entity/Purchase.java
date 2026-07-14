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
@Table(name = "purchases")
public class Purchase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_user_id")
	private User student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "listing_id")
	private MarketplaceListing listing;

	private Long paymentId;

	@Column(nullable = false, length = 24)
	private String status;

	@Column(nullable = false, length = 40)
	private String sourceType;

	@Column(length = 160)
	private String sourceReference;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String sourceMetadata;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(nullable = false)
	private OffsetDateTime purchasedAt;

	private OffsetDateTime accessExpiresAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected Purchase() {
	}

	public static Purchase grant(
			User student,
			MarketplaceListing listing,
			String sourceType,
			String sourceReference,
			String sourceMetadata,
			OffsetDateTime accessExpiresAt,
			OffsetDateTime now
	) {
		return grant(student, listing, sourceType, sourceReference, sourceMetadata, accessExpiresAt, null, null, now);
	}

	public static Purchase grant(
			User student,
			MarketplaceListing listing,
			String sourceType,
			String sourceReference,
			String sourceMetadata,
			OffsetDateTime accessExpiresAt,
			Long paymentId,
			BigDecimal amountPaid,
			OffsetDateTime now
	) {
		Purchase purchase = new Purchase();
		purchase.student = student;
		purchase.listing = listing;
		purchase.paymentId = paymentId;
		purchase.status = PurchaseStatuses.GRANTED;
		purchase.sourceType = sourceType;
		purchase.sourceReference = sourceReference;
		purchase.sourceMetadata = sourceMetadata;
		purchase.amount = amountPaid == null ? listing.getPriceAmount() : amountPaid;
		purchase.currency = listing.getCurrency();
		purchase.purchasedAt = now;
		purchase.accessExpiresAt = accessExpiresAt;
		purchase.updatedAt = now;
		return purchase;
	}

	@PrePersist
	void prePersist() {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (purchasedAt == null) {
			purchasedAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public boolean grantsAccessAt(OffsetDateTime now) {
		return PurchaseStatuses.GRANTED.equals(status)
				&& (accessExpiresAt == null || accessExpiresAt.isAfter(now));
	}

	public boolean isExpiredAt(OffsetDateTime now) {
		return PurchaseStatuses.GRANTED.equals(status)
				&& accessExpiresAt != null
				&& !accessExpiresAt.isAfter(now);
	}

	public Long getId() {
		return id;
	}

	public User getStudent() {
		return student;
	}

	public MarketplaceListing getListing() {
		return listing;
	}

	public Long getPaymentId() {
		return paymentId;
	}

	public String getStatus() {
		return status;
	}

	public String getSourceType() {
		return sourceType;
	}

	public String getSourceReference() {
		return sourceReference;
	}

	public String getSourceMetadata() {
		return sourceMetadata;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public OffsetDateTime getPurchasedAt() {
		return purchasedAt;
	}

	public OffsetDateTime getAccessExpiresAt() {
		return accessExpiresAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
