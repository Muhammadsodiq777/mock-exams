package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void teacherApprovalDecisionCreatesNotification() throws Exception {
		TeacherRegistration approvedTeacher = registerTeacher(uniqueEmail("t-appr-note"));
		TeacherRegistration rejectedTeacher = registerTeacher(uniqueEmail("t-rej-note"));
		String adminToken = registerAdmin(uniqueEmail("a-note-decision"));

		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/approve", approvedTeacher.profileId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/reject", rejectedTeacher.profileId())
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "rejectionReason": "Please add more teaching background."
								}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/teacher/notifications")
						.header("Authorization", "Bearer " + approvedTeacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].type").value("test_moderation"))
				.andExpect(jsonPath("$.data[0].title").value("Teacher profile approved"))
				.andExpect(jsonPath("$.data[0].payload.teacherProfileId").value(approvedTeacher.profileId()))
				.andExpect(jsonPath("$.data[0].readAt").doesNotExist())
				.andExpect(jsonPath("$.meta.page.totalElements").value(1))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());

		mockMvc.perform(get("/api/v1/teacher/notifications")
						.header("Authorization", "Bearer " + rejectedTeacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].type").value("test_moderation"))
				.andExpect(jsonPath("$.data[0].title").value("Teacher profile rejected"))
				.andExpect(jsonPath("$.data[0].payload.teacherProfileId").value(rejectedTeacher.profileId()))
				.andExpect(jsonPath("$.data[0].payload.rejectionReason").value("Please add more teaching background."))
				.andExpect(jsonPath("$.meta.page.totalElements").value(1));
	}

	@Test
	void userSeesOnlyTheirNotifications() throws Exception {
		TeacherRegistration firstTeacher = registerTeacher(uniqueEmail("t-first-note"));
		TeacherRegistration secondTeacher = registerTeacher(uniqueEmail("t-second-note"));
		String adminToken = registerAdmin(uniqueEmail("a-note-isolate"));
		approve(adminToken, firstTeacher.profileId());
		approve(adminToken, secondTeacher.profileId());

		mockMvc.perform(get("/api/v1/teacher/notifications")
						.header("Authorization", "Bearer " + firstTeacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].payload.teacherProfileId", hasItem(firstTeacher.profileId().intValue())))
				.andExpect(jsonPath("$.data[*].payload.teacherProfileId", not(hasItem(secondTeacher.profileId().intValue()))))
				.andExpect(jsonPath("$.meta.page.totalElements").value(1));
	}

	@Test
	void markReadIsIdempotent() throws Exception {
		TeacherRegistration teacher = registerTeacher(uniqueEmail("t-read-note"));
		String adminToken = registerAdmin(uniqueEmail("a-note-read"));
		approve(adminToken, teacher.profileId());
		Long notificationId = firstNotificationId(teacher.accessToken());

		mockMvc.perform(get("/api/v1/teacher/notifications/unread-count")
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.unreadCount").value(1));

		String firstReadResponse = mockMvc.perform(post("/api/v1/teacher/notifications/{notificationId}/read", notificationId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(notificationId))
				.andExpect(jsonPath("$.data.readAt").isNotEmpty())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String firstReadAt = objectMapper.readTree(firstReadResponse).at("/data/readAt").asText();

		String secondReadResponse = mockMvc.perform(post("/api/v1/teacher/notifications/{notificationId}/read", notificationId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(notificationId))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String secondReadAt = objectMapper.readTree(secondReadResponse).at("/data/readAt").asText();

		assertThat(secondReadAt).isEqualTo(firstReadAt);

		mockMvc.perform(get("/api/v1/teacher/notifications/unread-count")
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.unreadCount").value(0));
	}

	private void approve(String adminToken, Long profileId) throws Exception {
		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/approve", profileId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
	}

	private Long firstNotificationId(String accessToken) throws Exception {
		String response = mockMvc.perform(get("/api/v1/teacher/notifications")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).at("/data/0/id").asLong();
	}

	private String registerAdmin(String email) throws Exception {
		String accessToken = registerStudent(email);
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select u.id, r.id
				from users u
				join roles r on r.code = 'admin'
				where u.email = ?
				""", email);
		return accessToken;
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
		return accessTokenFrom(response);
	}

	private TeacherRegistration registerTeacher(String email) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/register/teacher")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Teacher Example",
								  "displayName": "Teacher Example",
								  "headline": "IELTS mentor",
								  "bio": "I help students prepare for IELTS."
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode root = objectMapper.readTree(response);
		return new TeacherRegistration(
				accessTokenFrom(response),
				root.at("/data/user/id").asLong(),
				root.at("/data/user/teacherProfile/id").asLong());
	}

	private String accessTokenFrom(String response) throws Exception {
		return objectMapper.readTree(response).at("/data/accessToken").asText();
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private record TeacherRegistration(String accessToken, Long userId, Long profileId) {
	}
}
