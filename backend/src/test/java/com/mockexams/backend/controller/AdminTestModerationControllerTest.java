package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTestModerationControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanModerationFixtures() {
		jdbcTemplate.update("delete from notifications");
		jdbcTemplate.update("delete from question_answers");
		jdbcTemplate.update("delete from question_options");
		jdbcTemplate.update("delete from questions");
		jdbcTemplate.update("delete from listening_segments");
		jdbcTemplate.update("delete from reading_paragraphs");
		jdbcTemplate.update("delete from test_sections");
		jdbcTemplate.update("delete from tests");
		jdbcTemplate.update("delete from teacher_profiles");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
		jdbcTemplate.update("delete from files");
	}

	@Test
	void anonymousRequestIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/admin/moderation/tests"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void nonAdminIsForbidden() throws Exception {
		String studentToken = registerStudent(uniqueEmail("mod-forbidden"));

		mockMvc.perform(get("/api/v1/admin/moderation/tests")
						.header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

		mockMvc.perform(post("/api/v1/admin/moderation/tests/1/approve")
						.header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void queueListsOnlySubmittedTestsWithEnvelopeAndPagination() throws Exception {
		TeacherRegistration teacher = approvedTeacher("mod-queue");
		String adminToken = registerAdmin(uniqueEmail("mod-queue-admin"));
		Long submittedTestId = submittedReadyReadingTest(teacher, "Queue Ready Test");
		Long draftTestId = createDraft(teacher.accessToken(), "Queue Draft Test", "reading")
				.andExpect(status().isCreated())
				.andReturnId();

		mockMvc.perform(get("/api/v1/admin/moderation/tests")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[?(@.id == %d)]", submittedTestId).exists())
				.andExpect(jsonPath("$.data[?(@.id == %d)]", draftTestId).doesNotExist())
				.andExpect(jsonPath("$.meta.page.number").value(0))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	@Test
	void queueMetaReportsPendingCountAndOldestSubmitted() throws Exception {
		TeacherRegistration teacher = approvedTeacher("mod-meta");
		String adminToken = registerAdmin(uniqueEmail("mod-meta-admin"));
		submittedReadyReadingTest(teacher, "Meta Ready Test");

		mockMvc.perform(get("/api/v1/admin/moderation/tests/meta")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.pendingCount").isNumber())
				.andExpect(jsonPath("$.data.oldestSubmittedAt").isNotEmpty());
	}

	@Test
	void previewIncludesAnswerKeysWhilePublicDetailDoesNot() throws Exception {
		TeacherRegistration teacher = approvedTeacher("mod-preview");
		String adminToken = registerAdmin(uniqueEmail("mod-preview-admin"));
		Long testId = submittedReadyReadingTest(teacher, "Preview Answer Key Test");

		mockMvc.perform(get("/api/v1/admin/moderation/tests/{testId}", testId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.test.id").value(testId))
				.andExpect(jsonPath("$.data.test.sections[0].questions[0].answerKeys[0].answerValue").value("A"))
				.andExpect(jsonPath("$.data.readyForDecision").value(true))
				.andExpect(jsonPath("$.data.validationFindings").isEmpty());

		mockMvc.perform(post("/api/v1/admin/moderation/tests/{testId}/approve", testId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		String publicResponse = mockMvc.perform(get("/api/v1/public/tests/{testId}", testId))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(publicResponse).doesNotContain("answerKeys");
		assertThat(publicResponse).doesNotContain("answerValue");
	}

	@Test
	void onlySubmittedTestCanBeApproved() throws Exception {
		TeacherRegistration teacher = approvedTeacher("mod-approve-status");
		String adminToken = registerAdmin(uniqueEmail("mod-approve-status-admin"));
		Long draftTestId = createDraft(teacher.accessToken(), "Draft Not Submitted", "reading")
				.andExpect(status().isCreated())
				.andReturnId();

		mockMvc.perform(post("/api/v1/admin/moderation/tests/{testId}/approve", draftTestId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TEST_MODERATION_STATUS_CONFLICT"));
	}

	@Test
	void approvalMakesTestVisibleInPublicCatalogAndNotifiesTeacher() throws Exception {
		TeacherRegistration teacher = approvedTeacher("mod-approve-visible");
		String adminToken = registerAdmin(uniqueEmail("mod-approve-visible-admin"));
		Long testId = submittedReadyReadingTest(teacher, "Approve Visible Test");

		mockMvc.perform(post("/api/v1/admin/moderation/tests/{testId}/approve", testId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.test.visibility").value("published"))
				.andExpect(jsonPath("$.data.test.publishedAt").isNotEmpty());

		mockMvc.perform(get("/api/v1/public/tests/{testId}", testId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(testId));

		Integer notificationCount = jdbcTemplate.queryForObject(
				"select count(*) from notifications where user_id = ? and type = 'test_moderation' and body like ?",
				Integer.class,
				teacher.userId(),
				"%" + "Approve Visible Test" + "%approved and is now visible%");
		assertThat(notificationCount).isEqualTo(1);
	}

	@Test
	void reApprovingAlreadyDecidedTestIsStatusConflict() throws Exception {
		TeacherRegistration teacher = approvedTeacher("mod-reapprove");
		String adminToken = registerAdmin(uniqueEmail("mod-reapprove-admin"));
		Long testId = submittedReadyReadingTest(teacher, "Reapprove Test");
		mockMvc.perform(post("/api/v1/admin/moderation/tests/{testId}/approve", testId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/moderation/tests/{testId}/approve", testId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TEST_MODERATION_STATUS_CONFLICT"));
	}

	@Test
	void rejectRequiresReasonAndReturnsTestEditable() throws Exception {
		TeacherRegistration teacher = approvedTeacher("mod-reject");
		String adminToken = registerAdmin(uniqueEmail("mod-reject-admin"));
		Long testId = submittedReadyReadingTest(teacher, "Reject Test");

		mockMvc.perform(post("/api/v1/admin/moderation/tests/{testId}/reject", testId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": ""
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/admin/moderation/tests/{testId}/reject", testId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": "Missing audio file references."
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.test.visibility").value("rejected"))
				.andExpect(jsonPath("$.data.test.moderationNote").value("Missing audio file references."));

		Integer notificationCount = jdbcTemplate.queryForObject(
				"select count(*) from notifications where user_id = ? and type = 'test_moderation' and body like ?",
				Integer.class,
				teacher.userId(),
				"%" + "Reject Test" + "%was rejected%");
		assertThat(notificationCount).isEqualTo(1);

		mockMvc.perform(get("/api/v1/teacher/tests/{testId}/preview", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.visibility").value("rejected"));
	}

	@Test
	void reRejectingAlreadyDecidedTestIsStatusConflict() throws Exception {
		TeacherRegistration teacher = approvedTeacher("mod-rereject");
		String adminToken = registerAdmin(uniqueEmail("mod-rereject-admin"));
		Long testId = submittedReadyReadingTest(teacher, "Rereject Test");
		mockMvc.perform(post("/api/v1/admin/moderation/tests/{testId}/reject", testId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": "First rejection."
								}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/moderation/tests/{testId}/reject", testId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": "Second rejection."
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TEST_MODERATION_STATUS_CONFLICT"));
	}

	@Test
	void approvalFailsWhenContentIsNoLongerReadyAtDecisionTime() throws Exception {
		TeacherRegistration teacher = approvedTeacher("mod-stale");
		String adminToken = registerAdmin(uniqueEmail("mod-stale-admin"));
		Long testId = submittedReadyReadingTest(teacher, "Stale Content Test");
		jdbcTemplate.update("update tests set title = '' where id = ?", testId);

		mockMvc.perform(post("/api/v1/admin/moderation/tests/{testId}/approve", testId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("TEST_MODERATION_NOT_READY"));
	}

	@Test
	void previewReportsValidationFindingsForStaleSubmittedContent() throws Exception {
		TeacherRegistration teacher = approvedTeacher("mod-findings");
		String adminToken = registerAdmin(uniqueEmail("mod-findings-admin"));
		Long testId = submittedReadyReadingTest(teacher, "Findings Test");
		jdbcTemplate.update(
				"update test_sections set passage_text = '' where test_id = ? and section_order = 1",
				testId);

		mockMvc.perform(get("/api/v1/admin/moderation/tests/{testId}", testId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.readyForDecision").value(false))
				.andExpect(jsonPath("$.data.validationFindings['sections[0].passageText']").exists());
	}

	private Long submittedReadyReadingTest(TeacherRegistration teacher, String title) throws Exception {
		Long testId = createDraft(teacher.accessToken(), title, "reading")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionAId = addSectionWithQuestion(teacher.accessToken(), testId, title + " Section 1");
		addSection(teacher.accessToken(), testId, title + " Section 2")
				.andExpect(status().isCreated());
		addSection(teacher.accessToken(), testId, title + " Section 3")
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/submit", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.visibility").value("submitted"));
		assertThat(sectionAId).isNotNull();
		return testId;
	}

	private Long addSectionWithQuestion(String token, Long testId, String title) throws Exception {
		Long sectionId = addSection(token, testId, title)
				.andExpect(status().isCreated())
				.andReturnSectionId(0);
		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "multiple_choice",
								  "questionText": "Which answer is correct?",
								  "instructions": "Choose one answer.",
								  "score": 1,
								  "options": [
								    { "optionLabel": "A", "optionText": "First option" },
								    { "optionLabel": "B", "optionText": "Second option" }
								  ],
								  "answers": [
								    { "answerValue": "A", "primary": true }
								  ]
								}
								"""))
				.andExpect(status().isCreated());
		return sectionId;
	}

	private ResultWithJson createDraft(String token, String title, String type) throws Exception {
		return new ResultWithJson(mockMvc.perform(post("/api/v1/teacher/tests")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "%s",
								  "description": "Teacher-owned practice test.",
								  "type": "%s",
								  "difficulty": "intermediate",
								  "estimatedBandMin": 5.5,
								  "estimatedBandMax": 7.0,
								  "durationMinutes": 60,
								  "priceAmount": 0,
								  "currency": "UZS",
								  "free": true,
								  "language": "en"
								}
								""".formatted(title, type))));
	}

	private ResultWithJson addSection(String token, Long testId, String title) throws Exception {
		return new ResultWithJson(mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections", testId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "%s",
								  "instructions": "Complete the section.",
								  "passageText": "Passage for %s.",
								  "durationMinutes": 20
								}
								""".formatted(title, title))));
	}

	private TeacherRegistration approvedTeacher(String prefix) throws Exception {
		TeacherRegistration teacher = registerTeacher(uniqueEmail(prefix));
		String adminToken = registerAdmin(uniqueEmail(prefix + "-admin"));
		mockMvc.perform(post("/api/v1/admin/teacher-profiles/{profileId}/approve", teacher.profileId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
		return teacher;
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
		String compactPrefix = prefix.length() > 18 ? prefix.substring(0, 18) : prefix;
		String compactUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		return compactPrefix + "-" + compactUuid + "@example.test";
	}

	private record TeacherRegistration(String accessToken, Long userId, Long profileId) {
	}

	private final class ResultWithJson {

		private final org.springframework.test.web.servlet.ResultActions resultActions;

		private ResultWithJson(org.springframework.test.web.servlet.ResultActions resultActions) {
			this.resultActions = resultActions;
		}

		private ResultWithJson andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
			resultActions.andExpect(matcher);
			return this;
		}

		private Long andReturnId() throws Exception {
			return root().at("/data/id").asLong();
		}

		private Long andReturnSectionId(int index) throws Exception {
			return root().at("/data/sections/" + index + "/id").asLong();
		}

		private JsonNode root() throws Exception {
			String response = resultActions.andReturn().getResponse().getContentAsString();
			return objectMapper.readTree(response);
		}
	}
}
