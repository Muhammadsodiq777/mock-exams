package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;

import com.mockexams.backend.teachers.entity.TeacherProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tests")
public class CatalogTest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "teacher_profile_id")
	private TeacherProfile teacherProfile;

	@Column(nullable = false, length = 180)
	private String title;

	private String description;

	@Column(nullable = false, length = 32)
	private String type;

	@Column(length = 40)
	private String difficulty;

	@Column(precision = 2, scale = 1)
	private BigDecimal estimatedBandMin;

	@Column(precision = 2, scale = 1)
	private BigDecimal estimatedBandMax;

	@Column(nullable = false)
	private Integer durationMinutes;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal priceAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "is_free", nullable = false)
	private Boolean free;

	@Column(nullable = false, length = 20)
	private String language;

	@Column(nullable = false, length = 24)
	private String visibility;

	private String moderationNote;

	private OffsetDateTime publishedAt;

	private Long createdByUserId;

	private Long reviewedByUserId;

	private OffsetDateTime reviewedAt;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

	private OffsetDateTime deletedAt;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "test_tag_links",
			joinColumns = @JoinColumn(name = "test_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<TestTag> tags = new LinkedHashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "test_category_links",
			joinColumns = @JoinColumn(name = "test_id"),
			inverseJoinColumns = @JoinColumn(name = "category_id"))
	private Set<TestCategory> categories = new LinkedHashSet<>();

	protected CatalogTest() {
	}

	public static CatalogTest createDraft(
			TeacherProfile teacherProfile,
			Long createdByUserId,
			String title,
			String description,
			String type,
			String difficulty,
			BigDecimal estimatedBandMin,
			BigDecimal estimatedBandMax,
			Integer durationMinutes,
			BigDecimal priceAmount,
			String currency,
			Boolean free,
			String language
	) {
		CatalogTest test = new CatalogTest();
		test.teacherProfile = teacherProfile;
		test.createdByUserId = createdByUserId;
		test.visibility = TestVisibilities.DRAFT;
		test.updateMetadata(
				title,
				description,
				type,
				difficulty,
				estimatedBandMin,
				estimatedBandMax,
				durationMinutes,
				priceAmount,
				currency,
				free,
				language);
		return test;
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

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getType() {
		return type;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public BigDecimal getEstimatedBandMin() {
		return estimatedBandMin;
	}

	public BigDecimal getEstimatedBandMax() {
		return estimatedBandMax;
	}

	public Integer getDurationMinutes() {
		return durationMinutes;
	}

	public BigDecimal getPriceAmount() {
		return priceAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public Boolean isFree() {
		return free;
	}

	public String getLanguage() {
		return language;
	}

	public String getVisibility() {
		return visibility;
	}

	public String getModerationNote() {
		return moderationNote;
	}

	public OffsetDateTime getPublishedAt() {
		return publishedAt;
	}

	public Long getCreatedByUserId() {
		return createdByUserId;
	}

	public Long getReviewedByUserId() {
		return reviewedByUserId;
	}

	public OffsetDateTime getReviewedAt() {
		return reviewedAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public OffsetDateTime getDeletedAt() {
		return deletedAt;
	}

	public Set<TestTag> getTags() {
		return tags;
	}

	public Set<TestCategory> getCategories() {
		return categories;
	}

	public void updateMetadata(
			String title,
			String description,
			String type,
			String difficulty,
			BigDecimal estimatedBandMin,
			BigDecimal estimatedBandMax,
			Integer durationMinutes,
			BigDecimal priceAmount,
			String currency,
			Boolean free,
			String language
	) {
		this.title = title;
		this.description = description;
		this.type = type;
		this.difficulty = difficulty;
		this.estimatedBandMin = estimatedBandMin;
		this.estimatedBandMax = estimatedBandMax;
		this.durationMinutes = durationMinutes;
		this.priceAmount = priceAmount;
		this.currency = currency;
		this.free = free;
		this.language = language;
		if (TestVisibilities.REJECTED.equals(visibility)) {
			markDraft();
		}
	}

	public void markDraft() {
		visibility = TestVisibilities.DRAFT;
		clearModerationState();
	}

	public void submitForModeration() {
		visibility = TestVisibilities.SUBMITTED;
		clearModerationState();
	}

	/**
	 * Approves a submitted test: publishes it, stamps the reviewer/timestamp, and clears any prior
	 * rejection note. Only legal from {@code submitted}; any other current status is a status
	 * conflict the caller must check for before calling this method.
	 */
	public void approve(Long reviewerUserId, OffsetDateTime reviewedAt) {
		visibility = TestVisibilities.PUBLISHED;
		publishedAt = reviewedAt;
		this.reviewedByUserId = reviewerUserId;
		this.reviewedAt = reviewedAt;
		moderationNote = null;
	}

	/**
	 * Rejects a submitted test: stamps the reviewer/timestamp/reason and returns it to an editable
	 * rejected state. Only legal from {@code submitted}; any other current status is a status
	 * conflict the caller must check for before calling this method.
	 */
	public void reject(Long reviewerUserId, OffsetDateTime reviewedAt, String reason) {
		visibility = TestVisibilities.REJECTED;
		moderationNote = reason;
		this.reviewedByUserId = reviewerUserId;
		this.reviewedAt = reviewedAt;
	}

	public void markArchived() {
		visibility = TestVisibilities.ARCHIVED;
	}

	private void clearModerationState() {
		moderationNote = null;
		reviewedByUserId = null;
		reviewedAt = null;
	}
}
