package com.mockexams.backend.teachers.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
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

import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.teachers.service.TeacherApprovalService;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherProfileControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TeacherApprovalService teacherApprovalService;

	@Test
	void teacherCanReadOwnProfile() throws Exception {
		TeacherRegistration teacher = registerTeacher(uniqueEmail("teacher-profile-read"));

		mockMvc.perform(get("/api/v1/teacher/profile")
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(teacher.profileId()))
				.andExpect(jsonPath("$.data.userId").value(teacher.userId()))
				.andExpect(jsonPath("$.data.displayName").value("Teacher Example"))
				.andExpect(jsonPath("$.data.status").value("pending_approval"))
				.andExpect(jsonPath("$.data.avatarFileId").value(nullValue()))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	@Test
	void rejectedProfileCanBeEditedAndResubmitted() throws Exception {
		TeacherRegistration teacher = registerTeacher(uniqueEmail("teacher-rejected"));
		String adminToken = registerAdmin(uniqueEmail("admin-reject"));

		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/reject", teacher.profileId())
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "rejectionReason": "Please add more teaching background."
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("rejected"))
				.andExpect(jsonPath("$.data.rejectionReason").value("Please add more teaching background."));

		mockMvc.perform(patch("/api/v1/teacher/profile")
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "Updated Teacher",
								  "headline": "Certified IELTS mentor",
								  "bio": "Ten years of IELTS classroom experience.",
								  "avatarFileId": null
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.displayName").value("Updated Teacher"))
				.andExpect(jsonPath("$.data.status").value("draft"))
				.andExpect(jsonPath("$.data.rejectionReason").doesNotExist())
				.andExpect(jsonPath("$.data.approvedByUserId").doesNotExist())
				.andExpect(jsonPath("$.data.approvedAt").doesNotExist());

		mockMvc.perform(post("/api/v1/teacher/profile/submit")
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("pending_approval"))
				.andExpect(jsonPath("$.data.rejectionReason").doesNotExist());
	}

	@Test
	void publicEndpointsExposeOnlyApprovedProfiles() throws Exception {
		TeacherRegistration pendingTeacher = registerTeacher(uniqueEmail("teacher-public-pending"));
		TeacherRegistration approvedTeacher = registerTeacher(uniqueEmail("teacher-public-approved"));
		String adminToken = registerAdmin(uniqueEmail("admin-public-approve"));
		approve(adminToken, approvedTeacher.profileId());

		mockMvc.perform(get("/api/v1/public/teacher-profiles")
						.param("page", "0")
						.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(approvedTeacher.profileId().intValue())))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(pendingTeacher.profileId().intValue()))))
				.andExpect(jsonPath("$.data[*].status", everyItem(org.hamcrest.Matchers.is("approved"))))
				.andExpect(jsonPath("$.meta.page.number").value(0))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());

		mockMvc.perform(get("/api/v1/public/teacher-profiles/{profileId}", pendingTeacher.profileId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEACHER_PROFILE_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/public/teacher-profiles/{profileId}", approvedTeacher.profileId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(approvedTeacher.profileId()))
				.andExpect(jsonPath("$.data.status").value("approved"));
	}

	@Test
	void adminApprovalRecordsReviewerAndTimestamp() throws Exception {
		TeacherRegistration teacher = registerTeacher(uniqueEmail("teacher-approval"));
		String adminEmail = uniqueEmail("admin-approval");
		String adminToken = registerAdmin(adminEmail);
		Long adminUserId = userIdFor(adminEmail);

		String response = mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/approve", teacher.profileId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("approved"))
				.andExpect(jsonPath("$.data.approvedByUserId").value(adminUserId))
				.andExpect(jsonPath("$.data.approvedAt").isNotEmpty())
				.andExpect(jsonPath("$.data.rejectionReason").doesNotExist())
				.andReturn()
				.getResponse()
				.getContentAsString();

		OffsetDateTime approvedAt = OffsetDateTime.parse(objectMapper.readTree(response)
				.at("/data/approvedAt")
				.asText());
		OffsetDateTime persistedApprovedAt = jdbcTemplate.queryForObject(
				"select approved_at from teacher_profiles where id = ?",
				OffsetDateTime.class,
				teacher.profileId());
		Long persistedReviewer = jdbcTemplate.queryForObject(
				"select approved_by_user_id from teacher_profiles where id = ?",
				Long.class,
				teacher.profileId());

		assertThat(approvedAt).isEqualTo(persistedApprovedAt);
		assertThat(persistedReviewer).isEqualTo(adminUserId);
	}

	@Test
	void teacherPublishingGateRequiresApprovedProfile() throws Exception {
		TeacherRegistration teacher = registerTeacher(uniqueEmail("teacher-publish-gate"));
		User user = userRepository.findWithRolesById(teacher.userId()).orElseThrow();

		assertThatThrownBy(() -> teacherApprovalService.requireApprovedForPublishing(user))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					BusinessException businessException = (BusinessException) ex;
					assertThat(businessException.getCode()).isEqualTo("TEACHER_APPROVAL_REQUIRED");
				});

		String adminToken = registerAdmin(uniqueEmail("admin-publish-gate"));
		approve(adminToken, teacher.profileId());

		assertThat(teacherApprovalService.requireApprovedForPublishing(user)).isEqualTo(teacher.profileId());
	}

	private void approve(String adminToken, Long profileId) throws Exception {
		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/approve", profileId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
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

	private Long userIdFor(String email) {
		return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private record TeacherRegistration(String accessToken, Long userId, Long profileId) {
	}
}
