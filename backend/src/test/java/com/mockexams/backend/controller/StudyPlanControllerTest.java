package com.mockexams.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"app.ai.study-plan.provider=mock",
		"app.ai.usage.free-requests-limit=10",
		"app.ai.usage.hard-limit-requests=10"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudyPlanControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanStudyPlanFixtures() {
		jdbcTemplate.update("delete from study_plan_tasks");
		jdbcTemplate.update("delete from study_plans");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from purchases");
		jdbcTemplate.update("delete from subscriptions");
		jdbcTemplate.update("delete from marketplace_listing_items");
		jdbcTemplate.update("delete from marketplace_listings");
		jdbcTemplate.update("delete from test_results");
		jdbcTemplate.update("delete from student_answers");
		jdbcTemplate.update("delete from test_attempts");
		jdbcTemplate.update("delete from question_answers");
		jdbcTemplate.update("delete from question_options");
		jdbcTemplate.update("delete from questions");
		jdbcTemplate.update("delete from listening_segments");
		jdbcTemplate.update("delete from reading_paragraphs");
		jdbcTemplate.update("delete from test_sections");
		jdbcTemplate.update("delete from test_tag_links");
		jdbcTemplate.update("delete from test_category_links");
		jdbcTemplate.update("delete from tests");
		jdbcTemplate.update("delete from teacher_profiles");
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
	}

	@Test
	void studentCanCreateReadListAndCompleteTasks() throws Exception {
		String token = registerStudent(uniqueEmail("study-plan-http"));
		LocalDate activeStart = LocalDate.now().minusDays(1);

		String createResponse = mockMvc.perform(post("/api/v1/student/study-plans")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "targetBand": 7.0,
								  "durationDays": 7,
								  "availableMinutesPerDay": 45,
								  "startsAt": "%s"
								}
								""".formatted(activeStart)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.feedbackType").doesNotExist())
				.andExpect(jsonPath("$.data.targetBand").value(7.0))
				.andExpect(jsonPath("$.data.tasks[0].skill").isNotEmpty())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode created = objectMapper.readTree(createResponse);
		long planId = created.at("/data/id").asLong();
		long taskId = created.at("/data/tasks/0/id").asLong();

		mockMvc.perform(get("/api/v1/student/study-plans/active")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(planId));

		mockMvc.perform(post("/api/v1/student/study-plans/{planId}/tasks/{taskId}/complete", planId, taskId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.completed").value(true))
				.andExpect(jsonPath("$.data.completedAt").isNotEmpty());

		mockMvc.perform(post("/api/v1/student/study-plans/{planId}/tasks/{taskId}/uncomplete", planId, taskId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.completed").value(false));
	}

	@Test
	void studyPlansRequireStudentRole() throws Exception {
		String teacherToken = registerTeacher(uniqueEmail("study-plan-teacher"));

		mockMvc.perform(get("/api/v1/student/study-plans/active")
						.header("Authorization", "Bearer " + teacherToken))
				.andExpect(status().isForbidden());
	}

	private String registerStudent(String email) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/register/student")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Student Example"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).at("/data/accessToken").asText();
	}

	private String registerTeacher(String email) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/register/teacher")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Teacher Example",
								  "displayName": "Teacher Example"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).at("/data/accessToken").asText();
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}
}
