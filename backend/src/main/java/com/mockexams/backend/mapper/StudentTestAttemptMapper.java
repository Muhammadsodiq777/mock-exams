package com.mockexams.backend.mapper;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.StudentAnswerRDO;
import com.mockexams.backend.dto.StudentTestAttemptRDO;
import com.mockexams.backend.entity.StudentAnswer;
import com.mockexams.backend.entity.TestAttempt;

@Component
public class StudentTestAttemptMapper {

	public StudentTestAttemptRDO toRDO(TestAttempt attempt, OffsetDateTime now) {
		return new StudentTestAttemptRDO(
				attempt.getId(),
				attempt.getTest().getId(),
				attempt.getStudent().getId(),
				attempt.getStatus(),
				attempt.getStartedAt(),
				attempt.getSubmittedAt(),
				attempt.getExpiresAt(),
				attempt.getTest().getDurationMinutes(),
				attempt.remainingSeconds(now),
				attempt.getTimeSpentSeconds());
	}

	public StudentAnswerRDO toRDO(StudentAnswer answer) {
		Long selectedOptionId = answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId();
		return new StudentAnswerRDO(
				answer.getId(),
				answer.getAttempt().getId(),
				answer.getQuestion().getId(),
				answer.getAnswerText(),
				selectedOptionId,
				answer.getAudioFileId(),
				answer.getAnsweredAt());
	}
}
