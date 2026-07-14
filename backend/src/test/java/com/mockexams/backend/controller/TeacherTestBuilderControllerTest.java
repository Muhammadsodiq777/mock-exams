package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
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
class TeacherTestBuilderControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanBuilderFixtures() {
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
	void approvedTeacherCanCreatePreparePreviewAndSubmitDraft() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-happy");

		Long testId = createDraft(teacher.accessToken(), "Academic Reading Draft")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.title").value("Academic Reading Draft"))
				.andExpect(jsonPath("$.data.visibility").value("draft"))
				.andExpect(jsonPath("$.data.teacherProfileId").value(teacher.profileId()))
				.andExpect(jsonPath("$.data.sections.length()").value(0))
				.andReturnId();

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}", testId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Academic Reading Draft Updated",
								  "description": "Updated teacher-owned draft.",
								  "type": "reading",
								  "difficulty": "upper_intermediate",
								  "estimatedBandMin": 6.0,
								  "estimatedBandMax": 8.0,
								  "durationMinutes": 65,
								  "priceAmount": 0,
								  "currency": "UZS",
								  "free": true,
								  "language": "en"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.title").value("Academic Reading Draft Updated"))
				.andExpect(jsonPath("$.data.difficulty").value("upper_intermediate"));

		Long sectionId = addSection(teacher.accessToken(), testId, "Reading Passage 1")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].title").value("Reading Passage 1"))
				.andExpect(jsonPath("$.data.sections[0].passageText").value("Passage for Reading Passage 1."))
				.andExpect(jsonPath("$.data.sections[0].sectionOrder").value(1))
				.andReturnSectionId(0);

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}/sections/{sectionId}", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Reading Passage 1A",
								  "instructions": "Read and answer.",
								  "passageText": "Updated passage text.",
								  "durationMinutes": 20
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].title").value("Reading Passage 1A"))
				.andExpect(jsonPath("$.data.sections[0].passageText").value("Updated passage text."))
				.andExpect(jsonPath("$.data.sections[0].durationMinutes").value(20));

		mockMvc.perform(get("/api/v1/teacher/tests/{testId}/preview", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(testId))
				.andExpect(jsonPath("$.data.sections[0].id").value(sectionId));

		addSection(teacher.accessToken(), testId, "Reading Passage 2")
				.andExpect(status().isCreated());
		addSection(teacher.accessToken(), testId, "Reading Passage 3")
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/submit", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.visibility").value("submitted"))
				.andExpect(jsonPath("$.data.moderationNote").doesNotExist());

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}", testId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Submitted Direct Edit",
								  "type": "reading",
								  "durationMinutes": 60,
								  "priceAmount": 0,
								  "currency": "UZS",
								  "free": true,
								  "language": "en"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TEACHER_TEST_STATUS_CONFLICT"));
	}

	@Test
	void onlyOwnerTeacherCanEditDraft() throws Exception {
		TeacherRegistration owner = approvedTeacher("builder-owner");
		TeacherRegistration otherTeacher = approvedTeacher("builder-other");
		Long testId = createDraft(owner.accessToken(), "Owner Draft")
				.andExpect(status().isCreated())
				.andReturnId();

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}", testId)
						.header("Authorization", "Bearer " + otherTeacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Not Mine",
								  "type": "reading",
								  "durationMinutes": 60,
								  "priceAmount": 0,
								  "currency": "UZS",
								  "free": true,
								  "language": "en"
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEST_NOT_FOUND"));
	}

	@Test
	void teacherListReturnsOnlyOwnedTestsWithStatusSearchSortAndPagination() throws Exception {
		TeacherRegistration owner = approvedTeacher("builder-list-owner");
		TeacherRegistration otherTeacher = approvedTeacher("builder-list-other");
		Long alphaId = createDraft(owner.accessToken(), "Alpha Reading Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long betaId = createDraft(owner.accessToken(), "Beta Reading Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long otherAlphaId = createDraft(otherTeacher.accessToken(), "Alpha Other Draft")
				.andExpect(status().isCreated())
				.andReturnId();

		mockMvc.perform(get("/api/v1/teacher/tests")
						.header("Authorization", "Bearer " + owner.accessToken())
						.param("status", "draft")
						.param("q", "alpha")
						.param("page", "0")
						.param("size", "10")
						.param("sort", "title,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(alphaId))
				.andExpect(jsonPath("$.data[0].title").value("Alpha Reading Draft"))
				.andExpect(jsonPath("$.data[0].visibility").value("draft"))
				.andExpect(jsonPath("$.data[0].sections").doesNotExist())
				.andExpect(jsonPath("$.data[*].id", not(hasItem(betaId.intValue()))))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(otherAlphaId.intValue()))))
				.andExpect(jsonPath("$.meta.page.number").value(0))
				.andExpect(jsonPath("$.meta.page.size").value(10))
				.andExpect(jsonPath("$.meta.page.totalElements").value(1))
				.andExpect(jsonPath("$.meta.sort[0].field").value("title"))
				.andExpect(jsonPath("$.meta.sort[0].direction").value("asc"))
				.andExpect(jsonPath("$.meta.filters.status").value("draft"))
				.andExpect(jsonPath("$.meta.filters.q").value("alpha"));
	}

	@Test
	void teacherCanArchiveOwnedDraft() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-archive-owner");
		Long testId = createDraft(teacher.accessToken(), "Archive Me")
				.andExpect(status().isCreated())
				.andReturnId();

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/archive", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(testId))
				.andExpect(jsonPath("$.data.visibility").value("archived"));

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}", testId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Archived Direct Edit"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TEACHER_TEST_STATUS_CONFLICT"));
	}

	@Test
	void archiveRequiresOwnershipAndRejectsSubmittedTests() throws Exception {
		TeacherRegistration owner = approvedTeacher("builder-archive-conflict");
		TeacherRegistration otherTeacher = approvedTeacher("builder-archive-other");
		Long testId = createDraft(owner.accessToken(), "Submitted Archive Conflict")
				.andExpect(status().isCreated())
				.andReturnId();

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/archive", testId)
						.header("Authorization", "Bearer " + otherTeacher.accessToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEST_NOT_FOUND"));

		jdbcTemplate.update("update tests set visibility = 'submitted' where id = ?", testId);

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/archive", testId)
						.header("Authorization", "Bearer " + owner.accessToken()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TEACHER_TEST_STATUS_CONFLICT"));
	}

	@Test
	void titleOnlyTestPatchPreservesExistingMetadata() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-patch-preserve");
		Long testId = createDraft(teacher.accessToken(), "Preserved Metadata Draft")
				.andExpect(status().isCreated())
				.andReturnId();

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}", testId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Only Title Changed"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.title").value("Only Title Changed"))
				.andExpect(jsonPath("$.data.description").value("Teacher-owned practice test."))
				.andExpect(jsonPath("$.data.type").value("reading"))
				.andExpect(jsonPath("$.data.difficulty").value("intermediate"))
				.andExpect(jsonPath("$.data.estimatedBandMin").value(5.5))
				.andExpect(jsonPath("$.data.estimatedBandMax").value(7.0))
				.andExpect(jsonPath("$.data.durationMinutes").value(60))
				.andExpect(jsonPath("$.data.priceAmount").value(0.0))
				.andExpect(jsonPath("$.data.currency").value("UZS"))
				.andExpect(jsonPath("$.data.free").value(true))
				.andExpect(jsonPath("$.data.language").value("en"));
	}

	@Test
	void partialSectionPatchPreservesOmittedFields() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-section-patch");
		Long testId = createDraft(teacher.accessToken(), "Section Patch Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSection(teacher.accessToken(), testId, "Original Section")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}/sections/{sectionId}", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "instructions": "Updated instructions only."
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].title").value("Original Section"))
				.andExpect(jsonPath("$.data.sections[0].instructions").value("Updated instructions only."))
				.andExpect(jsonPath("$.data.sections[0].passageText").value("Passage for Original Section."))
				.andExpect(jsonPath("$.data.sections[0].durationMinutes").value(20));

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}/sections/{sectionId}", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "durationMinutes": 25
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].title").value("Original Section"))
				.andExpect(jsonPath("$.data.sections[0].instructions").value("Updated instructions only."))
				.andExpect(jsonPath("$.data.sections[0].passageText").value("Passage for Original Section."))
				.andExpect(jsonPath("$.data.sections[0].durationMinutes").value(25));
	}

	@Test
	void partialBandPatchValidatesAgainstPreservedOtherBand() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-band-patch");
		Long testId = createDraft(teacher.accessToken(), "Band Patch Draft")
				.andExpect(status().isCreated())
				.andReturnId();

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}", testId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "estimatedBandMin": 7.5
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void unapprovedTeacherCannotCreateDraft() throws Exception {
		TeacherRegistration teacher = registerTeacher(uniqueEmail("builder-unapproved"));

		createDraft(teacher.accessToken(), "Blocked Draft")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("TEACHER_APPROVAL_REQUIRED"));
	}

	@Test
	void publishedTestsCannotBeEditedDirectly() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-published");
		Long testId = createDraft(teacher.accessToken(), "Published Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		jdbcTemplate.update("update tests set visibility = 'published', published_at = ? where id = ?",
				OffsetDateTime.now(), testId);

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections", testId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Late Section",
								  "instructions": "Should not be accepted.",
								  "durationMinutes": 10
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TEACHER_TEST_STATUS_CONFLICT"));
	}

	@Test
	void submitFailsWhenRequiredMetadataIsMissing() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-submit-metadata");
		Long testId = createDraft(teacher.accessToken(), "Incomplete Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		addSection(teacher.accessToken(), testId, "Only Section")
				.andExpect(status().isCreated());
		jdbcTemplate.update("update tests set title = '' where id = ?", testId);

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/submit", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void submitFailsWhenDraftHasNoSections() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-submit-sections");
		Long testId = createDraft(teacher.accessToken(), "No Sections Draft")
				.andExpect(status().isCreated())
				.andReturnId();

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/submit", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void readingSubmitFailsUnlessDraftHasExactlyThreeSections() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-submit-count");
		Long testId = createDraft(teacher.accessToken(), "Two Section Reading Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		addSection(teacher.accessToken(), testId, "Reading Passage 1")
				.andExpect(status().isCreated());
		addSection(teacher.accessToken(), testId, "Reading Passage 2")
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/submit", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void teacherCanManageReadingParagraphsAndPreviewQuestionLinks() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-reading");
		Long testId = createDraft(teacher.accessToken(), "Paragraph Reading Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSection(teacher.accessToken(), testId, "Reading Passage 1")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);

		Long paragraphA = addParagraph(teacher.accessToken(), testId, sectionId, "A", "First paragraph.")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].label").value("A"))
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].paragraphOrder").value(1))
				.andReturnParagraphId(0, 0);
		Long paragraphB = addParagraph(teacher.accessToken(), testId, sectionId, "B", "Second paragraph.")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].paragraphs[1].label").value("B"))
				.andExpect(jsonPath("$.data.sections[0].paragraphs[1].paragraphOrder").value(2))
				.andReturnParagraphId(0, 1);

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs/{paragraphId}",
						testId, sectionId, paragraphA)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "label": "C",
								  "content": "Updated first paragraph."
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].label").value("C"))
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].content").value("Updated first paragraph."));

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs/reorder", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "paragraphIds": [%d, %d]
								}
								""".formatted(paragraphB, paragraphA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].id").value(paragraphB))
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].paragraphOrder").value(1))
				.andExpect(jsonPath("$.data.sections[0].paragraphs[1].id").value(paragraphA))
				.andExpect(jsonPath("$.data.sections[0].paragraphs[1].paragraphOrder").value(2));

		Long questionId = addQuestion(teacher.accessToken(), testId, sectionId, "C")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].questions[0].relatedParagraphLabel").value("C"))
				.andReturnQuestionId(0, 0);

		mockMvc.perform(get("/api/v1/teacher/tests/{testId}/preview", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].passageText").value("Passage for Reading Passage 1."))
				.andExpect(jsonPath("$.data.sections[0].paragraphs[1].label").value("C"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].id").value(questionId))
				.andExpect(jsonPath("$.data.sections[0].questions[0].relatedParagraphLabel").value("C"));

		mockMvc.perform(delete("/api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs/{paragraphId}",
						testId, sectionId, paragraphB)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].paragraphs.length()").value(1))
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].paragraphOrder").value(1));
	}

	@Test
	void duplicateParagraphLabelInSectionIsRejected() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-dup-para");
		Long testId = createDraft(teacher.accessToken(), "Duplicate Paragraph Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSection(teacher.accessToken(), testId, "Reading Passage 1")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);
		addParagraph(teacher.accessToken(), testId, sectionId, "A", "First paragraph.")
				.andExpect(status().isCreated());

		addParagraph(teacher.accessToken(), testId, sectionId, "A", "Duplicate label.")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void paragraphsCanBeCreatedWithoutLabels() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-no-label");
		Long testId = createDraft(teacher.accessToken(), "Unlabeled Paragraph Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSection(teacher.accessToken(), testId, "Reading Passage 1")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);

		addParagraphWithoutLabel(teacher.accessToken(), testId, sectionId, "First unlabeled paragraph.")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].label").doesNotExist())
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].paragraphOrder").value(1));

		addParagraphWithoutLabel(teacher.accessToken(), testId, sectionId, "Second unlabeled paragraph.")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].paragraphs[1].label").doesNotExist())
				.andExpect(jsonPath("$.data.sections[0].paragraphs[1].paragraphOrder").value(2));
	}

	@Test
	void questionCannotLinkToMissingParagraphLabel() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-missing-link");
		Long testId = createDraft(teacher.accessToken(), "Missing Paragraph Link Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSection(teacher.accessToken(), testId, "Reading Passage 1")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);

		addQuestion(teacher.accessToken(), testId, sectionId, "Z")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		addQuestion(teacher.accessToken(), testId, sectionId, "   ")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].questions[0].relatedParagraphLabel").doesNotExist());
	}

	@Test
	void teacherCanAuthorObjectiveQuestionOptionsAnswerKeysAndExplanations() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-answer-key");
		Long testId = createDraft(teacher.accessToken(), "Answer Key Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSection(teacher.accessToken(), testId, "Reading Passage 1")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);

		Long questionId = new ResultWithJson(mockMvc.perform(
				post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "multiple_choice",
								  "questionDocument": {
								    "version": 1,
								    "type": "doc",
								    "content": [
								      { "type": "paragraph", "content": [{ "type": "text", "text": "Which answer is correct?" }] }
								    ]
								  },
								  "instructions": "Choose one answer.",
								  "score": 2,
								  "options": [
								    {
								      "optionLabel": "A",
								      "optionDocument": {
								        "version": 1,
								        "type": "doc",
								        "content": [
								          { "type": "paragraph", "content": [{ "type": "text", "text": "First option" }] }
								        ]
								      }
								    },
								    { "optionLabel": "B", "optionText": "Second option" }
								  ],
								  "answers": [
								    {
								      "answerValue": " B ",
								      "primary": true,
								      "explanationDocument": {
								        "version": 1,
								        "type": "doc",
								        "content": [
								          { "type": "paragraph", "content": [{ "type": "text", "text": "B is supported by the passage." }] }
								        ]
								      }
								    },
								    { "answerValue": "second-option", "primary": false }
								  ]
								}
								""")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].questions[0].id").exists())
				.andExpect(jsonPath("$.data.sections[0].questions[0].questionType").value("multiple_choice"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].score").value(2.0))
				.andExpect(jsonPath("$.data.sections[0].questions[0].options[0].optionLabel").value("A"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].options[0].optionText").value("First option"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].options[0].optionTextContent.renderedHtml").value("<p>First option</p>"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].answerKeys[0].answerValue").value("B"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].answerKeys[0].normalizedValue").value("b"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].answerKeys[0].primary").value(true))
				.andExpect(jsonPath("$.data.sections[0].questions[0].answerKeys[0].explanationContent.renderedHtml")
						.value("<p>B is supported by the passage.</p>"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].answerKeys[1].normalizedValue").value("second option"))
				.andReturnQuestionId(0, 0);

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions/{questionId}",
						testId, sectionId, questionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "sentence_completion",
								  "answers": [
								    { "answerValue": "  The   River, Nile!  ", "primary": true }
								  ]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].questions[0].questionType").value("sentence_completion"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].options.length()").value(0))
				.andExpect(jsonPath("$.data.sections[0].questions[0].answerKeys[0].answerValue").value("The   River, Nile!"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].answerKeys[0].normalizedValue").value("the river nile"));
	}

	@Test
	void objectiveQuestionValidationRequiresSupportedTypeOptionsAndAnswers() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-question-validation");
		Long testId = createDraft(teacher.accessToken(), "Question Validation Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSection(teacher.accessToken(), testId, "Reading Passage 1")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "drag_and_drop",
								  "questionText": "Unsupported?",
								  "answers": [{ "answerValue": "A" }]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "multiple_choice",
								  "questionText": "Which answer is correct?",
								  "answers": [{ "answerValue": "A" }]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "short_answer",
								  "questionText": "Name one answer."
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void nonOptionQuestionTypesRejectOptionsOnCreateAndUpdate() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-question-no-options");
		Long testId = createDraft(teacher.accessToken(), "Question Options Validation Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSection(teacher.accessToken(), testId, "Reading Passage 1")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "short_answer",
								  "questionText": "Name one answer.",
								  "options": [
								    { "optionLabel": "A", "optionText": "Unsupported option" }
								  ],
								  "answers": [{ "answerValue": "River" }]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		Long shortAnswerQuestionId = new ResultWithJson(mockMvc.perform(
				post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "short_answer",
								  "questionText": "Name one answer.",
								  "answers": [{ "answerValue": "River" }]
								}
								""")))
				.andExpect(status().isCreated())
				.andReturnQuestionId(0, 0);

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions/{questionId}",
						testId, sectionId, shortAnswerQuestionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "options": [
								    { "optionLabel": "A", "optionText": "Unsupported option" }
								  ]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void richTextValidationAppliesToQuestionOptionsAndExplanations() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-question-rich-bad");
		Long testId = createDraft(teacher.accessToken(), "Unsafe Question Rich Text Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSection(teacher.accessToken(), testId, "Reading Passage 1")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "multiple_choice",
								  "questionText": "Which answer is correct?",
								  "options": [
								    {
								      "optionLabel": "A",
								      "optionDocument": {
								        "version": 1,
								        "type": "doc",
								        "content": [
								          { "type": "script", "content": [{ "type": "text", "text": "bad" }] }
								        ]
								      }
								    }
								  ],
								  "answers": [{ "answerValue": "A" }]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "short_answer",
								  "questionText": "Name one answer.",
								  "answers": [
								    {
								      "answerValue": "River",
								      "explanationDocument": {
								        "version": 1,
								        "type": "doc",
								        "content": [
								          { "type": "image", "attrs": { "src": "https://example.test/bad.png", "alt": "Bad" } }
								        ]
								      }
								    }
								  ]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void segmentWithInvalidTimeRangeIsRejected() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-segment-time");
		Long audioFileId = createFile(teacher.userId(), "listening_audio", "listening.mp3", "audio/mpeg");
		Long testId = createDraft(teacher.accessToken(), "Listening Segment Draft", "listening")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSectionWithAudio(teacher.accessToken(), testId, "Listening Part 1", audioFileId)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].audioFileId").value(audioFileId))
				.andReturnSectionId(0);

		addSegment(teacher.accessToken(), testId, sectionId, "Questions 1-4", 30, 30)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		addSegment(teacher.accessToken(), testId, sectionId, "Questions 1-4", 5, 30)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].listeningSegments[0].startSeconds").value(5))
				.andExpect(jsonPath("$.data.sections[0].listeningSegments[0].endSeconds").value(30));
	}

	@Test
	void questionCannotLinkToSegmentFromAnotherSection() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-segment-link");
		Long audioFileId = createFile(teacher.userId(), "listening_audio", "listening.mp3", "audio/mpeg");
		Long testId = createDraft(teacher.accessToken(), "Listening Question Link Draft", "listening")
				.andExpect(status().isCreated())
				.andReturnId();
		Long firstSectionId = addSectionWithAudio(teacher.accessToken(), testId, "Listening Part 1", audioFileId)
				.andExpect(status().isCreated())
				.andReturnSectionId(0);
		Long secondSectionId = addSectionWithAudio(teacher.accessToken(), testId, "Listening Part 2", audioFileId)
				.andExpect(status().isCreated())
				.andReturnSectionId(1);
		Long segmentId = addSegment(teacher.accessToken(), testId, firstSectionId, "Questions 1-4", 0, 60)
				.andExpect(status().isCreated())
				.andReturnSegmentId(0, 0);

		addQuestionWithSegment(teacher.accessToken(), testId, secondSectionId, segmentId)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		addQuestionWithSegment(teacher.accessToken(), testId, firstSectionId, segmentId)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].questions[0].listeningSegmentId").value(segmentId));
	}

	@Test
	void questionListeningSegmentLinkCanBeClearedWithNullPayload() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-segment-clear");
		Long audioFileId = createFile(teacher.userId(), "listening_audio", "listening.mp3", "audio/mpeg");
		Long testId = createDraft(teacher.accessToken(), "Listening Question Clear Draft", "listening")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSectionWithAudio(teacher.accessToken(), testId, "Listening Part 1", audioFileId)
				.andExpect(status().isCreated())
				.andReturnSectionId(0);
		Long segmentId = addSegment(teacher.accessToken(), testId, sectionId, "Questions 1-4", 0, 60)
				.andExpect(status().isCreated())
				.andReturnSegmentId(0, 0);
		Long questionId = addQuestionWithSegment(teacher.accessToken(), testId, sectionId, segmentId)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].questions[0].listeningSegmentId").value(segmentId))
				.andReturnQuestionId(0, 0);

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions/{questionId}",
						testId, sectionId, questionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "listeningSegmentId": null
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].questions[0].id").value(questionId))
				.andExpect(jsonPath("$.data.sections[0].questions[0].listeningSegmentId").doesNotExist());
	}

	@Test
	void questionPatchPreservesListeningSegmentLinkWhenFieldIsOmitted() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-segment-preserve");
		Long audioFileId = createFile(teacher.userId(), "listening_audio", "listening.mp3", "audio/mpeg");
		Long testId = createDraft(teacher.accessToken(), "Listening Question Preserve Draft", "listening")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSectionWithAudio(teacher.accessToken(), testId, "Listening Part 1", audioFileId)
				.andExpect(status().isCreated())
				.andReturnSectionId(0);
		Long segmentId = addSegment(teacher.accessToken(), testId, sectionId, "Questions 1-4", 0, 60)
				.andExpect(status().isCreated())
				.andReturnSegmentId(0, 0);
		Long questionId = addQuestionWithSegment(teacher.accessToken(), testId, sectionId, segmentId)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].questions[0].listeningSegmentId").value(segmentId))
				.andReturnQuestionId(0, 0);

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions/{questionId}",
						testId, sectionId, questionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionText": "What does the speaker say now?",
								  "score": 2,
								  "options": [
								    { "optionLabel": "A", "optionText": "Updated first option" },
								    { "optionLabel": "B", "optionText": "Updated second option" }
								  ]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].questions[0].id").value(questionId))
				.andExpect(jsonPath("$.data.sections[0].questions[0].questionText").value("What does the speaker say now?"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].score").value(2.0))
				.andExpect(jsonPath("$.data.sections[0].questions[0].options[0].optionText").value("Updated first option"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].listeningSegmentId").value(segmentId));
	}

	@Test
	void listeningTestSubmitFailsWithoutRequiredSectionAudio() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-listening-audio");
		Long testId = createDraft(teacher.accessToken(), "Listening Missing Audio Draft", "listening")
				.andExpect(status().isCreated())
				.andReturnId();
		addSection(teacher.accessToken(), testId, "Listening Part 1")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].audioFileId").doesNotExist());

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/submit", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void sectionAudioFileMustUseListeningAudioPurpose() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-audio-purpose");
		Long passageImageFileId = createFile(teacher.userId(), "passage_image");
		Long testId = createDraft(teacher.accessToken(), "Listening Wrong Audio Draft", "listening")
				.andExpect(status().isCreated())
				.andReturnId();

		addSectionWithAudio(teacher.accessToken(), testId, "Listening Part 1", passageImageFileId)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void sectionAudioFileMustBelongToCurrentTeacher() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-audio-owner");
		TeacherRegistration otherTeacher = approvedTeacher("builder-audio-other");
		Long otherTeacherAudioFileId = createFile(
				otherTeacher.userId(),
				"listening_audio",
				"other-listening.mp3",
				"audio/mpeg");
		Long testId = createDraft(teacher.accessToken(), "Listening Other Audio Draft", "listening")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = addSection(teacher.accessToken(), testId, "Listening Part 1")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}/sections/{sectionId}", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "audioFileId": %d
								}
								""".formatted(otherTeacherAudioFileId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void richTextRejectsUnsafeDocumentsAndUnknownFileEmbeds() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-rich-bad");
		Long testId = createDraft(teacher.accessToken(), "Unsafe Rich Text Draft")
				.andExpect(status().isCreated())
				.andReturnId();

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections", testId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Unsafe Section",
								  "passageDocument": {
								    "version": 1,
								    "type": "doc",
								    "content": [
								      { "type": "script", "content": [{ "type": "text", "text": "bad" }] }
								    ]
								  }
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		Long sectionId = addSection(teacher.accessToken(), testId, "Reading Passage 1")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "label": "A",
								  "contentDocument": {
								    "version": 1,
								    "type": "doc",
								    "content": [
								      { "type": "image", "attrs": { "src": "https://example.test/image.png", "alt": "Bad" } }
								    ]
								  }
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "label": "A",
								  "contentDocument": {
								    "version": 1,
								    "type": "doc",
								    "content": [
								      { "type": "image", "attrs": { "fileId": 999999, "alt": "Missing" } }
								    ]
								  }
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void emptyRichTextDoesNotSatisfyRequiredReadingFields() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-rich-empty");
		Long testId = createDraft(teacher.accessToken(), "Empty Rich Text Draft")
				.andExpect(status().isCreated())
				.andReturnId();

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections", testId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Empty Section",
								  "passageDocument": {
								    "version": 1,
								    "type": "doc",
								    "content": [
								      { "type": "paragraph", "content": [{ "type": "text", "text": "   " }] }
								    ]
								  }
								}
				"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].passageText").value(""))
				.andExpect(jsonPath("$.data.sections[0].passageTextContent.contentText").value(""));

		addSection(teacher.accessToken(), testId, "Reading Passage 2")
				.andExpect(status().isCreated());
		addSection(teacher.accessToken(), testId, "Reading Passage 3")
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/submit", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void teacherPreviewReturnsSanitizedRichTextProjections() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-rich-preview");
		Long imageFileId = createFile(teacher.userId(), "passage_image");
		Long testId = createDraft(teacher.accessToken(), "Rich Preview Draft")
				.andExpect(status().isCreated())
				.andReturnId();

		Long sectionId = new ResultWithJson(mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections", testId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Reading Passage 1",
								  "instructionsDocument": {
								    "version": 1,
								    "type": "doc",
								    "content": [
								      { "type": "paragraph", "content": [
								        { "type": "text", "text": "Use safe links only ", "marks": [{ "type": "link", "attrs": { "href": "javascript:alert(1)" } }] },
								        { "type": "text", "text": "please" }
								      ] }
								    ]
								  },
								  "passageDocument": {
								    "version": 1,
								    "type": "doc",
								    "content": [
								      { "type": "heading", "attrs": { "level": 2 }, "content": [{ "type": "text", "text": "Solar <Energy>" }] },
								      { "type": "paragraph", "content": [{ "type": "text", "text": "A passage with ", "marks": [{ "type": "bold" }] }, { "type": "text", "text": "formatting." }] },
								      { "type": "image", "attrs": { "fileId": %d, "alt": "Panel <diagram>" } }
								    ]
								  },
								  "durationMinutes": 20
								}
								""".formatted(imageFileId))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].passageText").value("Solar <Energy> A passage with formatting. Panel <diagram>"))
				.andExpect(jsonPath("$.data.sections[0].passageTextContent.contentText").value("Solar <Energy> A passage with formatting. Panel <diagram>"))
				.andExpect(jsonPath("$.data.sections[0].passageTextContent.renderedHtml").value("<h2>Solar &lt;Energy&gt;</h2><p><strong>A passage with </strong>formatting.</p><figure data-file-id=\"%d\"><img data-file-id=\"%d\" alt=\"Panel &lt;diagram&gt;\"></figure>".formatted(imageFileId, imageFileId)))
				.andExpect(jsonPath("$.data.sections[0].instructionsContent.renderedHtml").value("<p>Use safe links only please</p>"))
				.andReturnSectionId(0);

		addParagraph(teacher.accessToken(), testId, sectionId, "A", "Legacy <plain> paragraph.")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].contentText").value("Legacy <plain> paragraph."))
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].renderedHtml").value("<p>Legacy &lt;plain&gt; paragraph.</p>"));

		mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "matching_information",
								  "questionDocument": {
								    "version": 1,
								    "type": "doc",
								    "content": [
								      { "type": "paragraph", "content": [{ "type": "text", "text": "Which paragraph mentions solar panels?" }] }
								    ]
								  },
								  "instructionsDocument": {
								    "version": 1,
								    "type": "doc",
								    "content": [
								      { "type": "blockquote", "content": [{ "type": "paragraph", "content": [{ "type": "text", "text": "Choose one paragraph." }] }] }
								    ]
								  },
								  "score": 1,
								  "relatedParagraphLabel": "A",
								  "options": [
								    { "optionLabel": "A", "optionText": "Paragraph A" },
								    { "optionLabel": "B", "optionText": "Paragraph B" }
								  ],
								  "answers": [
								    { "answerValue": "A", "primary": true }
								  ]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.sections[0].questions[0].questionText").value("Which paragraph mentions solar panels?"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].questionTextContent.renderedHtml").value("<p>Which paragraph mentions solar panels?</p>"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].instructionsContent.renderedHtml").value("<blockquote><p>Choose one paragraph.</p></blockquote>"));

		mockMvc.perform(get("/api/v1/teacher/tests/{testId}/preview", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].passageTextContent.contentDocument.version").value(1))
				.andExpect(jsonPath("$.data.sections[0].passageTextContent.renderedHtml").value("<h2>Solar &lt;Energy&gt;</h2><p><strong>A passage with </strong>formatting.</p><figure data-file-id=\"%d\"><img data-file-id=\"%d\" alt=\"Panel &lt;diagram&gt;\"></figure>".formatted(imageFileId, imageFileId)))
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].renderedHtml").value("<p>Legacy &lt;plain&gt; paragraph.</p>"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].questionTextContent.contentText").value("Which paragraph mentions solar panels?"));
	}

	@Test
	void teacherPreviewRerendersStoredRichTextDocumentsInsteadOfTrustingCachedHtml() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-rich-cache");
		Long testId = createDraft(teacher.accessToken(), "Rich Cache Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long sectionId = new ResultWithJson(mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections", testId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Reading Passage 1",
								  "passageDocument": {
								    "version": 1,
								    "type": "doc",
								    "content": [
								      { "type": "paragraph", "content": [{ "type": "text", "text": "Safe <passage>" }] }
								    ]
								  },
								  "durationMinutes": 20
								}
								""")))
				.andExpect(status().isCreated())
				.andReturnSectionId(0);
		Long paragraphId = new ResultWithJson(mockMvc.perform(
				post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs", testId, sectionId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "contentDocument": {
								    "version": 1,
								    "type": "doc",
								    "content": [
								      { "type": "paragraph", "content": [{ "type": "text", "text": "Safe <paragraph>" }] }
								    ]
								  }
								}
								""")))
				.andExpect(status().isCreated())
				.andReturnParagraphId(0, 0);
		jdbcTemplate.update("update test_sections set passage_text_rendered_html = ? where id = ?",
				"<img src=x onerror=alert(1)>", sectionId);
		jdbcTemplate.update("update reading_paragraphs set content_rendered_html = ? where id = ?",
				"<script>alert(1)</script>", paragraphId);

		mockMvc.perform(get("/api/v1/teacher/tests/{testId}/preview", testId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].passageTextContent.renderedHtml")
						.value("<p>Safe &lt;passage&gt;</p>"))
				.andExpect(jsonPath("$.data.sections[0].paragraphs[0].renderedHtml")
						.value("<p>Safe &lt;paragraph&gt;</p>"));
	}

	@Test
	void reorderingSectionsPreservesUniqueOrder() throws Exception {
		TeacherRegistration teacher = approvedTeacher("builder-reorder");
		Long testId = createDraft(teacher.accessToken(), "Reorder Draft")
				.andExpect(status().isCreated())
				.andReturnId();
		Long firstId = addSection(teacher.accessToken(), testId, "First")
				.andExpect(status().isCreated())
				.andReturnSectionId(0);
		Long secondId = addSection(teacher.accessToken(), testId, "Second")
				.andExpect(status().isCreated())
				.andReturnSectionId(1);
		Long thirdId = addSection(teacher.accessToken(), testId, "Third")
				.andExpect(status().isCreated())
				.andReturnSectionId(2);

		mockMvc.perform(patch("/api/v1/teacher/tests/{testId}/sections/reorder", testId)
						.header("Authorization", "Bearer " + teacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sectionIds": [%d, %d, %d]
								}
								""".formatted(thirdId, firstId, secondId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].id").value(thirdId))
				.andExpect(jsonPath("$.data.sections[0].sectionOrder").value(1))
				.andExpect(jsonPath("$.data.sections[1].id").value(firstId))
				.andExpect(jsonPath("$.data.sections[1].sectionOrder").value(2))
				.andExpect(jsonPath("$.data.sections[2].id").value(secondId))
				.andExpect(jsonPath("$.data.sections[2].sectionOrder").value(3));

		Integer totalSections = jdbcTemplate.queryForObject(
				"select count(*) from test_sections where test_id = ?",
				Integer.class,
				testId);
		Integer distinctOrders = jdbcTemplate.queryForObject(
				"select count(distinct section_order) from test_sections where test_id = ?",
				Integer.class,
				testId);
		Integer maxOrder = jdbcTemplate.queryForObject(
				"select max(section_order) from test_sections where test_id = ?",
				Integer.class,
				testId);
		assertThat(distinctOrders).isEqualTo(totalSections);
		assertThat(maxOrder).isEqualTo(totalSections);

		mockMvc.perform(delete("/api/v1/teacher/tests/{testId}/sections/{sectionId}", testId, firstId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[*].id", hasItem(thirdId.intValue())))
				.andExpect(jsonPath("$.data.sections[0].sectionOrder").value(1))
				.andExpect(jsonPath("$.data.sections[1].sectionOrder").value(2));
	}

	private ResultWithJson createDraft(String token, String title) throws Exception {
		return createDraft(token, title, "reading");
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

	private ResultWithJson addSectionWithAudio(String token, Long testId, String title, Long audioFileId)
			throws Exception {
		return new ResultWithJson(mockMvc.perform(post("/api/v1/teacher/tests/{testId}/sections", testId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "%s",
								  "instructions": "Listen and answer.",
								  "passageText": "Listening notes for %s.",
								  "audioFileId": %d,
								  "durationMinutes": 20
								}
								""".formatted(title, title, audioFileId))));
	}

	private ResultWithJson addSegment(
			String token,
			Long testId,
			Long sectionId,
			String title,
			Integer startSeconds,
			Integer endSeconds
	) throws Exception {
		return new ResultWithJson(mockMvc.perform(
				post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/listening-segments", testId, sectionId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "%s",
								  "startSeconds": %d,
								  "endSeconds": %d,
								  "transcript": "Speaker transcript."
								}
								""".formatted(title, startSeconds, endSeconds))));
	}

	private ResultWithJson addParagraph(String token, Long testId, Long sectionId, String label, String content)
			throws Exception {
		return new ResultWithJson(mockMvc.perform(
				post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs", testId, sectionId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "label": "%s",
								  "content": "%s"
								}
								""".formatted(label, content))));
	}

	private ResultWithJson addParagraphWithoutLabel(String token, Long testId, Long sectionId, String content)
			throws Exception {
		return new ResultWithJson(mockMvc.perform(
				post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/paragraphs", testId, sectionId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "%s"
								}
								""".formatted(content))));
	}

	private ResultWithJson addQuestion(String token, Long testId, Long sectionId, String relatedParagraphLabel)
			throws Exception {
		return new ResultWithJson(mockMvc.perform(
				post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "matching_information",
								  "questionText": "Which paragraph mentions this idea?",
								  "instructions": "Choose the paragraph.",
								  "score": 1,
								  "relatedParagraphLabel": "%s",
								  "options": [
								    { "optionLabel": "A", "optionText": "Paragraph A" },
								    { "optionLabel": "B", "optionText": "Paragraph B" }
								  ],
								  "answers": [
								    { "answerValue": "A", "primary": true }
								  ]
								}
								""".formatted(relatedParagraphLabel))));
	}

	private ResultWithJson addQuestionWithSegment(String token, Long testId, Long sectionId, Long listeningSegmentId)
			throws Exception {
		return new ResultWithJson(mockMvc.perform(
				post("/api/v1/teacher/tests/{testId}/sections/{sectionId}/questions", testId, sectionId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "questionType": "multiple_choice",
								  "questionText": "What does the speaker say?",
								  "instructions": "Choose one answer.",
								  "score": 1,
								  "listeningSegmentId": %d,
								  "options": [
								    { "optionLabel": "A", "optionText": "First option" },
								    { "optionLabel": "B", "optionText": "Second option" }
								  ],
								  "answers": [
								    { "answerValue": "A", "primary": true }
								  ]
								}
								""".formatted(listeningSegmentId))));
	}

	private Long createFile(Long ownerUserId, String purpose) {
		return createFile(ownerUserId, purpose, "passage.png", "image/png");
	}

	private Long createFile(Long ownerUserId, String purpose, String originalName, String contentType) {
		String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
		String objectKey = purpose + "/" + ownerUserId + "/" + UUID.randomUUID() + extension;
		jdbcTemplate.update("""
				insert into files (owner_user_id, purpose, bucket, object_key, original_name, content_type, size_bytes)
				values (?, ?, 'mock-exams-local', ?, ?, ?, 128)
				""", ownerUserId, purpose, objectKey, originalName, contentType);
		return jdbcTemplate.queryForObject("select id from files where object_key = ?", Long.class, objectKey);
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

		private Long andReturnParagraphId(int sectionIndex, int paragraphIndex) throws Exception {
			return root().at("/data/sections/" + sectionIndex + "/paragraphs/" + paragraphIndex + "/id").asLong();
		}

		private Long andReturnQuestionId(int sectionIndex, int questionIndex) throws Exception {
			return root().at("/data/sections/" + sectionIndex + "/questions/" + questionIndex + "/id").asLong();
		}

		private Long andReturnSegmentId(int sectionIndex, int segmentIndex) throws Exception {
			return root().at("/data/sections/" + sectionIndex + "/listeningSegments/" + segmentIndex + "/id").asLong();
		}

		private JsonNode root() throws Exception {
			String response = resultActions.andReturn().getResponse().getContentAsString();
			return objectMapper.readTree(response);
		}
	}
}
