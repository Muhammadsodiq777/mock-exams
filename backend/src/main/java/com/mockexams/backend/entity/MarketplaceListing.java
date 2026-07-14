package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
@Table(name = "marketplace_listings")
public class MarketplaceListing {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "teacher_profile_id")
	private TeacherProfile teacherProfile;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "test_id")
	private CatalogTest test;

	@Column(nullable = false, length = 180)
	private String title;

	private String description;

	@Column(nullable = false, length = 40)
	private String listingType;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal priceAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@Column(nullable = false, length = 20)
	private String language;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

	protected MarketplaceListing() {
	}

	public static MarketplaceListing draft(
			TeacherProfile teacherProfile,
			CatalogTest test,
			String title,
			String description,
			String listingType,
			BigDecimal priceAmount,
			String currency,
			String language
	) {
		MarketplaceListing listing = new MarketplaceListing();
		listing.teacherProfile = teacherProfile;
		listing.update(test, title, description, listingType, priceAmount, currency, language);
		listing.active = false;
		return listing;
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

	public Long getId() {
		return id;
	}

	public TeacherProfile getTeacherProfile() {
		return teacherProfile;
	}

	public Long getTeacherProfileId() {
		return teacherProfile == null ? null : teacherProfile.getId();
	}

	public CatalogTest getTest() {
		return test;
	}

	public Long getTestId() {
		return test == null ? null : test.getId();
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getListingType() {
		return listingType;
	}

	public BigDecimal getPriceAmount() {
		return priceAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public boolean isActive() {
		return active;
	}

	public String getLanguage() {
		return language;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void update(
			CatalogTest test,
			String title,
			String description,
			String listingType,
			BigDecimal priceAmount,
			String currency,
			String language
	) {
		this.test = test;
		this.title = title;
		this.description = description;
		this.listingType = listingType;
		this.priceAmount = priceAmount;
		this.currency = currency;
		this.language = language;
	}

	public void activate() {
		active = true;
	}

	public void deactivate() {
		active = false;
	}
}
