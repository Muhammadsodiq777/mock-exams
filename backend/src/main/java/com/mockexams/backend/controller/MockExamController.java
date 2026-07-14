package com.mockexams.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.dto.MockExamRDO;
import com.mockexams.backend.service.MockExamService;

@RestController
@Validated
@RequestMapping({ "/api/v1/exams", "/api/exams" })
class MockExamController {

	private final MockExamService mockExamService;

	MockExamController(MockExamService mockExamService) {
		this.mockExamService = mockExamService;
	}

	@GetMapping
	ResponseEntity<List<MockExamRDO>> listExams() {
		return ResponseEntity.ok(mockExamService.listExams());
	}
}
