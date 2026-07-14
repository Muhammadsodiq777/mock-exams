package com.mockexams.backend.teachers.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher_profiles")
public class TeacherProfile {

	public static final String STATUS_PENDING_APPROVAL = TeacherProfileStatuses.PENDING_APPROVAL;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false, length = 160)
	private String displayName;

	@Column(length = 180)
	private String headline;

	private String bio;

	private Long avatarFileId;

	@Column(nullable = false, length = 32)
	private String status = STATUS_PENDING_APPROVAL;

	private Long approvedByUserId;

	private OffsetDateTime approvedAt;

	private String rejectionReason;

	@Column(nullable = false, precision = 3, scale = 2)
	private BigDecimal ratingAverage = BigDecimal.ZERO;

	@Column(nullable = false)
	private Integer ratingCount = 0;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected TeacherProfile() {
	}

	public TeacherProfile(User user, String displayName, String headline, String bio) {
		this.user = user;
		this.displayName = displayName;
		this.headline = headline;
		this.bio = bio;
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

	public String getDisplayName() {
		return displayName;
	}

	public String getHeadline() {
		return headline;
	}

	public String getBio() {
		return bio;
	}

	public String getStatus() {
		return status;
	}

	public User getUser() {
		return user;
	}

	public Long getAvatarFileId() {
		return avatarFileId;
	}

	public Long getApprovedByUserId() {
		return approvedByUserId;
	}

	public OffsetDateTime getApprovedAt() {
		return approvedAt;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public BigDecimal getRatingAverage() {
		return ratingAverage;
	}

	public Integer getRatingCount() {
		return ratingCount;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void updateProfile(String displayName, String headline, String bio, Long avatarFileId) {
		this.displayName = displayName;
		this.headline = headline;
		this.bio = bio;
		this.avatarFileId = avatarFileId;
		if (TeacherProfileStatuses.REJECTED.equals(status)) {
			status = TeacherProfileStatuses.DRAFT;
			clearModerationState();
		}
	}

	public void markDraft() {
		status = TeacherProfileStatuses.DRAFT;
		clearModerationState();
	}

	public void submitForApproval() {
		status = TeacherProfileStatuses.PENDING_APPROVAL;
		clearModerationState();
	}

	public void approve(Long reviewerUserId, OffsetDateTime approvedAt) {
		status = TeacherProfileStatuses.APPROVED;
		this.approvedByUserId = reviewerUserId;
		this.approvedAt = approvedAt;
		this.rejectionReason = null;
	}

	public void reject(String rejectionReason) {
		status = TeacherProfileStatuses.REJECTED;
		this.rejectionReason = rejectionReason;
		this.approvedByUserId = null;
		this.approvedAt = null;
	}

	public void suspend() {
		status = TeacherProfileStatuses.SUSPENDED;
	}

	public void reactivate() {
		status = TeacherProfileStatuses.APPROVED;
	}

	public void updateRating(BigDecimal ratingAverage, int ratingCount) {
		this.ratingAverage = ratingAverage;
		this.ratingCount = ratingCount;
	}

	private void clearModerationState() {
		approvedByUserId = null;
		approvedAt = null;
		rejectionReason = null;
	}
}
