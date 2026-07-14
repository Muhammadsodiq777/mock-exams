package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.mockexams.backend.auth.service.PasswordHasher;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpeakingScheduleControllerTest {

	private static final String PASSWORD = "correct-password-123";
	private static final String WEBHOOK_SECRET = "dev-only-payment-webhook-secret";
	private static final String SCHEDULE_USER_FILTER = "select id from users where email like 'schedule-%@example.test'";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordHasher passwordHasher;

	@BeforeEach
	void setUp() {
		cleanScheduleFixtures();
	}

	@AfterEach
	void tearDown() {
		cleanScheduleFixtures();
	}

	@Test
	void approvedTeacherCanAnnounceAndStudentCanReserveWithoutDoubleBooking() throws Exception {
		TeacherFixture teacher = approvedTeacher("schedule-announced");
		String studentToken = registerStudent(uniqueEmail("schedule-student"));
		String otherStudentToken = registerStudent(uniqueEmail("schedule-other"));

		JsonNode slot = createSlot(teacher.accessToken())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("draft"))
				.andExpect(jsonPath("$.data.announced").value(false))
				.andReturnData();
		Long slotId = slot.get("id").asLong();

		mockMvc.perform(post("/api/v1/teacher/schedule/slots/{slotId}/announce", slotId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("announced"))
				.andExpect(jsonPath("$.data.announced").value(true));

		mockMvc.perform(get("/api/v1/student/schedule/slots")
						.header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(slotId));

			JsonNode booking = reserveSlot(studentToken, slotId)
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.data.status").value("reserved"))
					.andExpect(jsonPath("$.data.holdExpiresAt").isNotEmpty())
					.andReturnData();
			Long bookingId = booking.get("id").asLong();
			assertThat(booking.get("slotId").asLong()).isEqualTo(slotId);

			mockMvc.perform(get("/api/v1/student/schedule/bookings")
							.header("Authorization", "Bearer " + studentToken))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data[0].id").value(bookingId));
			mockMvc.perform(get("/api/v1/teacher/schedule/bookings")
							.header("Authorization", "Bearer " + teacher.accessToken()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data[0].id").value(bookingId));

			reserveSlot(otherStudentToken, slotId)
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.error.code").value("SCHEDULE_SLOT_UNAVAILABLE"));
	}

	@Test
	void liveSpeakingPaymentWebhookConfirmsBookingAndWalletEarningOnce() throws Exception {
		TeacherFixture teacher = approvedTeacher("schedule-paid");
		String studentToken = registerStudent(uniqueEmail("schedule-paid-student"));
		Long slotId = createSlot(teacher.accessToken()).andReturnData().get("id").asLong();
		mockMvc.perform(post("/api/v1/teacher/schedule/slots/{slotId}/announce", slotId)
						.header("Authorization", "Bearer " + teacher.accessToken()))
				.andExpect(status().isOk());
		Long bookingId = reserveSlot(studentToken, slotId).andReturnData().get("id").asLong();

		JsonNode payment = createBookingPayment(studentToken, bookingId)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("pending"))
				.andExpect(jsonPath("$.data.targetType").value("live_speaking_booking"))
				.andExpect(jsonPath("$.data.targetLiveSpeakingBookingId").value(bookingId))
				.andReturnData();

		String webhook = webhook(payment.get("providerPaymentId").asText(), "paid", "100000.00", "UZS");
		postWebhook(webhook, signature(webhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("paid"))
				.andExpect(jsonPath("$.data.confirmationDispatchedAt").isNotEmpty());
		postWebhook(webhook, signature(webhook))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("paid"));

		mockMvc.perform(get("/api/v1/student/schedule/bookings/{bookingId}", bookingId)
						.header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("booked"))
				.andExpect(jsonPath("$.data.paymentStatus").value("paid"))
				.andExpect(jsonPath("$.data.meetUrl").value("https://meet.google.com/mock-" + bookingId))
				.andExpect(jsonPath("$.data.providerSyncStatus").value("synced"));

		assertThat(jdbcTemplate.queryForObject("""
				select count(*)
				from teacher_transactions
				where source_type = 'live_speaking_booking'
				""", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from google_calendar_sync_jobs", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject(
				"select balance_amount from teacher_wallets where teacher_profile_id = ?",
				BigDecimal.class,
				teacher.profileId())).isEqualByComparingTo("75000.00");
	}

	private ResultWithData createSlot(String teacherToken) throws Exception {
		OffsetDateTime startsAt = now().plusDays(1).withNano(0);
		OffsetDateTime endsAt = startsAt.plusMinutes(30);
		return new ResultWithData(mockMvc.perform(post("/api/v1/teacher/schedule/slots")
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "startsAt": "%s",
						  "endsAt": "%s",
						  "timezone": "Asia/Tashkent",
						  "priceAmount": 100000.00,
						  "currency": "UZS"
						}
						""".formatted(startsAt, endsAt))));
	}

	private ResultWithData reserveSlot(String studentToken, Long slotId) throws Exception {
		return new ResultWithData(mockMvc.perform(post("/api/v1/student/schedule/slots/{slotId}/reserve", slotId)
				.header("Authorization", "Bearer " + studentToken)));
	}

	private ResultWithData createBookingPayment(String studentToken, Long bookingId) throws Exception {
		return new ResultWithData(mockMvc.perform(post("/api/v1/student/schedule/bookings/{bookingId}/payment", bookingId)
				.header("Authorization", "Bearer " + studentToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")));
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

	private TeacherFixture approvedTeacher(String prefix) throws Exception {
		String email = uniqueEmail(prefix);
		Long userId = createUserWithRole(email, "teacher");
		Long profileId = createTeacherProfile(userId);
		return new TeacherFixture(userId, profileId, login(email));
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

	private String login(String email) throws Exception {
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

	private Long createUserWithRole(String email, String roleCode) {
		Long userId = insertAndReturnId("""
				insert into users (email, password_hash, full_name, status, preferred_language)
				values (?, ?, ?, 'active', 'en')
				""",
				email,
				passwordHasher.hash(PASSWORD),
				"Fixture " + roleCode);
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

	private void cleanScheduleFixtures() {
		jdbcTemplate.update("""
				delete from promo_code_redemptions
				where payment_id in (
				  select id from payments
				  where target_live_speaking_booking_id in (select id from speaking_session_bookings)
				)
				""");
		jdbcTemplate.update("""
				delete from payment_disputes
				where payment_id in (
				  select id from payments
				  where target_live_speaking_booking_id in (select id from speaking_session_bookings)
				)
				""");
		jdbcTemplate.update("""
				delete from payments
				where target_live_speaking_booking_id in (select id from speaking_session_bookings)
				""");
		jdbcTemplate.update("delete from google_calendar_sync_jobs");
		jdbcTemplate.update("delete from speaking_session_bookings");
		jdbcTemplate.update("delete from speaking_session_slots");
		jdbcTemplate.update("delete from teacher_transactions where source_type = 'live_speaking_booking'");
		jdbcTemplate.update("delete from teacher_wallets where teacher_profile_id in (select id from teacher_profiles where user_id in ("
				+ SCHEDULE_USER_FILTER + "))");
		jdbcTemplate.update("delete from teacher_availability_blocks where teacher_profile_id in (select id from teacher_profiles where user_id in ("
				+ SCHEDULE_USER_FILTER + "))");
		jdbcTemplate.update("delete from google_calendar_connections where teacher_profile_id in (select id from teacher_profiles where user_id in ("
				+ SCHEDULE_USER_FILTER + "))");
		jdbcTemplate.update("delete from notifications where user_id in (" + SCHEDULE_USER_FILTER + ")");
		jdbcTemplate.update("delete from teacher_profiles where user_id in (" + SCHEDULE_USER_FILTER + ")");
		jdbcTemplate.update("delete from user_roles where user_id in (" + SCHEDULE_USER_FILTER + ")");
		jdbcTemplate.update("delete from users where email like 'schedule-%@example.test'");
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private record TeacherFixture(Long userId, Long profileId, String accessToken) {
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
