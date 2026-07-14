package com.mockexams.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.mockexams.backend.service.AiUsageAccounting;
import com.mockexams.backend.service.AiUsageRequest;
import com.mockexams.backend.service.AiUsageReservation;
import com.mockexams.backend.service.AiUsageService;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"app.openai.enabled=true",
		"app.openai.api-key=test-real-openai-key",
		"app.ai.usage.free-requests-limit=1",
		"app.ai.usage.hard-limit-requests=2"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiUsageControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AiUsageService aiUsageService;

	@BeforeEach
	void setUp() {
		updateUsageLimitSetting("ai.free_monthly_requests", 1);
		updateUsageLimitSetting("ai.hard_monthly_requests", 2);
	}

	@AfterEach
	void tearDown() {
		updateUsageLimitSetting("ai.free_monthly_requests", 20);
		updateUsageLimitSetting("ai.hard_monthly_requests", 50);
	}

	@Test
	void studentCanReadRemainingAiUsage() throws Exception {
		Registration student = registerStudent(uniqueEmail("student-ai-usage"));
		recordSuccessfulStudyPlan(student.userId());

		mockMvc.perform(get("/api/v1/student/ai-usage")
						.header("Authorization", "Bearer " + student.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.freeRequestsLimit").value(1))
				.andExpect(jsonPath("$.data.usedRequests").value(1))
				.andExpect(jsonPath("$.data.hardRequestsRemaining").value(1))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	@Test
	void adminCanReadAiUsageReportRowsAndTotals() throws Exception {
		Registration student = registerStudent(uniqueEmail("student-ai-report"));
		String adminToken = registerAdmin(uniqueEmail("admin-ai-report"));
		recordSuccessfulStudyPlan(student.userId());

		mockMvc.perform(get("/api/v1/admin/ai-usage/report")
						.header("Authorization", "Bearer " + adminToken)
						.param("studentUserId", student.userId().toString())
						.param("status", "completed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].studentUserId").value(student.userId()))
				.andExpect(jsonPath("$.data[0].feedbackType").value("study_plan"))
				.andExpect(jsonPath("$.data[0].status").value("completed"))
				.andExpect(jsonPath("$.data[0].provider").value("openai"))
				.andExpect(jsonPath("$.data[0].model").value("gpt-4.1-mini"))
				.andExpect(jsonPath("$.meta.page.totalElements").value(1));

		mockMvc.perform(get("/api/v1/admin/ai-usage/report/totals")
						.header("Authorization", "Bearer " + adminToken)
						.param("studentUserId", student.userId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalRequests").value(1))
				.andExpect(jsonPath("$.data.completedRequests").value(1))
				.andExpect(jsonPath("$.data.promptTokens").value(10))
				.andExpect(jsonPath("$.data.completionTokens").value(5));
	}

	@Test
	void adminReportRejectsUnsupportedStatusFilter() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("admin-ai-invalid-status"));

		mockMvc.perform(get("/api/v1/admin/ai-usage/report")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "not_a_status"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	private void recordSuccessfulStudyPlan(Long userId) {
		AiUsageReservation reservation = aiUsageService.guardAndReserve(userId, AiUsageRequest.studyPlan());
		aiUsageService.recordSuccess(reservation, new AiUsageAccounting("openai", "gpt-4.1-mini", 10, 5, null));
	}

	private void updateUsageLimitSetting(String key, int limit) {
		jdbcTemplate.update("""
				update platform_settings
				set setting_value = ?
				where setting_key = ?
				""", "{\"value\": " + limit + ", \"period\": \"monthly\"}", key);
	}

	private String registerAdmin(String email) throws Exception {
		Registration registration = registerStudent(email);
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select u.id, r.id
				from users u
				join roles r on r.code = 'admin'
				where u.email = ?
				""", email);
		return registration.accessToken();
	}

	private Registration registerStudent(String email) throws Exception {
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
		JsonNode root = objectMapper.readTree(response);
		return new Registration(
				root.at("/data/accessToken").asText(),
				root.at("/data/user/id").asLong());
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private record Registration(String accessToken, Long userId) {
	}
}
