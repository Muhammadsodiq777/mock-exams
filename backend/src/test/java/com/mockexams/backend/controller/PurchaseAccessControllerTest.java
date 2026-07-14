package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.mockexams.backend.auth.service.PasswordHasher;
import com.mockexams.backend.service.AccessControlService;
import com.mockexams.backend.service.AccessGrantRequest;
import com.mockexams.backend.service.FileStorageService;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseAccessControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private AccessControlService accessControlService;

	@MockBean
	private FileStorageService fileStorageService;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("delete from teacher_transactions");
		jdbcTemplate.update("delete from promo_code_redemptions");
		jdbcTemplate.update("delete from subscriptions");
		jdbcTemplate.update("delete from purchases");
		jdbcTemplate.update("delete from payments");
		jdbcTemplate.update("delete from marketplace_listing_items");
		jdbcTemplate.update("delete from marketplace_listings");
		jdbcTemplate.update("delete from study_plan_tasks");
		jdbcTemplate.update("delete from study_plans");
		jdbcTemplate.update("delete from test_results");
		jdbcTemplate.update("delete from student_answers");
		jdbcTemplate.update("delete from test_attempts");
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

		when(fileStorageService.createDownloadUrl(any()))
				.thenReturn("http://localhost:9000/mock-exams-test/signed-url");
	}

	@Test
	void studentCannotStartPaidTestWithoutAccess() throws Exception {
		String accessToken = registerStudent(uniqueEmail("paid-attempt-denied"));
		TeacherFixture teacher = approvedTeacher("paid-attempt-denied-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Paid Reading", false);
		createActiveListing(teacher.profileId(), testId, "Paid Reading Listing", "single_test", new BigDecimal("15000.00"));

		mockMvc.perform(post("/api/v1/student/tests/{testId}/attempts", testId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.error.code").value("PAYMENT_REQUIRED"));
	}

	@Test
	void internalPaidPurchaseGrantGivesAccessAndIsIdempotent() throws Exception {
		String studentEmail = uniqueEmail("paid-grant");
		String accessToken = registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		TeacherFixture teacher = approvedTeacher("paid-grant-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Paid Grant Reading", false);
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Paid Grant Listing",
				"single_test",
				new BigDecimal("25000.00"));

		Long firstPurchaseId = accessControlService.grantInternalAccess(
				studentId,
				listingId,
				new AccessGrantRequest("payment", "payment-123", "{\"provider\":\"mock\"}", null))
				.id();
		Long duplicatePurchaseId = accessControlService.grantInternalAccess(
				studentId,
				listingId,
				new AccessGrantRequest("payment", "payment-123", "{\"provider\":\"mock\"}", null))
				.id();

		assertThat(duplicatePurchaseId).isEqualTo(firstPurchaseId);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from purchases where student_user_id = ? and listing_id = ?",
				Integer.class,
				studentId,
				listingId)).isOne();

		mockMvc.perform(post("/api/v1/student/tests/{testId}/attempts", testId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.testId").value(testId));
	}

	@Test
	void freeListingGrantIsIdempotentAndAppearsInPurchaseHistory() throws Exception {
		String studentEmail = uniqueEmail("free-grant");
		String accessToken = registerStudent(studentEmail);
		TeacherFixture teacher = approvedTeacher("free-grant-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Free Reading", true);
		Long listingId = createActiveListing(teacher.profileId(), testId, "Free Reading Listing", "single_test", BigDecimal.ZERO);

		Long firstPurchaseId = grantFreeAccess(accessToken, listingId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("granted"))
				.andExpect(jsonPath("$.data.sourceType").value("free_listing"))
				.andReturnId();
		Long duplicatePurchaseId = grantFreeAccess(accessToken, listingId)
				.andExpect(status().isOk())
				.andReturnId();

		assertThat(duplicatePurchaseId).isEqualTo(firstPurchaseId);
		mockMvc.perform(get("/api/v1/student/purchases")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(firstPurchaseId))
				.andExpect(jsonPath("$.data[0].sourceMetadata").doesNotExist());
	}

	@Test
	void freeListingGrantIsDeniedWhenTeacherIsNoLongerApproved() throws Exception {
		String accessToken = registerStudent(uniqueEmail("free-unapproved"));
		TeacherFixture teacher = approvedTeacher("free-grant-unapproved-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Unapproved Free Reading", true);
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Unapproved Free Listing",
				"single_test",
				BigDecimal.ZERO);
		jdbcTemplate.update("""
				update teacher_profiles
				set status = 'pending_approval', approved_by_user_id = null, approved_at = null
				where id = ?
				""", teacher.profileId());

		grantFreeAccess(accessToken, listingId)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("LISTING_NOT_ACCESSIBLE"));
	}

	@Test
	void duplicatePurchaseGrantIsIdempotentAcrossSourceReferences() throws Exception {
		String studentEmail = uniqueEmail("duplicate-purchase");
		registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		TeacherFixture teacher = approvedTeacher("duplicate-purchase-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Duplicate Paid", false);
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Duplicate Paid Listing",
				"single_test",
				new BigDecimal("25000.00"));

		Long firstPurchaseId = accessControlService.grantInternalAccess(
				studentId,
				listingId,
				new AccessGrantRequest("payment", "payment-a", null, null))
				.id();
		Long duplicatePurchaseId = accessControlService.grantInternalAccess(
				studentId,
				listingId,
				new AccessGrantRequest("manual_admin", "admin-note", null, null))
				.id();

		assertThat(duplicatePurchaseId).isEqualTo(firstPurchaseId);
		assertThat(jdbcTemplate.queryForObject("select count(*) from purchases", Integer.class)).isOne();
	}

	@Test
	void expiredSubscriptionDeniesAccess() throws Exception {
		String studentEmail = uniqueEmail("expired-subscription");
		String accessToken = registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		TeacherFixture teacher = approvedTeacher("expired-subscription-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Subscription Reading", false);
		Long listingId = createActiveListing(
				teacher.profileId(),
				null,
				"Subscription Listing",
				"subscription_plan",
				new BigDecimal("50000.00"));
		createListingItem(listingId, testId, null, "Subscription test", 1);
		createSubscription(studentId, listingId, "active", now().minusDays(10), now().minusDays(1));

		mockMvc.perform(post("/api/v1/student/tests/{testId}/attempts", testId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("ACCESS_EXPIRED"));
	}

	@Test
	void studentCannotReadAnotherStudentsPurchaseHistory() throws Exception {
		String ownerEmail = uniqueEmail("purchase-owner");
		String ownerToken = registerStudent(ownerEmail);
		Long ownerId = userIdForEmail(ownerEmail);
		String otherToken = registerStudent(uniqueEmail("purchase-other"));
		TeacherFixture teacher = approvedTeacher("purchase-owner-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Owned Purchase", false);
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Owned Listing",
				"single_test",
				new BigDecimal("25000.00"));
		Long purchaseId = accessControlService.grantInternalAccess(
				ownerId,
				listingId,
				new AccessGrantRequest("payment", "payment-owner", null, null))
				.id();

		mockMvc.perform(get("/api/v1/student/purchases/{purchaseId}", purchaseId)
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(purchaseId));
		mockMvc.perform(get("/api/v1/student/purchases/{purchaseId}", purchaseId)
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("PURCHASE_NOT_FOUND"));
	}

	@Test
	void paidListingItemsAndCourseMaterialRemainInaccessibleUntilGrant() throws Exception {
		String studentEmail = uniqueEmail("paid-items");
		String accessToken = registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		TeacherFixture teacher = approvedTeacher("paid-items-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Pack Test", false);
		Long fileId = createCourseMaterialFile(teacher.userId(), "paid-pack.pdf");
		Long listingId = createActiveListing(
				teacher.profileId(),
				null,
				"Paid Pack",
				"writing_course",
				new BigDecimal("30000.00"));
		createListingItem(listingId, testId, null, "Paid test", 1);
		createListingItem(listingId, null, fileId, "Paid PDF", 2);

		mockMvc.perform(get("/api/v1/student/marketplace/listings/{listingId}/items", listingId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.error.code").value("PAYMENT_REQUIRED"));
		mockMvc.perform(get("/api/v1/files/{fileId}/download-url", fileId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.error.code").value("PAYMENT_REQUIRED"));

		accessControlService.grantInternalAccess(
				studentId,
				listingId,
				new AccessGrantRequest("payment", "payment-items", null, null));

		mockMvc.perform(get("/api/v1/student/marketplace/listings/{listingId}/items", listingId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[1].fileId").value(fileId));
		mockMvc.perform(get("/api/v1/files/{fileId}/download-url", fileId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.url", startsWith("http://localhost:9000/")));
	}

	@Test
	void purchasedListingItemsAreDeniedWhenItemTestBecomesUnpublished() throws Exception {
		String studentEmail = uniqueEmail("unpublished-item");
		String accessToken = registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		TeacherFixture teacher = approvedTeacher("unpublished-item-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Soon Draft Pack Test", false);
		Long listingId = createActiveListing(
				teacher.profileId(),
				null,
				"Soon Draft Pack",
				"test_pack",
				new BigDecimal("30000.00"));
		createListingItem(listingId, testId, null, "Paid test", 1);
		accessControlService.grantInternalAccess(
				studentId,
				listingId,
				new AccessGrantRequest("payment", "payment-unpublished-item", null, null));
		jdbcTemplate.update("update tests set visibility = 'draft', published_at = null where id = ?", testId);

		mockMvc.perform(get("/api/v1/student/marketplace/listings/{listingId}/items", listingId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("LISTING_NOT_ACCESSIBLE"));
		mockMvc.perform(post("/api/v1/student/tests/{testId}/attempts", testId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("TEST_NOT_FOUND"));
	}

	@Test
	void inactiveListingDeniesAlreadyPurchasedContent() throws Exception {
		String studentEmail = uniqueEmail("inactive-purchase");
		String accessToken = registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		TeacherFixture teacher = approvedTeacher("inactive-purchase-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Inactive Paid", false);
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Inactive Paid Listing",
				"single_test",
				new BigDecimal("20000.00"));
		accessControlService.grantInternalAccess(
				studentId,
				listingId,
				new AccessGrantRequest("payment", "payment-inactive", null, null));
		jdbcTemplate.update("update marketplace_listings set is_active = false where id = ?", listingId);

		mockMvc.perform(post("/api/v1/student/tests/{testId}/attempts", testId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("LISTING_NOT_ACCESSIBLE"));
	}

	private ResultWithId grantFreeAccess(String accessToken, Long listingId) throws Exception {
		return new ResultWithId(mockMvc.perform(post(
				"/api/v1/student/marketplace/listings/{listingId}/free-access",
				listingId)
				.header("Authorization", "Bearer " + accessToken)));
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

	private String accessTokenFrom(String response) throws Exception {
		JsonNode root = objectMapper.readTree(response);
		return root.at("/data/accessToken").asText();
	}

	private TeacherFixture approvedTeacher(String prefix) {
		Long userId = createUserWithRole(prefix, "teacher");
		Long profileId = createTeacherProfile(userId);
		return new TeacherFixture(userId, profileId);
	}

	private Long createUserWithRole(String prefix, String roleCode) {
		Long userId = insertAndReturnId("""
				insert into users (email, password_hash, full_name, status, preferred_language)
				values (?, ?, ?, 'active', 'en')
				""",
				prefix + "-" + UUID.randomUUID() + "@example.test",
				passwordHasher.hash(PASSWORD),
				"Fixture " + prefix);
		Long roleId = jdbcTemplate.queryForObject("select id from roles where code = ?", Long.class, roleCode);
		jdbcTemplate.update("insert into user_roles (user_id, role_id) values (?, ?)", userId, roleId);
		return userId;
	}

	private Long createTeacherProfile(Long userId) {
		return insertAndReturnId("""
				insert into teacher_profiles (
				  user_id, display_name, headline, bio, status, approved_by_user_id, approved_at
				)
				values (?, ?, 'IELTS teacher', 'Experienced teacher.', 'approved', ?, ?)
				""",
				userId,
				"Teacher " + userId,
				userId,
				now());
	}

	private Long createPublishedTest(Long userId, Long teacherProfileId, String title, boolean free) {
		BigDecimal price = free ? BigDecimal.ZERO : new BigDecimal("15000.00");
		return insertAndReturnId("""
				insert into tests (
				  teacher_profile_id, title, description, type, difficulty, estimated_band_min,
				  estimated_band_max, duration_minutes, price_amount, currency, is_free, language,
				  visibility, published_at, created_by_user_id
				)
				values (?, ?, 'Purchase access fixture.', 'reading', 'intermediate', 5.5, 7.0, 60, ?, 'UZS', ?, 'en', 'published', ?, ?)
				""",
				teacherProfileId,
				title,
				price,
				free,
				now(),
				userId);
	}

	private Long createActiveListing(
			Long teacherProfileId,
			Long testId,
			String title,
			String listingType,
			BigDecimal priceAmount
	) {
		return insertAndReturnId("""
				insert into marketplace_listings (
				  teacher_profile_id, test_id, title, description, listing_type, price_amount, currency, is_active, language
				)
				values (?, ?, ?, 'Purchase listing fixture.', ?, ?, 'UZS', true, 'en')
				""",
				teacherProfileId,
				testId,
				title,
				listingType,
				priceAmount);
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

	private Long createCourseMaterialFile(Long ownerUserId, String originalName) {
		return insertAndReturnId("""
				insert into files (owner_user_id, purpose, bucket, object_key, original_name, content_type, size_bytes)
				values (?, 'course_material', 'test-bucket', ?, ?, 'application/pdf', 1024)
				""",
				ownerUserId,
				"course/" + UUID.randomUUID(),
				originalName);
	}

	private Long createSubscription(
			Long studentId,
			Long listingId,
			String status,
			OffsetDateTime startsAt,
			OffsetDateTime endsAt
	) {
		return insertAndReturnId("""
				insert into subscriptions (student_user_id, listing_id, status, starts_at, ends_at)
				values (?, ?, ?, ?, ?)
				""",
				studentId,
				listingId,
				status,
				startsAt,
				endsAt);
	}

	private Long userIdForEmail(String email) {
		return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
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

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private record TeacherFixture(Long userId, Long profileId) {
	}

	private final class ResultWithId {

		private final org.springframework.test.web.servlet.ResultActions delegate;

		private ResultWithId(org.springframework.test.web.servlet.ResultActions delegate) {
			this.delegate = delegate;
		}

		private ResultWithId andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
			delegate.andExpect(matcher);
			return this;
		}

		private Long andReturnId() throws Exception {
			String response = delegate.andReturn().getResponse().getContentAsString();
			return objectMapper.readTree(response).at("/data/id").asLong();
		}
	}
}
