package com.mockexams.backend.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestCatalogControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanCatalogFixtures() {
		jdbcTemplate.update("delete from test_category_links");
		jdbcTemplate.update("delete from test_tag_links");
		jdbcTemplate.update("delete from question_answers");
		jdbcTemplate.update("delete from question_options");
		jdbcTemplate.update("delete from questions");
		jdbcTemplate.update("delete from test_sections");
		jdbcTemplate.update("delete from tests");
		jdbcTemplate.update("delete from test_categories");
		jdbcTemplate.update("delete from test_tags");
		jdbcTemplate.update("delete from teacher_profiles");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
	}

	@Test
	void draftTestsAreNotReturnedPublicly() throws Exception {
		Long authorId = createAdminUser("catalog-draft-author");
		Long publishedId = createTest(authorId, null, "Published Reading", "reading", "published", true);
		Long draftId = createTest(authorId, null, "Draft Reading", "reading", "draft", true);

		mockMvc.perform(get("/api/v1/public/tests"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(publishedId.intValue())))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(draftId.intValue()))))
				.andExpect(jsonPath("$.meta.filters.visibility").value("published"));
	}

	@Test
	void teacherFilterReturnsOnlyApprovedPublishedTests() throws Exception {
		Long approvedTeacherUserId = createUser("catalog-approved-teacher");
		Long pendingTeacherUserId = createUser("catalog-pending-teacher");
		Long approvedTeacherId = createTeacherProfile(approvedTeacherUserId, "approved");
		Long pendingTeacherId = createTeacherProfile(pendingTeacherUserId, "pending_approval");
		Long approvedTestId = createTest(approvedTeacherUserId, approvedTeacherId, "Approved Teacher Test", "reading",
				"published", true);
		Long pendingTestId = createTest(pendingTeacherUserId, pendingTeacherId, "Pending Teacher Test", "reading",
				"published", true);

		mockMvc.perform(get("/api/v1/public/tests").param("teacherId", approvedTeacherId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(approvedTestId.intValue())))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(pendingTestId.intValue()))))
				.andExpect(jsonPath("$.data[*].teacher.id", containsInAnyOrder(approvedTeacherId.intValue())));
	}

	@Test
	void priceAndTypeFiltersCombineCorrectly() throws Exception {
		Long authorId = createAdminUser("catalog-price-type-author");
		Long freeReadingId = createTest(authorId, null, "Free Reading", "reading", "published", true);
		Long paidReadingId = createTest(authorId, null, "Paid Reading", "reading", "published", false);
		Long paidListeningId = createTest(authorId, null, "Paid Listening", "listening", "published", false);

		mockMvc.perform(get("/api/v1/public/tests")
						.param("type", "reading")
						.param("price", "paid"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(paidReadingId.intValue())))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(freeReadingId.intValue()))))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(paidListeningId.intValue()))))
				.andExpect(jsonPath("$.meta.filters.type").value("reading"))
				.andExpect(jsonPath("$.meta.filters.price").value("paid"));
	}

	@Test
	void archivedTestsAreHidden() throws Exception {
		Long authorId = createAdminUser("catalog-archived-author");
		Long publishedId = createTest(authorId, null, "Visible Test", "reading", "published", true);
		Long archivedId = createTest(authorId, null, "Archived Test", "reading", "archived", true);
		Long deletedId = createTest(authorId, null, "Deleted Test", "reading", "published", true);
		jdbcTemplate.update("update tests set deleted_at = ? where id = ?", OffsetDateTime.now(ZoneOffset.UTC), deletedId);

		mockMvc.perform(get("/api/v1/public/tests"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(publishedId.intValue())))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(archivedId.intValue()))))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(deletedId.intValue()))));
	}

	@Test
	void detailEndpointAppliesPublicVisibilityRules() throws Exception {
		Long authorId = createAdminUser("catalog-detail-author");
		Long publishedId = createTest(authorId, null, "Visible Detail", "reading", "published", true);
		Long rejectedId = createTest(authorId, null, "Rejected Detail", "reading", "rejected", true);

		mockMvc.perform(get("/api/v1/public/tests/{testId}", publishedId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(publishedId))
				.andExpect(jsonPath("$.data.title").value("Visible Detail"));

		mockMvc.perform(get("/api/v1/public/tests/{testId}", rejectedId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEST_NOT_FOUND"));
	}

	@Test
	void studentFacingDetailHidesAnswerKeysScoresAndExplanations() throws Exception {
		Long authorId = createAdminUser("catalog-safe-preview-author");
		Long testId = createTest(authorId, null, "Safe Student Preview", "reading", "published", true);
		Long sectionId = createSection(testId, "Reading Passage 1");
		Long questionId = createQuestion(sectionId, "multiple_choice", "Which answer is correct?", 2);
		createOption(questionId, "A", "First option");
		createOption(questionId, "B", "Second option");
		createAnswer(questionId, "B", "b", true, "B is explained here.");
		createAnswer(questionId, "second option", "second option", false, "Variant explanation.");

		mockMvc.perform(get("/api/v1/public/tests/{testId}", testId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sections[0].title").value("Reading Passage 1"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].id").value(questionId))
				.andExpect(jsonPath("$.data.sections[0].questions[0].questionType").value("multiple_choice"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].questionText").value("Which answer is correct?"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].options[0].optionLabel").value("A"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].options[1].optionText").value("Second option"))
				.andExpect(jsonPath("$.data.sections[0].questions[0].score").doesNotExist())
				.andExpect(jsonPath("$.data.sections[0].questions[0].answerKeys").doesNotExist())
				.andExpect(jsonPath("$.data.sections[0].questions[0].answers").doesNotExist())
				.andExpect(jsonPath("$.data.sections[0].questions[0].acceptedVariants").doesNotExist())
				.andExpect(jsonPath("$.data.sections[0].questions[0].explanation").doesNotExist());
	}

	@Test
	void nullTeacherTestsArePublicOnlyWhenCreatedByAdmin() throws Exception {
		Long adminUserId = createAdminUser("catalog-official-author");
		Long teacherUserId = createUserWithRole("catalog-detached-teacher", "teacher");
		Long teacherProfileId = createTeacherProfile(teacherUserId, "approved");
		Long officialTestId = createTest(adminUserId, null, "Official Platform Test", "reading", "published", true);
		Long detachedTeacherTestId = createTest(teacherUserId, teacherProfileId, "Detached Teacher Test", "reading",
				"published", true);
		jdbcTemplate.update("delete from teacher_profiles where id = ?", teacherProfileId);

		mockMvc.perform(get("/api/v1/public/tests"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(officialTestId.intValue())))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(detachedTeacherTestId.intValue()))));

		mockMvc.perform(get("/api/v1/public/tests/{testId}", detachedTeacherTestId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEST_NOT_FOUND"));
	}

	@Test
	void categoryAndTagEndpointsReturnReadModels() throws Exception {
		Long categoryId = createCategory("IELTS Reading", "ielts-reading", true);
		Long inactiveCategoryId = createCategory("Inactive", "inactive", false);
		Long tagId = createTag("Academic", "academic");

		mockMvc.perform(get("/api/v1/public/test-categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(categoryId.intValue())))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(inactiveCategoryId.intValue()))));

		mockMvc.perform(get("/api/v1/public/test-tags"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(tagId.intValue())))
				.andExpect(jsonPath("$.data[*].slug", hasItem("academic")));
	}

	@Test
	void inactiveLinkedCategoriesAreHiddenFromListAndDetailResponses() throws Exception {
		Long authorId = createAdminUser("catalog-category-response-author");
		Long testId = createTest(authorId, null, "Categorized Test", "reading", "published", true);
		Long activeCategoryId = createCategory("Active Reading", "active-reading", true);
		Long inactiveCategoryId = createCategory("Inactive Reading", "inactive-reading", false);
		linkCategory(testId, activeCategoryId);
		linkCategory(testId, inactiveCategoryId);

		mockMvc.perform(get("/api/v1/public/tests"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].categories[*].slug", hasItem("active-reading")))
				.andExpect(jsonPath("$.data[0].categories[*].slug", not(hasItem("inactive-reading"))));

		mockMvc.perform(get("/api/v1/public/tests/{testId}", testId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.categories[*].slug", hasItem("active-reading")))
				.andExpect(jsonPath("$.data.categories[*].slug", not(hasItem("inactive-reading"))));
	}

	@Test
	void categoryFilterIgnoresInactiveCategorySlugs() throws Exception {
		Long authorId = createAdminUser("catalog-category-filter-author");
		Long testId = createTest(authorId, null, "Filtered Category Test", "reading", "published", true);
		Long activeCategoryId = createCategory("Visible Category", "visible-category", true);
		Long inactiveCategoryId = createCategory("Hidden Category", "hidden-category", false);
		linkCategory(testId, activeCategoryId);
		linkCategory(testId, inactiveCategoryId);

		mockMvc.perform(get("/api/v1/public/tests").param("category", "visible-category"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(testId.intValue())));

		mockMvc.perform(get("/api/v1/public/tests").param("category", "hidden-category"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	void unknownSortOrFilterValuesFailValidation() throws Exception {
		mockMvc.perform(get("/api/v1/public/tests").param("sort", "unknown,desc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(get("/api/v1/public/tests").param("price", "discounted"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(get("/api/v1/public/tests").param("unknown", "value"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		mockMvc.perform(get("/api/v1/public/tests").param("bandMin", "not-a-number"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	private Long createUser(String emailPrefix) {
		return insertAndReturnId("""
				insert into users (email, password_hash, full_name, status, preferred_language)
				values (?, 'hash', 'Catalog Fixture', 'active', 'en')
				""", uniqueEmail(emailPrefix));
	}

	private Long createAdminUser(String emailPrefix) {
		return createUserWithRole(emailPrefix, "admin");
	}

	private Long createUserWithRole(String emailPrefix, String roleCode) {
		Long userId = createUser(emailPrefix);
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select ?, r.id
				from roles r
				where r.code = ?
				""", userId, roleCode);
		return userId;
	}

	private Long createTeacherProfile(Long userId, String status) {
		if ("approved".equals(status)) {
			return insertAndReturnId("""
					insert into teacher_profiles (
					  user_id, display_name, headline, bio, status, approved_by_user_id, approved_at
					)
					values (?, 'Approved Teacher', 'IELTS mentor', 'Catalog fixture teacher.', 'approved', ?, ?)
					""", userId, userId, OffsetDateTime.now(ZoneOffset.UTC));
		}
		return insertAndReturnId("""
				insert into teacher_profiles (user_id, display_name, headline, bio, status)
				values (?, 'Pending Teacher', 'IELTS mentor', 'Catalog fixture teacher.', ?)
				""", userId, status);
	}

	private Long createTest(
			Long authorUserId,
			Long teacherProfileId,
			String title,
			String type,
			String visibility,
			boolean free
	) {
		BigDecimal price = free ? BigDecimal.ZERO : new BigDecimal("15000.00");
		return insertAndReturnId("""
				insert into tests (
				  teacher_profile_id, title, description, type, difficulty, estimated_band_min,
				  estimated_band_max, duration_minutes, price_amount, currency, is_free, language,
				  visibility, published_at, created_by_user_id
				)
				values (?, ?, 'Practice test fixture.', ?, 'intermediate', 5.5, 7.0, 60, ?, 'UZS', ?, 'en', ?, ?, ?)
				""",
				teacherProfileId,
				title,
				type,
				price,
				free,
				visibility,
				OffsetDateTime.now(ZoneOffset.UTC),
				authorUserId);
	}

	private Long createSection(Long testId, String title) {
		return insertAndReturnId("""
				insert into test_sections (
				  test_id, title, instructions, passage_text, section_order, duration_minutes
				)
				values (?, ?, 'Read and answer.', 'Passage text.', 1, 20)
				""", testId, title);
	}

	private Long createQuestion(Long sectionId, String questionType, String questionText, int score) {
		return insertAndReturnId("""
				insert into questions (
				  section_id, question_type, question_text, instructions, score, question_order
				)
				values (?, ?, ?, 'Choose one answer.', ?, 1)
				""", sectionId, questionType, questionText, score);
	}

	private void createOption(Long questionId, String label, String optionText) {
		jdbcTemplate.update("""
				insert into question_options (question_id, option_label, option_text, option_order)
				values (?, ?, ?, (select coalesce(max(option_order), 0) + 1 from question_options where question_id = ?))
				""", questionId, label, optionText, questionId);
	}

	private void createAnswer(Long questionId, String answerValue, String normalizedValue, boolean primary, String explanation) {
		jdbcTemplate.update("""
				insert into question_answers (
				  question_id, answer_value, normalized_value, is_primary, answer_order, explanation
				)
				values (?, ?, ?, ?, (select coalesce(max(answer_order), 0) + 1 from question_answers where question_id = ?), ?)
				""", questionId, answerValue, normalizedValue, primary, questionId, explanation);
	}

	private Long createCategory(String name, String slug, boolean active) {
		return insertAndReturnId("""
				insert into test_categories (name, slug, sort_order, is_active)
				values (?, ?, 10, ?)
				""", name, slug, active);
	}

	private Long createTag(String name, String slug) {
		return insertAndReturnId("""
				insert into test_tags (name, slug)
				values (?, ?)
				""", name, slug);
	}

	private void linkCategory(Long testId, Long categoryId) {
		jdbcTemplate.update("""
				insert into test_category_links (test_id, category_id)
				values (?, ?)
				""", testId, categoryId);
	}

	private Long insertAndReturnId(String sql, Object... args) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql, new String[] { "id" });
			for (int i = 0; i < args.length; i++) {
				statement.setObject(i + 1, args[i]);
			}
			return statement;
		}, keyHolder);
		return keyHolder.getKey().longValue();
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}
}
