package com.mockexams.backend.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.mockexams.backend.auth.service.PasswordHasher;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordHasher passwordHasher;

	@BeforeEach
	void cleanMarketplaceFixtures() {
		jdbcTemplate.update("delete from marketplace_listing_items");
		jdbcTemplate.update("delete from purchases");
		jdbcTemplate.update("delete from subscriptions");
		jdbcTemplate.update("delete from marketplace_listings");
		jdbcTemplate.update("delete from test_category_links");
		jdbcTemplate.update("delete from test_tag_links");
		jdbcTemplate.update("delete from question_answers");
		jdbcTemplate.update("delete from question_options");
		jdbcTemplate.update("delete from questions");
		jdbcTemplate.update("delete from listening_segments");
		jdbcTemplate.update("delete from reading_paragraphs");
		jdbcTemplate.update("delete from test_sections");
		jdbcTemplate.update("delete from tests");
		jdbcTemplate.update("delete from files");
		jdbcTemplate.update("delete from teacher_profiles");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
	}

	@Test
	void teacherCannotListAnotherTeachersTest() throws Exception {
		TeacherRegistration owner = approvedTeacher("market-owner");
		TeacherRegistration otherTeacher = approvedTeacher("market-other");
		Long ownerTestId = createPublishedTest(owner.userId(), owner.profileId(), "Owner Reading", "reading");

		mockMvc.perform(post("/api/v1/teacher/marketplace/listings")
						.header("Authorization", "Bearer " + otherTeacher.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Borrowed Test Listing",
								  "description": "Should not be allowed.",
								  "listingType": "single_test",
								  "testId": %d,
								  "priceAmount": 0,
								  "currency": "UZS",
								  "language": "en"
								}
								""".formatted(ownerTestId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("LISTING_ITEM_INVALID"));
	}

	@Test
	void packRequiresAtLeastOneItemBeforeActivation() throws Exception {
		TeacherRegistration teacher = approvedTeacher("market-pack");
		Long listingId = createListing(
				teacher.accessToken(),
				"Reading Pack",
				"test_pack",
				null,
				new BigDecimal("15000.00"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.active").value(false))
				.andReturnId();

		mockMvc.perform(post("/api/v1/teacher/marketplace/listings/{listingId}/activate", listingId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("LISTING_STATUS_CONFLICT"));

		mockMvc.perform(get("/api/v1/teacher/marketplace/listings/{listingId}/activation", listingId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.ready").value(false))
				.andExpect(jsonPath("$.data.errors", hasItem("Pack listings require at least one item.")));
	}

	@Test
	void freeSingleTestListingCanBeActivatedWithZeroPriceAndBrowsedPublicly() throws Exception {
		TeacherRegistration teacher = approvedTeacher("market-free");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Free Reading", "reading");
		Long listingId = createListing(
				teacher.accessToken(),
				"Free Reading Listing",
				"single_test",
				testId,
				BigDecimal.ZERO)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.free").value(true))
				.andReturnId();

		mockMvc.perform(post("/api/v1/teacher/marketplace/listings/{listingId}/activate", listingId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.active").value(true));

		mockMvc.perform(get("/api/v1/public/marketplace")
						.param("price", "free")
						.param("type", "single_test"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(listingId.intValue())))
				.andExpect(jsonPath("$.data[0].free").value(true))
				.andExpect(jsonPath("$.meta.filters.status").value("active"))
				.andExpect(jsonPath("$.meta.filters.price").value("free"));
	}

	@Test
	void publicMarketplaceReturnsActiveListingsOnlyFromApprovedTeachersAndValidContent() throws Exception {
		TeacherRegistration approvedTeacher = approvedTeacher("market-approved");
		TeacherRegistration pendingTeacher = pendingTeacher("market-pending");
		Long activeTestId = createPublishedTest(approvedTeacher.userId(), approvedTeacher.profileId(), "Active Test", "reading");
		Long draftTestId = createDraftTest(approvedTeacher.userId(), approvedTeacher.profileId(), "Draft Test", "reading");
		Long activeListingId = createActiveListing(approvedTeacher.profileId(), activeTestId, "Visible Listing", "single_test");
		Long inactiveListingId = createInactiveListing(approvedTeacher.profileId(), activeTestId, "Inactive Listing", "single_test");
		Long pendingListingId = createActiveListing(pendingTeacher.profileId(), null, "Pending Teacher Listing", "vocabulary_pack");
		Long draftContentListingId = createActiveListing(approvedTeacher.profileId(), draftTestId, "Draft Content Listing", "single_test");
		Long mixedItemListingId = createActiveListing(approvedTeacher.profileId(), null, "Mixed Item Listing", "test_pack");
		createListingItem(mixedItemListingId, activeTestId, null, "Published item", 1);
		createListingItem(mixedItemListingId, draftTestId, null, "Draft item", 2);

		mockMvc.perform(get("/api/v1/public/marketplace"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem(activeListingId.intValue())))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(inactiveListingId.intValue()))))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(pendingListingId.intValue()))))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(draftContentListingId.intValue()))))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(mixedItemListingId.intValue()))));
	}

	@Test
	void teacherCanManageItemsDeactivateAndPreviewListing() throws Exception {
		TeacherRegistration teacher = approvedTeacher("market-items");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Pack Test", "reading");
		Long fileId = createCourseMaterialFile(teacher.userId(), "pack-material.pdf");
		Long listingId = createListing(
				teacher.accessToken(),
				"Prepared Pack",
				"writing_course",
				null,
				new BigDecimal("25000.00"))
				.andExpect(status().isCreated())
				.andReturnId();

		createListingItem(listingId, teacher.accessToken(), """
				{
				  "title": "Pack Test",
				  "description": "First pack item.",
				  "testId": %d,
				  "itemOrder": 1
				}
				""".formatted(testId))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.items[0].testId").value(testId));

		Long fileItemId = createListingItem(listingId, teacher.accessToken(), """
				{
				  "title": "Course PDF",
				  "description": "Printable material.",
				  "fileId": %d,
				  "itemOrder": 2
				}
				""".formatted(fileId))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.items[1].fileId").value(fileId))
				.andReturnItemId(1);

		mockMvc.perform(get("/api/v1/teacher/marketplace/listings/{listingId}/preview", listingId)
						.header("Authorization", "Bearer " + teacher.accessToken())
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(listingId))
				.andExpect(jsonPath("$.data.items.length()").value(2));

		mockMvc.perform(post("/api/v1/teacher/marketplace/listings/{listingId}/activate", listingId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.active").value(true));

		mockMvc.perform(delete("/api/v1/teacher/marketplace/listings/{listingId}/items/{itemId}", listingId, fileItemId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/v1/teacher/marketplace/listings/{listingId}/deactivate", listingId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.active").value(false));
	}

	private ResultActionsWithId createListing(
			String accessToken,
			String title,
			String listingType,
			Long testId,
			BigDecimal priceAmount
	) throws Exception {
		String testLine = testId == null ? "" : "\"testId\": %d,".formatted(testId);
		return new ResultActionsWithId(mockMvc.perform(post("/api/v1/teacher/marketplace/listings")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "%s",
						  "description": "Teacher marketplace listing.",
						  "listingType": "%s",
						  %s
						  "priceAmount": %s,
						  "currency": "UZS",
						  "language": "en"
						}
						""".formatted(title, listingType, testLine, priceAmount.toPlainString()))));
	}

	private ResultActionsWithId createListingItem(Long listingId, String accessToken, String body) throws Exception {
		return new ResultActionsWithId(mockMvc.perform(post(
				"/api/v1/teacher/marketplace/listings/{listingId}/items",
				listingId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)));
	}

	private TeacherRegistration approvedTeacher(String prefix) throws Exception {
		return teacher(prefix, "approved");
	}

	private TeacherRegistration pendingTeacher(String prefix) throws Exception {
		return teacher(prefix, "pending_approval");
	}

	private TeacherRegistration teacher(String prefix, String status) throws Exception {
		Long userId = createUserWithRole(prefix, "teacher");
		Long profileId = createTeacherProfile(userId, status);
		String token = login(prefix + "@example.test");
		return new TeacherRegistration(userId, profileId, token);
	}

	private Long createUserWithRole(String prefix, String roleCode) {
		Long userId = createUser(prefix);
		Long roleId = jdbcTemplate.queryForObject("select id from roles where code = ?", Long.class, roleCode);
		jdbcTemplate.update("insert into user_roles (user_id, role_id) values (?, ?)", userId, roleId);
		return userId;
	}

	private Long createUser(String prefix) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
					insert into users (email, password_hash, full_name, status, preferred_language)
					values (?, ?, ?, 'active', 'en')
					""", new String[] { "id" });
			statement.setString(1, prefix + "@example.test");
			statement.setString(2, passwordHasher.hash(PASSWORD));
			statement.setString(3, "Teacher " + prefix);
			return statement;
		}, keyHolder);
		return keyHolder.getKey().longValue();
	}

	private Long createTeacherProfile(Long userId, String status) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
					insert into teacher_profiles (
					  user_id, display_name, headline, bio, status, approved_by_user_id, approved_at
					)
					values (?, ?, 'IELTS teacher', 'Experienced teacher.', ?, ?, ?)
					""", new String[] { "id" });
			statement.setLong(1, userId);
			statement.setString(2, "Teacher " + userId);
			statement.setString(3, status);
			if ("approved".equals(status)) {
				statement.setLong(4, userId);
				statement.setObject(5, OffsetDateTime.now(ZoneOffset.UTC));
			}
			else {
				statement.setObject(4, null);
				statement.setObject(5, null);
			}
			return statement;
		}, keyHolder);
		return keyHolder.getKey().longValue();
	}

	private String login(String email) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s"
								}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString()
				.replaceAll("(?s).*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
	}

	private Long createPublishedTest(Long userId, Long teacherProfileId, String title, String type) {
		return createTest(userId, teacherProfileId, title, type, "published");
	}

	private Long createDraftTest(Long userId, Long teacherProfileId, String title, String type) {
		return createTest(userId, teacherProfileId, title, type, "draft");
	}

	private Long createTest(Long userId, Long teacherProfileId, String title, String type, String visibility) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
					insert into tests (
					  teacher_profile_id, title, description, type, difficulty, estimated_band_min, estimated_band_max,
					  duration_minutes, price_amount, currency, is_free, language, visibility, published_at, created_by_user_id
					)
					values (?, ?, 'Marketplace fixture test.', ?, 'intermediate', 5.5, 7.0, 60, 0, 'UZS', true, 'en', ?, ?, ?)
					""", new String[] { "id" });
			statement.setObject(1, teacherProfileId);
			statement.setString(2, title);
			statement.setString(3, type);
			statement.setString(4, visibility);
			statement.setObject(5, "published".equals(visibility) ? OffsetDateTime.now(ZoneOffset.UTC) : null);
			statement.setLong(6, userId);
			return statement;
		}, keyHolder);
		return keyHolder.getKey().longValue();
	}

	private Long createActiveListing(Long teacherProfileId, Long testId, String title, String listingType) {
		return insertListing(teacherProfileId, testId, title, listingType, true);
	}

	private Long createInactiveListing(Long teacherProfileId, Long testId, String title, String listingType) {
		return insertListing(teacherProfileId, testId, title, listingType, false);
	}

	private Long createListingItem(Long listingId, Long testId, Long fileId, String title, int itemOrder) {
		return insertAndReturnId("""
				insert into marketplace_listing_items (listing_id, test_id, file_id, title, description, item_order)
				values (?, ?, ?, ?, 'Listing item fixture.', ?)
				""",
				listingId,
				testId,
				fileId,
				title,
				itemOrder);
	}

	private Long insertListing(Long teacherProfileId, Long testId, String title, String listingType, boolean active) {
		return insertAndReturnId("""
				insert into marketplace_listings (
				  teacher_profile_id, test_id, title, description, listing_type, price_amount, currency, is_active, language
				)
				values (?, ?, ?, 'Listing fixture.', ?, 0, 'UZS', ?, 'en')
				""",
				teacherProfileId,
				testId,
				title,
				listingType,
				active);
	}

	private Long createCourseMaterialFile(Long ownerUserId, String originalName) {
		return insertAndReturnId("""
				insert into files (owner_user_id, purpose, bucket, object_key, original_name, content_type, size_bytes)
				values (?, 'course_material', 'test-bucket', ?, ?, 'application/pdf', 1024)
				""",
				ownerUserId,
				"course/" + UUID.randomUUID(),
				originalName);
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

	private record TeacherRegistration(Long userId, Long profileId, String accessToken) {
	}

	private final class ResultActionsWithId {

		private final org.springframework.test.web.servlet.ResultActions delegate;

		private ResultActionsWithId(org.springframework.test.web.servlet.ResultActions delegate) {
			this.delegate = delegate;
		}

		private ResultActionsWithId andExpect(org.springframework.test.web.servlet.ResultMatcher matcher)
				throws Exception {
			delegate.andExpect(matcher);
			return this;
		}

		private Long andReturnId() throws Exception {
			String response = delegate.andReturn().getResponse().getContentAsString();
			return objectMapper.readTree(response).at("/data/id").asLong();
		}

		private Long andReturnItemId(int index) throws Exception {
			String response = delegate.andReturn().getResponse().getContentAsString();
			return objectMapper.readTree(response).at("/data/items/" + index + "/id").asLong();
		}
	}
}
