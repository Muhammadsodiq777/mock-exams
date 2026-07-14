package com.mockexams.backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MockExamControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void listExamsReturnsSeededExamDtos() throws Exception {
		mockMvc.perform(get("/api/v1/exams"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].title").value("Algebra Fundamentals"))
				.andExpect(jsonPath("$[0].subject").value("Mathematics"))
				.andExpect(jsonPath("$[0].durationMinutes").value(60))
				.andExpect(jsonPath("$[0].questionCount").value(40))
				.andExpect(jsonPath("$[0].createdAt").exists());
	}
}
