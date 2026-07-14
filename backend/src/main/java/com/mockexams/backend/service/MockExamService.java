package com.mockexams.backend.service;

import java.util.List;

import com.mockexams.backend.dto.MockExamRDO;

public interface MockExamService {

	List<MockExamRDO> listExams();
}
