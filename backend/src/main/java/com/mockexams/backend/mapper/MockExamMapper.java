package com.mockexams.backend.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.MockExamRDO;
import com.mockexams.backend.entity.MockExam;

@Component
public class MockExamMapper {

	public MockExamRDO toRDO(MockExam mockExam) {
		return new MockExamRDO(
				mockExam.getId(),
				mockExam.getTitle(),
				mockExam.getSubject(),
				mockExam.getDurationMinutes(),
				mockExam.getQuestionCount(),
				mockExam.getCreatedAt());
	}
}
