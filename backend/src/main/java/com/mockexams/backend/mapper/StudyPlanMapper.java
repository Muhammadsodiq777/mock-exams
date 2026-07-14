package com.mockexams.backend.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.StudyPlanRDO;
import com.mockexams.backend.dto.StudyPlanResourceRDO;
import com.mockexams.backend.dto.StudyPlanTaskRDO;
import com.mockexams.backend.entity.StudyPlan;
import com.mockexams.backend.entity.StudyPlanTask;

@Component
public class StudyPlanMapper {

	public StudyPlanRDO toRDO(StudyPlan plan, List<StudyPlanTask> tasks) {
		return new StudyPlanRDO(
				plan.getId(),
				plan.getStudentUser().getId(),
				plan.getTargetBand(),
				plan.getCurrentBand(),
				plan.getDurationDays(),
				plan.getWeakestSkills(),
				plan.getPlanSummary(),
				plan.getStartsAt(),
				plan.getEndsAt(),
				plan.getCreatedByFeedbackId(),
				tasks.stream().map(this::toTaskRDO).toList(),
				plan.getCreatedAt(),
				plan.getUpdatedAt());
	}

	public StudyPlanTaskRDO toTaskRDO(StudyPlanTask task) {
		return new StudyPlanTaskRDO(
				task.getId(),
				task.getTaskDate(),
				task.getTaskOrder(),
				task.getSkill(),
				task.getTitle(),
				task.getDescription(),
				task.getEstimatedMinutes(),
				resource(task),
				task.getRationale(),
				task.isCompleted(),
				task.getCompletedAt());
	}

	private StudyPlanResourceRDO resource(StudyPlanTask task) {
		String type = task.getRecommendedResourceType();
		Long id = switch (type == null ? "" : type) {
			case "test" -> task.getRecommendedTestId();
			case "listing" -> task.getRecommendedListingId();
			default -> null;
		};
		return new StudyPlanResourceRDO(type, id, task.getRecommendedResourceTitle());
	}
}
