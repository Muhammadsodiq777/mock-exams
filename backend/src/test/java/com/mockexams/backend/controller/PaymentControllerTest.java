package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MvcResult;

import com.mockexams.backend.auth.service.PasswordHasher;
import com.mockexams.backend.service.FileStorageService;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

	private static final String PASSWORD = "correct-password-123";
	private static final String WEBHOOK_SECRET = "dev-only-payment-webhook-secret";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordHasher passwordHasher;

	@MockBean
	private FileStorageService fileStorageService;

	@BeforeEach
	void setUp() {
		cleanPaymentFixtures();
		updateCommissionPercent("25.00");
	}

	@AfterEach
	void tearDown() {
		cleanPaymentFixtures();
		updateCommissionPercent("25.00");
	}

	private void cleanPaymentFixtures() {
		jdbcTemplate.update("delete from teacher_transactions");
		jdbcTemplate.update("delete from promo_code_redemptions");
		jdbcTemplate.update("delete from withdrawal_requests");
		jdbcTemplate.update("delete from teacher_wallets");
		jdbcTemplate.update("delete from subscriptions");
		jdbcTemplate.update("delete from purchases");
		jdbcTemplate.update("delete from payment_disputes");
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
	void pendingPaymentIsCreatedWithProviderId() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-create"));
		TeacherFixture teacher = approvedTeacher("payment-create-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Paid Payment Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Paid Payment Listing",
				new BigDecimal("25000.00"));

		JsonNode payment = createMarketplacePayment(accessToken, listingId)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("pending"))
				.andExpect(jsonPath("$.data.provider").value("mock"))
				.andExpect(jsonPath("$.data.targetType").value("marketplace_listing"))
				.andExpect(jsonPath("$.data.targetListingId").value(listingId))
				.andExpect(jsonPath("$.data.amount").value(25000.00))
				.andExpect(jsonPath("$.data.currency").value("UZS"))
				.andReturnData();

		assertThat(payment.get("providerPaymentId").asText()).startsWith("mock-");
		assertThat(payment.get("providerCheckoutUrl").asText()).startsWith("mock://payments/");
	}

	@Test
	void paidCallbackUpdatesStatusOnceAndGrantsMarketplaceAccessOnce() throws Exception {
		String studentEmail = uniqueEmail("payment-paid");
		String accessToken = registerStudent(studentEmail);
		Long studentId = userIdForEmail(studentEmail);
		TeacherFixture teacher = approvedTeacher("payment-paid-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Paid Callback Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Paid Callback Listing",
				new BigDecimal("25000.00"));
		JsonNode payment = createMarketplacePayment(accessToken, listingId).andReturnData();

		String webhook = webhook(payment.get("providerPaymentId").asText(), "paid", "25000.00", "UZS");
		postWebhook(webhook, signature(webhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("paid"))
				.andExpect(jsonPath("$.data.confirmationDispatchedAt").isNotEmpty());
		postWebhook(webhook, signature(webhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("paid"));

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from purchases where student_user_id = ? and listing_id = ? and payment_id = ?",
				Integer.class,
				studentId,
				listingId,
				payment.get("id").asLong())).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from purchases", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from teacher_transactions", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject("""
				select gross_amount
				from teacher_transactions
				where purchase_id = (
				  select id from purchases where student_user_id = ? and listing_id = ?
				)
				""", BigDecimal.class, studentId, listingId)).isEqualByComparingTo("25000.00");
		assertThat(jdbcTemplate.queryForObject("""
				select platform_commission_amount
				from teacher_transactions
				where purchase_id = (
				  select id from purchases where student_user_id = ? and listing_id = ?
				)
				""", BigDecimal.class, studentId, listingId)).isEqualByComparingTo("6250.00");
		assertThat(jdbcTemplate.queryForObject(
				"select balance_amount from teacher_wallets where teacher_profile_id = ?",
				BigDecimal.class,
				teacher.profileId())).isEqualByComparingTo("18750.00");
	}

	@Test
	void failedCallbackDoesNotGrantAccess() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-failed"));
		TeacherFixture teacher = approvedTeacher("payment-failed-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Failed Callback Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Failed Callback Listing",
				new BigDecimal("25000.00"));
		JsonNode payment = createMarketplacePayment(accessToken, listingId).andReturnData();

		String webhook = webhook(payment.get("providerPaymentId").asText(), "failed", "25000.00", "UZS");
		postWebhook(webhook, signature(webhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("failed"))
				.andExpect(jsonPath("$.data.confirmationDispatchedAt").doesNotExist());

		assertThat(jdbcTemplate.queryForObject("select count(*) from purchases", Integer.class)).isZero();
	}

	@Test
	void invalidWebhookSignatureIsRejected() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-signature"));
		TeacherFixture teacher = approvedTeacher("payment-signature-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Signature Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Signature Listing",
				new BigDecimal("25000.00"));
		JsonNode payment = createMarketplacePayment(accessToken, listingId).andReturnData();

		String webhook = webhook(payment.get("providerPaymentId").asText(), "paid", "25000.00", "UZS");
		postWebhook(webhook, "bad-signature")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("PAYMENT_SIGNATURE_INVALID"));

		assertThat(jdbcTemplate.queryForObject("select status from payments where id = ?", String.class,
				payment.get("id").asLong())).isEqualTo("pending");
		assertThat(jdbcTemplate.queryForObject("select count(*) from purchases", Integer.class)).isZero();
	}

	@Test
	void paymentAmountMismatchIsFlaggedAndDoesNotGrantAccess() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-mismatch"));
		TeacherFixture teacher = approvedTeacher("payment-mismatch-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Mismatch Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Mismatch Listing",
				new BigDecimal("25000.00"));
		JsonNode payment = createMarketplacePayment(accessToken, listingId).andReturnData();

		String webhook = webhook(payment.get("providerPaymentId").asText(), "paid", "24000.00", "UZS");
		postWebhook(webhook, signature(webhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("failed"))
				.andExpect(jsonPath("$.data.failureCode").value("PAYMENT_AMOUNT_MISMATCH"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from purchases", Integer.class)).isZero();
	}

	@Test
	void studentCanReadOwnPaymentHistoryAndAdminCanReadSafePaymentList() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-history"));
		String adminToken = tokenForUser(createUserWithRole("payment-admin", "admin"));
		TeacherFixture teacher = approvedTeacher("payment-history-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "History Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"History Listing",
				new BigDecimal("25000.00"));
		Long paymentId = createMarketplacePayment(accessToken, listingId).andReturnData().get("id").asLong();

		mockMvc.perform(get("/api/v1/student/payments")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(paymentId));
		mockMvc.perform(get("/api/v1/admin/payments")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(paymentId));
	}

	@Test
	void adminPaymentListIsPagedAndFilterableByStatus() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-admin-page"));
		String adminToken = tokenForUser(createUserWithRole("payment-admin-page", "admin"));
		TeacherFixture teacher = approvedTeacher("payment-admin-page-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Admin Page Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Admin Page Listing",
				new BigDecimal("25000.00"));
		JsonNode firstPayment = createMarketplacePayment(accessToken, listingId).andReturnData();
		createMarketplacePayment(accessToken, listingId).andReturnData();
		String paidWebhook = webhook(firstPayment.get("providerPaymentId").asText(), "paid", "25000.00", "UZS");
		postWebhook(paidWebhook, signature(paidWebhook)).andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/admin/payments")
						.header("Authorization", "Bearer " + adminToken)
						.param("page", "0")
						.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.meta.page.number").value(0))
				.andExpect(jsonPath("$.meta.page.size").value(1))
				.andExpect(jsonPath("$.meta.page.totalElements").value(2))
				.andExpect(jsonPath("$.meta.page.totalPages").value(2))
				.andExpect(jsonPath("$.meta.sort[0].field").value("createdAt"))
				.andExpect(jsonPath("$.meta.sort[0].direction").value("desc"));

		mockMvc.perform(get("/api/v1/admin/payments")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "paid"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(firstPayment.get("id").asLong()))
				.andExpect(jsonPath("$.meta.page.totalElements").value(1))
				.andExpect(jsonPath("$.meta.filters.status").value("paid"));

		mockMvc.perform(get("/api/v1/admin/payments")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "unknown"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void cancelledCallbackIsTerminalAndCannotBecomePaid() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-cancelled"));
		TeacherFixture teacher = approvedTeacher("payment-cancelled-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Cancelled Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Cancelled Listing",
				new BigDecimal("25000.00"));
		JsonNode payment = createMarketplacePayment(accessToken, listingId).andReturnData();

		String cancelledWebhook = webhook(payment.get("providerPaymentId").asText(), "cancelled", "25000.00", "UZS");
		postWebhook(cancelledWebhook, signature(cancelledWebhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("cancelled"))
				.andExpect(jsonPath("$.data.confirmationDispatchedAt").doesNotExist());

		String paidWebhook = webhook(payment.get("providerPaymentId").asText(), "paid", "25000.00", "UZS");
		postWebhook(paidWebhook, signature(paidWebhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("cancelled"))
				.andExpect(jsonPath("$.data.confirmationDispatchedAt").doesNotExist());

		assertThat(jdbcTemplate.queryForObject("select count(*) from purchases", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from teacher_transactions", Integer.class)).isZero();
	}

	@Test
	void paidPaymentOnlyAcceptsRefundedMarkerAndKeepsSideEffectsIntact() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-refunded"));
		TeacherFixture teacher = approvedTeacher("payment-refunded-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Refunded Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Refunded Listing",
				new BigDecimal("25000.00"));
		JsonNode payment = createMarketplacePayment(accessToken, listingId).andReturnData();

		String paidWebhook = webhook(payment.get("providerPaymentId").asText(), "paid", "25000.00", "UZS");
		postWebhook(paidWebhook, signature(paidWebhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("paid"));

		String cancelledWebhook = webhook(payment.get("providerPaymentId").asText(), "cancelled", "25000.00", "UZS");
		postWebhook(cancelledWebhook, signature(cancelledWebhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("paid"));

		String pendingWebhook = webhook(payment.get("providerPaymentId").asText(), "pending", "25000.00", "UZS");
		postWebhook(pendingWebhook, signature(pendingWebhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("paid"));

		String refundedWebhook = webhook(payment.get("providerPaymentId").asText(), "refunded", "25000.00", "UZS");
		postWebhook(refundedWebhook, signature(refundedWebhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("refunded"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from purchases", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from teacher_transactions", Integer.class)).isOne();
	}

	@Test
	void paymentCurrencyMismatchIsFlaggedAndDoesNotGrantAccess() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-currency-mismatch"));
		TeacherFixture teacher = approvedTeacher("payment-currency-mismatch-teacher");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Currency Mismatch Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Currency Mismatch Listing",
				new BigDecimal("25000.00"));
		JsonNode payment = createMarketplacePayment(accessToken, listingId).andReturnData();

		String webhook = webhook(payment.get("providerPaymentId").asText(), "paid", "25000.00", "USD");
		postWebhook(webhook, signature(webhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("failed"))
				.andExpect(jsonPath("$.data.failureCode").value("PAYMENT_AMOUNT_MISMATCH"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from purchases", Integer.class)).isZero();
	}

	@Test
	void studentWithGrantedAccessCannotCreateAnotherPaymentForTheSameListing() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-already-owned"));
		TeacherFixture teacher = approvedTeacher("payment-already-owned-tchr");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Already Owned Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Already Owned Listing",
				new BigDecimal("25000.00"));
		JsonNode firstPayment = createMarketplacePayment(accessToken, listingId).andReturnData();
		String paidWebhook = webhook(firstPayment.get("providerPaymentId").asText(), "paid", "25000.00", "UZS");
		postWebhook(paidWebhook, signature(paidWebhook)).andExpect(status().isOk());

		createMarketplacePayment(accessToken, listingId)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("ALREADY_PURCHASED"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from payments", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from purchases", Integer.class)).isOne();
	}

	@Test
	void promoQuoteForAnOwnedListingReportsInvalidReasonInsteadOfCreatingAnything() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-quote-owned"));
		TeacherFixture teacher = approvedTeacher("payment-quote-owned-tchr");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Quote Owned Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Quote Owned Listing",
				new BigDecimal("25000.00"));
		JsonNode firstPayment = createMarketplacePayment(accessToken, listingId).andReturnData();
		String paidWebhook = webhook(firstPayment.get("providerPaymentId").asText(), "paid", "25000.00", "UZS");
		postWebhook(paidWebhook, signature(paidWebhook)).andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/student/payments/promo-quote")
						.header("Authorization", "Bearer " + accessToken)
						.param("listingId", listingId.toString())
						.param("promoCode", "ANYCODE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.valid").value(false))
				.andExpect(jsonPath("$.data.reasonCode").value("ALREADY_PURCHASED"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from payments", Integer.class)).isOne();
	}

	@Test
	void studentWithoutAccessCanStillCreatePaymentForTheSameListing() throws Exception {
		String ownerToken = registerStudent(uniqueEmail("payment-owner-only"));
		String otherToken = registerStudent(uniqueEmail("payment-not-owner"));
		TeacherFixture teacher = approvedTeacher("payment-not-owner-tchr");
		Long testId = createPublishedTest(teacher.userId(), teacher.profileId(), "Not Owner Reading");
		Long listingId = createActiveListing(
				teacher.profileId(),
				testId,
				"Not Owner Listing",
				new BigDecimal("25000.00"));
		JsonNode ownerPayment = createMarketplacePayment(ownerToken, listingId).andReturnData();
		String paidWebhook = webhook(ownerPayment.get("providerPaymentId").asText(), "paid", "25000.00", "UZS");
		postWebhook(paidWebhook, signature(paidWebhook)).andExpect(status().isOk());

		createMarketplacePayment(otherToken, listingId)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("pending"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from payments", Integer.class)).isEqualTo(2);
	}

	@Test
	void subscriptionTargetIsUnsupportedAndMissingLiveSpeakingBookingIsNotFound() throws Exception {
		String accessToken = registerStudent(uniqueEmail("payment-target"));

		mockMvc.perform(post("/api/v1/student/payments")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "targetType": "subscription",
								  "subscriptionId": 1
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PAYMENT_TARGET_UNSUPPORTED"));

		mockMvc.perform(post("/api/v1/student/payments")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "targetType": "live_speaking_booking",
								  "liveSpeakingBookingId": 1
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("SCHEDULE_BOOKING_NOT_FOUND"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from payments", Integer.class)).isZero();
	}

	private ResultWithData createMarketplacePayment(String accessToken, Long listingId) throws Exception {
		return new ResultWithData(mockMvc.perform(post("/api/v1/student/payments")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "targetType": "marketplace_listing",
						  "listingId": %d
						}
						""".formatted(listingId))));
	}

	private ResultWithData postWebhook(String body, String signature) throws Exception {
		return new ResultWithData(mockMvc.perform(post("/api/v1/provider/payments/webhook")
				.header("X-MockExams-Payment-Signature", signature)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)));
	}

	private String webhook(String providerPaymentId, String status, String amount, String currency) {
		return """
				{
				  "eventId": "%s",
				  "providerPaymentId": "%s",
				  "status": "%s",
				  "amount": %s,
				  "currency": "%s",
				  "occurredAt": "%s"
				}
				""".formatted(UUID.randomUUID(), providerPaymentId, status, amount, currency, now());
	}

	private String signature(String payload) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void updateCommissionPercent(String percent) {
		jdbcTemplate.update("""
				update platform_settings
				set setting_value = ?
				where setting_key = 'marketplace.default_commission_percent'
				""", "{\"value\": " + percent + "}");
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

	private String tokenForUser(Long userId) throws Exception {
		String email = jdbcTemplate.queryForObject("select email from users where id = ?", String.class, userId);
		String response = mockMvc.perform(post("/api/v1/auth/login")
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
				uniqueEmail(prefix),
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

	private Long createPublishedTest(Long userId, Long teacherProfileId, String title) {
		return insertAndReturnId("""
				insert into tests (
				  teacher_profile_id, title, description, type, difficulty, estimated_band_min,
				  estimated_band_max, duration_minutes, price_amount, currency, is_free, language,
				  visibility, published_at, created_by_user_id
				)
				values (?, ?, 'Payment fixture.', 'reading', 'intermediate', 5.5, 7.0, 60, 15000.00, 'UZS', false, 'en', 'published', ?, ?)
				""",
				teacherProfileId,
				title,
				now(),
				userId);
	}

	private Long createActiveListing(
			Long teacherProfileId,
			Long testId,
			String title,
			BigDecimal priceAmount
	) {
		return insertAndReturnId("""
				insert into marketplace_listings (
				  teacher_profile_id, test_id, title, description, listing_type, price_amount, currency, is_active, language
				)
				values (?, ?, ?, 'Payment listing fixture.', 'single_test', ?, 'UZS', true, 'en')
				""",
				teacherProfileId,
				testId,
				title,
				priceAmount);
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

	private final class ResultWithData {

		private final org.springframework.test.web.servlet.ResultActions delegate;

		private ResultWithData(org.springframework.test.web.servlet.ResultActions delegate) {
			this.delegate = delegate;
		}

		private ResultWithData andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
			delegate.andExpect(matcher);
			return this;
		}

		private JsonNode andReturnData() throws Exception {
			MvcResult result = delegate.andReturn();
			return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
		}
	}
}
