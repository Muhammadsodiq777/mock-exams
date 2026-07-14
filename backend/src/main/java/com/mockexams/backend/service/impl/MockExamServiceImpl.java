package com.mockexams.backend.service.impl;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.MockExamRDO;
import com.mockexams.backend.mapper.MockExamMapper;
import com.mockexams.backend.repository.MockExamRepository;
import com.mockexams.backend.service.MockExamService;

@Service
class MockExamServiceImpl implements MockExamService {

	private final MockExamRepository mockExamRepository;
	private final MockExamMapper mockExamMapper;

	MockExamServiceImpl(MockExamRepository mockExamRepository, MockExamMapper mockExamMapper) {
		this.mockExamRepository = mockExamRepository;
		this.mockExamMapper = mockExamMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<MockExamRDO> listExams() {
		return mockExamRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
				.map(mockExamMapper::toRDO)
				.toList();
	}
}
