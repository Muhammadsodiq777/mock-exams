package com.mockexams.backend.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_plan_tasks")
public class StudyPlanTask {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "study_plan_id", nullable = false)
	private StudyPlan studyPlan;

	@Column(nullable = false)
	private LocalDate taskDate;

	@Column(nullable = false, length = 32)
	private String skill;

	@Column(nullable = false, length = 180)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Column(nullable = false)
	private int estimatedMinutes;

	@Column(length = 32)
	private String recommendedResourceType;

	@Column(length = 180)
	private String recommendedResourceTitle;

	private Long recommendedTestId;

	private Long recommendedListingId;

	@Column(columnDefinition = "text")
	private String rationale;

	private OffsetDateTime completedAt;

	@Column(nullable = false)
	private int taskOrder;

	protected StudyPlanTask() {
	}

	public static StudyPlanTask create(
			StudyPlan studyPlan,
			LocalDate taskDate,
			String skill,
			String title,
			String description,
			int estimatedMinutes,
			String recommendedResourceType,
			String recommendedResourceTitle,
			Long recommendedTestId,
			Long recommendedListingId,
			String rationale,
			int taskOrder
	) {
		StudyPlanTask task = new StudyPlanTask();
		task.studyPlan = studyPlan;
		task.taskDate = taskDate;
		task.skill = skill;
		task.title = title;
		task.description = description;
		task.estimatedMinutes = estimatedMinutes;
		task.recommendedResourceType = recommendedResourceType;
		task.recommendedResourceTitle = recommendedResourceTitle;
		task.recommendedTestId = recommendedTestId;
		task.recommendedListingId = recommendedListingId;
		task.rationale = rationale;
		task.taskOrder = taskOrder;
		return task;
	}

	public void markCompleted(OffsetDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public void markIncomplete() {
		this.completedAt = null;
	}

	public boolean isCompleted() {
		return completedAt != null;
	}

	public Long getId() {
		return id;
	}

	public StudyPlan getStudyPlan() {
		return studyPlan;
	}

	public LocalDate getTaskDate() {
		return taskDate;
	}

	public String getSkill() {
		return skill;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public int getEstimatedMinutes() {
		return estimatedMinutes;
	}

	public String getRecommendedResourceType() {
		return recommendedResourceType;
	}

	public String getRecommendedResourceTitle() {
		return recommendedResourceTitle;
	}

	public Long getRecommendedTestId() {
		return recommendedTestId;
	}

	public Long getRecommendedListingId() {
		return recommendedListingId;
	}

	public String getRationale() {
		return rationale;
	}

	public OffsetDateTime getCompletedAt() {
		return completedAt;
	}

	public int getTaskOrder() {
		return taskOrder;
	}
}
