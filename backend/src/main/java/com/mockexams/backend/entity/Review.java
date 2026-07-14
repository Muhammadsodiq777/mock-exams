package com.mockexams.backend.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.users.entity.User;
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
@Table(name = "reviews")
public class Review {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_user_id")
	private User student;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "teacher_profile_id")
	private TeacherProfile teacherProfile;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "test_id")
	private CatalogTest test;

	@Column(nullable = false)
	private Short rating;

	private String comment;

	private OffsetDateTime hiddenAt;

	private Long hiddenByUserId;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected Review() {
	}

	public static Review forTest(User student, CatalogTest test, int rating, String comment) {
		Review review = new Review();
		review.student = student;
		review.test = test;
		review.rating = (short) rating;
		review.comment = comment;
		return review;
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

	public void updateContent(int rating, String comment) {
		this.rating = (short) rating;
		this.comment = comment;
	}

	public void hide(Long adminUserId, OffsetDateTime now) {
		if (hiddenAt == null) {
			hiddenAt = now;
			hiddenByUserId = adminUserId;
		}
	}

	public void unhide() {
		hiddenAt = null;
		hiddenByUserId = null;
	}

	public boolean isHidden() {
		return hiddenAt != null;
	}

	public Long getId() {
		return id;
	}

	public User getStudent() {
		return student;
	}

	public TeacherProfile getTeacherProfile() {
		return teacherProfile;
	}

	public CatalogTest getTest() {
		return test;
	}

	public Short getRating() {
		return rating;
	}

	public String getComment() {
		return comment;
	}

	public OffsetDateTime getHiddenAt() {
		return hiddenAt;
	}

	public Long getHiddenByUserId() {
		return hiddenByUserId;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
