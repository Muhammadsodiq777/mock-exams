package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import com.mockexams.backend.teachers.entity.TeacherProfile;

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
@Table(name = "promo_codes")
public class PromoCode {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "teacher_profile_id")
	private TeacherProfile teacherProfile;

	@Column(nullable = false, length = 60)
	private String code;

	@Column(nullable = false, length = 60)
	private String codeNormalized;

	@Column(precision = 5, scale = 2)
	private BigDecimal discountPercent;

	@Column(precision = 14, scale = 2)
	private BigDecimal discountAmount;

	@Column(length = 3)
	private String currency;

	private OffsetDateTime startsAt;

	private OffsetDateTime expiresAt;

	private Integer usageLimit;

	@Column(nullable = false)
	private int usedCount;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected PromoCode() {
	}

	public static PromoCode create(
			TeacherProfile teacherProfile,
			String displayCode,
			BigDecimal discountPercent,
			BigDecimal discountAmount,
			String currency,
			OffsetDateTime startsAt,
			OffsetDateTime expiresAt,
			Integer usageLimit,
			boolean active
	) {
		PromoCode promoCode = new PromoCode();
		promoCode.teacherProfile = teacherProfile;
		promoCode.code = displayCode;
		promoCode.codeNormalized = normalize(displayCode);
		promoCode.discountPercent = discountPercent;
		promoCode.discountAmount = discountAmount;
		promoCode.currency = currency;
		promoCode.startsAt = startsAt;
		promoCode.expiresAt = expiresAt;
		promoCode.usageLimit = usageLimit;
		promoCode.usedCount = 0;
		promoCode.active = active;
		return promoCode;
	}

	public static String normalize(String code) {
		return code == null ? null : code.trim().toLowerCase(Locale.ROOT);
	}

	@PrePersist
	void prePersist() {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public void updateDiscount(BigDecimal discountPercent, BigDecimal discountAmount, String currency) {
		this.discountPercent = discountPercent;
		this.discountAmount = discountAmount;
		this.currency = currency;
	}

	public void updateWindow(OffsetDateTime startsAt, OffsetDateTime expiresAt) {
		this.startsAt = startsAt;
		this.expiresAt = expiresAt;
	}

	public void updateUsageLimit(Integer usageLimit) {
		this.usageLimit = usageLimit;
	}

	public void updateActive(boolean active) {
		this.active = active;
	}

	public boolean isPercentDiscount() {
		return discountPercent != null;
	}

	public boolean hasStartedAt(OffsetDateTime now) {
		return startsAt == null || !startsAt.isAfter(now);
	}

	public boolean isExpiredAt(OffsetDateTime now) {
		return expiresAt != null && !expiresAt.isAfter(now);
	}

	public boolean isExhausted() {
		return usageLimit != null && usedCount >= usageLimit;
	}

	public Long getId() {
		return id;
	}

	public TeacherProfile getTeacherProfile() {
		return teacherProfile;
	}

	public String getCode() {
		return code;
	}

	public String getCodeNormalized() {
		return codeNormalized;
	}

	public BigDecimal getDiscountPercent() {
		return discountPercent;
	}

	public BigDecimal getDiscountAmount() {
		return discountAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public OffsetDateTime getStartsAt() {
		return startsAt;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public Integer getUsageLimit() {
		return usageLimit;
	}

	public int getUsedCount() {
		return usedCount;
	}

	public boolean isActive() {
		return active;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
