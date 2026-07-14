package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.mockexams.backend.users.entity.User;

@Entity
@Table(name = "study_plans")
public class StudyPlan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_user_id", nullable = false)
	private User studentUser;

	@Column(nullable = false, precision = 2, scale = 1)
	private BigDecimal targetBand;

	@Column(precision = 2, scale = 1)
	private BigDecimal currentBand;

	@Column(nullable = false)
	private int durationDays;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private List<String> weakestSkills = List.of();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> planSummary = new LinkedHashMap<>();

	@Column(nullable = false)
	private LocalDate startsAt;

	@Column(nullable = false)
	private LocalDate endsAt;

	private Long createdByFeedbackId;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	@OneToMany(mappedBy = "studyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<StudyPlanTask> tasks = new ArrayList<>();

	protected StudyPlan() {
	}

	public static StudyPlan create(
			User studentUser,
			BigDecimal targetBand,
			BigDecimal currentBand,
			int durationDays,
			List<String> weakestSkills,
			Map<String, Object> planSummary,
			LocalDate startsAt,
			Long createdByFeedbackId
	) {
		StudyPlan plan = new StudyPlan();
		plan.studentUser = studentUser;
		plan.targetBand = targetBand;
		plan.currentBand = currentBand;
		plan.durationDays = durationDays;
		plan.weakestSkills = copyList(weakestSkills);
		plan.planSummary = copyMap(planSummary);
		plan.startsAt = startsAt;
		plan.endsAt = startsAt.plusDays(durationDays - 1L);
		plan.createdByFeedbackId = createdByFeedbackId;
		return plan;
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

	public void refreshGeneratedContent(
			BigDecimal currentBand,
			List<String> weakestSkills,
			Map<String, Object> planSummary,
			Long createdByFeedbackId
	) {
		this.currentBand = currentBand;
		this.weakestSkills = copyList(weakestSkills);
		this.planSummary = copyMap(planSummary);
		this.createdByFeedbackId = createdByFeedbackId;
	}

	public boolean isActive(LocalDate today) {
		return !startsAt.isAfter(today) && !endsAt.isBefore(today);
	}

	private static List<String> copyList(List<String> value) {
		return value == null ? List.of() : List.copyOf(value);
	}

	private static Map<String, Object> copyMap(Map<String, Object> value) {
		return value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
	}

	public Long getId() {
		return id;
	}

	public User getStudentUser() {
		return studentUser;
	}

	public BigDecimal getTargetBand() {
		return targetBand;
	}

	public BigDecimal getCurrentBand() {
		return currentBand;
	}

	public int getDurationDays() {
		return durationDays;
	}

	public List<String> getWeakestSkills() {
		return weakestSkills;
	}

	public Map<String, Object> getPlanSummary() {
		return planSummary;
	}

	public LocalDate getStartsAt() {
		return startsAt;
	}

	public LocalDate getEndsAt() {
		return endsAt;
	}

	public Long getCreatedByFeedbackId() {
		return createdByFeedbackId;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
