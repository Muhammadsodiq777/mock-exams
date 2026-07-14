package com.mockexams.backend.controller;

import static com.mockexams.backend.support.ApiContractAssertions.errorEnvelope;
import static com.mockexams.backend.support.ApiContractAssertions.pageEnvelope;
import static com.mockexams.backend.support.ApiContractAssertions.validationFieldError;
import static org.hamcrest.Matchers.hasItem;
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
class ApiContractControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanFixtures() {
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
	void publicCatalogListUsesStandardPageEnvelope() throws Exception {
		Long authorId = createAdminUser("contract-catalog-author");
		Long testId = createPublishedTest(authorId, "Contract Reading");

		mockMvc.perform(get("/api/v1/public/tests")
						.param("type", "reading"))
				.andExpect(status().isOk())
				.andExpect(pageEnvelope())
				.andExpect(jsonPath("$.data[*].id", hasItem(testId.intValue())))
				.andExpect(jsonPath("$.meta.sort[0].field").value("publishedAt"))
				.andExpect(jsonPath("$.meta.sort[0].direction").value("desc"))
				.andExpect(jsonPath("$.meta.filters.visibility").value("published"))
				.andExpect(jsonPath("$.meta.filters.type").value("reading"));
	}

	@Test
	void publicCatalogSortAcceptsDocumentedFieldDirectionForm() throws Exception {
		mockMvc.perform(get("/api/v1/public/tests").param("sort", "publishedAt", "desc"))
				.andExpect(status().isOk())
				.andExpect(pageEnvelope())
				.andExpect(jsonPath("$.meta.sort[0].field").value("publishedAt"))
				.andExpect(jsonPath("$.meta.sort[0].direction").value("desc"));
	}

	@Test
	void validationFailuresUseStandardErrorEnvelopeWithFieldCodes() throws Exception {
		mockMvc.perform(get("/api/v1/public/tests").param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(validationFieldError("size", "Max"));
	}

	@Test
	void businessFailuresUseStandardErrorEnvelopeWithStableCode() throws Exception {
		mockMvc.perform(get("/api/v1/public/tests").param("unknown", "value"))
				.andExpect(status().isBadRequest())
				.andExpect(errorEnvelope("VALIDATION_FAILED"));
	}

	private Long createAdminUser(String emailPrefix) {
		Long userId = insertAndReturnId("""
				insert into users (email, password_hash, full_name, status, preferred_language)
				values (?, 'hash', 'Contract Fixture', 'active', 'en')
				""", uniqueEmail(emailPrefix));
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select ?, r.id
				from roles r
				where r.code = 'admin'
				""", userId);
		return userId;
	}

	private Long createPublishedTest(Long authorUserId, String title) {
		return insertAndReturnId("""
				insert into tests (
				  title, description, type, difficulty, estimated_band_min, estimated_band_max,
				  duration_minutes, price_amount, currency, is_free, language, visibility,
				  published_at, created_by_user_id
				)
				values (?, 'Contract fixture.', 'reading', 'intermediate', 5.5, 7.0, 60, ?, 'UZS',
				  true, 'en', 'published', ?, ?)
				""",
				title,
				BigDecimal.ZERO,
				OffsetDateTime.now(ZoneOffset.UTC),
				authorUserId);
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
