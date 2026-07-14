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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
class PromoRedemptionControllerTest {

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
		cleanPromoFixtures();
		when(fileStorageService.createDownloadUrl(any()))
				.thenReturn("http://localhost:9000/mock-exams-test/signed-url");
	}

	@AfterEach
	void tearDown() {
		cleanPromoFixtures();
	}

	private void cleanPromoFixtures() {
		jdbcTemplate.update("delete from teacher_transactions");
		jdbcTemplate.update("delete from promo_code_redemptions");
		jdbcTemplate.update("delete from withdrawal_requests");
		jdbcTemplate.update("delete from teacher_wallets");
		jdbcTemplate.update("delete from subscriptions");
		jdbcTemplate.update("delete from purchases");
		jdbcTemplate.update("delete from payment_disputes");
		jdbcTemplate.update("delete from payments");
		jdbcTemplate.update("delete from promo_codes");
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
		jdbcTemplate.update("""
				update platform_settings
				set setting_value = ?
				where setting_key = 'marketplace.default_commission_percent'
				""", "{\"value\": 25.00}");
	}

	@Test
	void quoteShowsAmountsForValidCodeAndReasonForInvalidCode() throws Exception {
		String studentToken = registerStudent(uniqueEmail("promo-quote"));
		Fixture fixture = teacherWithActiveListing("promo-quote-teacher", new BigDecimal("25000.00"));
		createTeacherCode(fixture, "QUOTE20", "\"discountPercent\": 20.00");

		mockMvc.perform(get("/api/v1/student/payments/promo-quote")
						.header("Authorization", "Bearer " + studentToken)
						.param("listingId", fixture.listingId().toString())
						.param("promoCode", "quote20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.valid").value(true))
				.andExpect(jsonPath("$.data.code").value("QUOTE20"))
				.andExpect(jsonPath("$.data.reasonCode").doesNotExist())
				.andExpect(jsonPath("$.data.originalAmount").value(25000.00))
				.andExpect(jsonPath("$.data.discountAmount").value(5000.00))
				.andExpect(jsonPath("$.data.finalAmount").value(20000.00))
				.andExpect(jsonPath("$.data.currency").value("UZS"));

		mockMvc.perform(get("/api/v1/student/payments/promo-quote")
						.header("Authorization", "Bearer " + studentToken)
						.param("listingId", fixture.listingId().toString())
						.param("promoCode", "UNKNOWN99"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.valid").value(false))
				.andExpect(jsonPath("$.data.reasonCode").value("PROMO_CODE_INVALID"))
				.andExpect(jsonPath("$.data.originalAmount").value(25000.00))
				.andExpect(jsonPath("$.data.finalAmount").value(25000.00));

		assertThat(jdbcTemplate.queryForObject("select used_count from promo_codes", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from promo_code_redemptions", Integer.class)).isZero();
	}

	@Test
	void percentPromoDiscountsPaymentRecordsOneRedemptionAndPaysDiscountedEarning() throws Exception {
		String studentEmail = uniqueEmail("promo-percent");
		String studentToken = registerStudent(studentEmail);
		Fixture fixture = teacherWithActiveListing("promo-percent-teacher", new BigDecimal("25000.00"));
		long promoCodeId = createTeacherCode(fixture, "PERCENT20", "\"discountPercent\": 20.00");

		JsonNode payment = createMarketplacePayment(studentToken, fixture.listingId(), "percent20")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("pending"))
				.andExpect(jsonPath("$.data.amount").value(20000.00))
				.andExpect(jsonPath("$.data.promoCode").value("PERCENT20"))
				.andExpect(jsonPath("$.data.promoOriginalAmount").value(25000.00))
				.andExpect(jsonPath("$.data.promoDiscountAmount").value(5000.00))
				.andReturnData();

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from promo_code_redemptions where promo_code_id = ? and payment_id = ?",
				Integer.class, promoCodeId, payment.get("id").asLong())).isOne();
		assertThat(jdbcTemplate.queryForObject(
				"select discount_amount from promo_code_redemptions where payment_id = ?",
				BigDecimal.class, payment.get("id").asLong())).isEqualByComparingTo("5000.00");
		assertThat(jdbcTemplate.queryForObject(
				"select used_count from promo_codes where id = ?", Integer.class, promoCodeId)).isOne();

		String webhook = webhook(payment.get("providerPaymentId").asText(), "paid", "20000.00", "UZS");
		postWebhook(webhook, signature(webhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("paid"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from purchases", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject("select amount from purchases", BigDecimal.class))
				.isEqualByComparingTo("20000.00");
		assertThat(jdbcTemplate.queryForObject("select gross_amount from teacher_transactions", BigDecimal.class))
				.isEqualByComparingTo("20000.00");
		assertThat(jdbcTemplate.queryForObject(
				"select platform_commission_amount from teacher_transactions", BigDecimal.class))
				.isEqualByComparingTo("5000.00");
		assertThat(jdbcTemplate.queryForObject(
				"select balance_amount from teacher_wallets where teacher_profile_id = ?",
				BigDecimal.class, fixture.profileId())).isEqualByComparingTo("15000.00");
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from promo_code_redemptions", Integer.class)).isOne();
	}

	@Test
	void fixedDiscountCannotCreateNegativePayableAndGrantsZeroPaymentImmediately() throws Exception {
		String studentEmail = uniqueEmail("promo-zero");
		String studentToken = registerStudent(studentEmail);
		Fixture fixture = teacherWithActiveListing("promo-zero-teacher", new BigDecimal("25000.00"));
		createTeacherCode(fixture, "FULLOFF", "\"discountAmount\": 30000.00, \"currency\": \"UZS\"");

		JsonNode payment = createMarketplacePayment(studentToken, fixture.listingId(), "fulloff")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("paid"))
				.andExpect(jsonPath("$.data.amount").value(0.00))
				.andExpect(jsonPath("$.data.promoDiscountAmount").value(25000.00))
				.andExpect(jsonPath("$.data.promoOriginalAmount").value(25000.00))
				.andExpect(jsonPath("$.data.providerPaymentId").doesNotExist())
				.andExpect(jsonPath("$.data.providerCheckoutUrl").doesNotExist())
				.andExpect(jsonPath("$.data.confirmationDispatchedAt").isNotEmpty())
				.andReturnData();

		assertThat(jdbcTemplate.queryForObject("select count(*) from purchases where payment_id = ?",
				Integer.class, payment.get("id").asLong())).isOne();
		assertThat(jdbcTemplate.queryForObject("select amount from purchases", BigDecimal.class))
				.isEqualByComparingTo("0.00");
		assertThat(jdbcTemplate.queryForObject("select count(*) from teacher_transactions", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject(
				"select discount_amount from promo_code_redemptions where payment_id = ?",
				BigDecimal.class, payment.get("id").asLong())).isEqualByComparingTo("25000.00");

		mockMvc.perform(get("/api/v1/student/marketplace/listings/" + fixture.listingId() + "/access")
						.header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessible").value(true));
	}

	@Test
	void expiredCodeIsRejected() throws Exception {
		String studentToken = registerStudent(uniqueEmail("promo-expired"));
		Fixture fixture = teacherWithActiveListing("promo-expired-teacher", new BigDecimal("25000.00"));
		createTeacherCode(
				fixture,
				"EXPIRED10",
				"\"discountPercent\": 10.00, \"expiresAt\": \"2026-01-01T00:00:00Z\"");

		createMarketplacePayment(studentToken, fixture.listingId(), "EXPIRED10")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PROMO_CODE_EXPIRED"));

		mockMvc.perform(get("/api/v1/student/payments/promo-quote")
						.header("Authorization", "Bearer " + studentToken)
						.param("listingId", fixture.listingId().toString())
						.param("promoCode", "EXPIRED10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.valid").value(false))
				.andExpect(jsonPath("$.data.reasonCode").value("PROMO_CODE_EXPIRED"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from payments", Integer.class)).isZero();
	}

	@Test
	void inactiveAndNotYetStartedCodesAreRejected() throws Exception {
		String studentToken = registerStudent(uniqueEmail("promo-inactive"));
		Fixture fixture = teacherWithActiveListing("promo-inactive-teacher", new BigDecimal("25000.00"));
		long inactiveId = createTeacherCode(fixture, "DISABLED10", "\"discountPercent\": 10.00");
		jdbcTemplate.update("update promo_codes set is_active = false where id = ?", inactiveId);
		createTeacherCode(
				fixture,
				"FUTURE10",
				"\"discountPercent\": 10.00, \"startsAt\": \"2027-01-01T00:00:00Z\"");

		createMarketplacePayment(studentToken, fixture.listingId(), "DISABLED10")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PROMO_CODE_INACTIVE"));
		createMarketplacePayment(studentToken, fixture.listingId(), "FUTURE10")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PROMO_CODE_INACTIVE"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from payments", Integer.class)).isZero();
	}

	@Test
	void usageLimitIsEnforced() throws Exception {
		String studentToken = registerStudent(uniqueEmail("promo-limit"));
		Fixture fixture = teacherWithActiveListing("promo-limit-teacher", new BigDecimal("25000.00"));
		long promoCodeId = createTeacherCode(fixture, "ONCE10", "\"discountPercent\": 10.00, \"usageLimit\": 1");

		createMarketplacePayment(studentToken, fixture.listingId(), "ONCE10")
				.andExpect(status().isCreated());
		createMarketplacePayment(studentToken, fixture.listingId(), "ONCE10")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PROMO_CODE_EXHAUSTED"));

		assertThat(jdbcTemplate.queryForObject(
				"select used_count from promo_codes where id = ?", Integer.class, promoCodeId)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from payments", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from promo_code_redemptions", Integer.class)).isOne();
	}

	@Test
	void failedWebhookReleasesUsageExactlyOnceAndAllowsRetry() throws Exception {
		String studentToken = registerStudent(uniqueEmail("promo-fail-release"));
		Fixture fixture = teacherWithActiveListing("promo-fail-release-teacher", new BigDecimal("25000.00"));
		long promoCodeId = createTeacherCode(fixture, "FAILREL", "\"discountPercent\": 10.00, \"usageLimit\": 1");

		JsonNode payment = createMarketplacePayment(studentToken, fixture.listingId(), "FAILREL")
				.andExpect(status().isCreated())
				.andReturnData();

		assertThat(jdbcTemplate.queryForObject(
				"select used_count from promo_codes where id = ?", Integer.class, promoCodeId)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from promo_code_redemptions", Integer.class)).isOne();

		String webhook = webhook(payment.get("providerPaymentId").asText(), "failed", "22500.00", "UZS");
		postWebhook(webhook, signature(webhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("failed"));

		assertThat(jdbcTemplate.queryForObject(
				"select used_count from promo_codes where id = ?", Integer.class, promoCodeId)).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from promo_code_redemptions", Integer.class)).isZero();

		// Duplicate failed webhook must not double-release (usage stays at zero, never negative).
		String duplicateWebhook = webhook(payment.get("providerPaymentId").asText(), "failed", "22500.00", "UZS");
		postWebhook(duplicateWebhook, signature(duplicateWebhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("failed"));

		assertThat(jdbcTemplate.queryForObject(
				"select used_count from promo_codes where id = ?", Integer.class, promoCodeId)).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from promo_code_redemptions", Integer.class)).isZero();

		// Student can redeem again after the failed payment released usage.
		createMarketplacePayment(studentToken, fixture.listingId(), "FAILREL")
				.andExpect(status().isCreated());
		assertThat(jdbcTemplate.queryForObject(
				"select used_count from promo_codes where id = ?", Integer.class, promoCodeId)).isOne();
	}

	@Test
	void cancelledWebhookReleasesUsageButPaidWebhookDoesNot() throws Exception {
		String studentToken = registerStudent(uniqueEmail("promo-cancel-rel"));
		Fixture fixture = teacherWithActiveListing("promo-cancel-rel-tchr", new BigDecimal("25000.00"));
		long promoCodeId = createTeacherCode(fixture, "CANCELREL", "\"discountPercent\": 10.00, \"usageLimit\": 2");

		JsonNode cancelledPayment = createMarketplacePayment(studentToken, fixture.listingId(), "CANCELREL")
				.andExpect(status().isCreated())
				.andReturnData();
		String cancelWebhook = webhook(cancelledPayment.get("providerPaymentId").asText(), "cancelled", "22500.00", "UZS");
		postWebhook(cancelWebhook, signature(cancelWebhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("cancelled"));

		assertThat(jdbcTemplate.queryForObject(
				"select used_count from promo_codes where id = ?", Integer.class, promoCodeId)).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from promo_code_redemptions", Integer.class)).isZero();

		JsonNode paidPayment = createMarketplacePayment(studentToken, fixture.listingId(), "CANCELREL")
				.andExpect(status().isCreated())
				.andReturnData();
		String paidWebhook = webhook(paidPayment.get("providerPaymentId").asText(), "paid", "22500.00", "UZS");
		postWebhook(paidWebhook, signature(paidWebhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("paid"));

		// Paid payments must NOT release usage.
		assertThat(jdbcTemplate.queryForObject(
				"select used_count from promo_codes where id = ?", Integer.class, promoCodeId)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from promo_code_redemptions", Integer.class)).isOne();
	}

	@Test
	void teacherCodeCannotDiscountAnotherTeachersListingWhilePlatformCodeCan() throws Exception {
		String studentToken = registerStudent(uniqueEmail("promo-owner"));
		Fixture owner = teacherWithActiveListing("promo-owner-teacher", new BigDecimal("25000.00"));
		Fixture other = teacherWithActiveListing("promo-other-teacher", new BigDecimal("25000.00"));
		createTeacherCode(owner, "OWNER10", "\"discountPercent\": 10.00");
		String adminToken = tokenForUser(createUserWithRole("promo-owner-admin", "admin"));
		mockMvc.perform(post("/api/v1/admin/promo-codes")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "PLATFORM10",
								  "discountPercent": 10.00
								}
								"""))
				.andExpect(status().isCreated());

		createMarketplacePayment(studentToken, other.listingId(), "OWNER10")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PROMO_CODE_WRONG_OWNER"));

		createMarketplacePayment(studentToken, other.listingId(), "PLATFORM10")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.amount").value(22500.00))
				.andExpect(jsonPath("$.data.promoCode").value("PLATFORM10"));
	}

	@Test
	void currencyMismatchedCodeIsRejected() throws Exception {
		String studentToken = registerStudent(uniqueEmail("promo-currency"));
		Fixture fixture = teacherWithActiveListing("promo-currency-teacher", new BigDecimal("25000.00"));
		createTeacherCode(fixture, "USD5", "\"discountAmount\": 5.00, \"currency\": \"USD\"");

		createMarketplacePayment(studentToken, fixture.listingId(), "USD5")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PROMO_CODE_CURRENCY_MISMATCH"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from payments", Integer.class)).isZero();
	}

	@Test
	void inactiveListingCannotRedeemPromo() throws Exception {
		String studentToken = registerStudent(uniqueEmail("promo-listing"));
		Fixture fixture = teacherWithActiveListing("promo-listing-teacher", new BigDecimal("25000.00"));
		createTeacherCode(fixture, "LISTING10", "\"discountPercent\": 10.00");
		jdbcTemplate.update("update marketplace_listings set is_active = false where id = ?", fixture.listingId());

		createMarketplacePayment(studentToken, fixture.listingId(), "LISTING10")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("LISTING_NOT_ACCESSIBLE"));

		mockMvc.perform(get("/api/v1/student/payments/promo-quote")
						.header("Authorization", "Bearer " + studentToken)
						.param("listingId", fixture.listingId().toString())
						.param("promoCode", "LISTING10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.valid").value(false))
				.andExpect(jsonPath("$.data.reasonCode").value("LISTING_NOT_ACCESSIBLE"));

		assertThat(jdbcTemplate.queryForObject("select count(*) from payments", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("select count(*) from promo_code_redemptions", Integer.class)).isZero();
	}

	@Test
	void concurrentRedemptionsCannotExceedUsageLimit() throws Exception {
		String studentToken = registerStudent(uniqueEmail("promo-race"));
		Fixture fixture = teacherWithActiveListing("promo-race-teacher", new BigDecimal("25000.00"));
		long promoCodeId = createTeacherCode(fixture, "RACE10", "\"discountPercent\": 10.00, \"usageLimit\": 1");

		int workers = 2;
		ExecutorService executorService = Executors.newFixedThreadPool(workers);
		CountDownLatch ready = new CountDownLatch(workers);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger created = new AtomicInteger();
		AtomicInteger exhausted = new AtomicInteger();
		List<Throwable> unexpected = new ArrayList<>();
		try {
			for (int i = 0; i < workers; i++) {
				executorService.submit(() -> {
					ready.countDown();
					try {
						start.await(5, TimeUnit.SECONDS);
						MvcResult result = createMarketplacePayment(studentToken, fixture.listingId(), "RACE10")
								.andReturn();
						int status = result.getResponse().getStatus();
						if (status == 201) {
							created.incrementAndGet();
						}
						else if (status == 409 && "PROMO_CODE_EXHAUSTED".equals(objectMapper
								.readTree(result.getResponse().getContentAsString())
								.at("/error/code").asText())) {
							exhausted.incrementAndGet();
						}
						else {
							unexpected.add(new IllegalStateException("Unexpected status " + status));
						}
					}
					catch (Throwable ex) {
						unexpected.add(ex);
					}
				});
			}
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			executorService.shutdown();
			assertThat(executorService.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
		}
		finally {
			executorService.shutdownNow();
		}

		assertThat(unexpected).isEmpty();
		assertThat(created.get()).isOne();
		assertThat(exhausted.get()).isOne();
		assertThat(jdbcTemplate.queryForObject(
				"select used_count from promo_codes where id = ?", Integer.class, promoCodeId)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from promo_code_redemptions", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from payments", Integer.class)).isOne();
	}

	private ResultWithData createMarketplacePayment(String accessToken, Long listingId, String promoCode)
			throws Exception {
		return new ResultWithData(mockMvc.perform(post("/api/v1/student/payments")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "targetType": "marketplace_listing",
						  "listingId": %d,
						  "promoCode": "%s"
						}
						""".formatted(listingId, promoCode))));
	}

	private long createTeacherCode(Fixture fixture, String code, String discountJsonFields) throws Exception {
		String response = mockMvc.perform(post("/api/v1/teacher/promo-codes")
						.header("Authorization", "Bearer " + fixture.teacherToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "%s",
								  %s
								}
								""".formatted(code, discountJsonFields)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).at("/data/id").asLong();
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

	private Fixture teacherWithActiveListing(String prefix, BigDecimal priceAmount) throws Exception {
		Long userId = createUserWithRole(prefix, "teacher");
		Long profileId = createTeacherProfile(userId);
		Long testId = createPublishedTest(userId, profileId, "Promo Reading " + prefix);
		Long listingId = createActiveListing(profileId, testId, "Promo Listing " + prefix, priceAmount);
		return new Fixture(userId, profileId, listingId, tokenForUser(userId));
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
				values (?, ?, 'Promo fixture.', 'reading', 'intermediate', 5.5, 7.0, 60, 15000.00, 'UZS', false, 'en', 'published', ?, ?)
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
				values (?, ?, ?, 'Promo listing fixture.', 'single_test', ?, 'UZS', true, 'en')
				""",
				teacherProfileId,
				testId,
				title,
				priceAmount);
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

	private record Fixture(Long userId, Long profileId, Long listingId, String teacherToken) {
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

		private MvcResult andReturn() {
			return delegate.andReturn();
		}

		private JsonNode andReturnData() throws Exception {
			MvcResult result = delegate.andReturn();
			return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
		}
	}
}
