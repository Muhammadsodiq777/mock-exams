package com.mockexams.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class AdminPlatformSettingControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanUsers() {
		jdbcTemplate.update("update platform_settings set updated_by_user_id = null");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from notifications");
		jdbcTemplate.update("delete from user_roles");
		jdbcTemplate.update("delete from users");
	}

	@Test
	void adminCanListAndReadSupportedSettingsWithoutSecrets() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("settings-list-admin"));

		String response = mockMvc.perform(get("/api/v1/admin/settings")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].key").value(org.hamcrest.Matchers.hasItem("marketplace.default_commission_percent")))
				.andExpect(jsonPath("$.data[*].key").value(org.hamcrest.Matchers.hasItem("google.calendar_meet")))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(response).doesNotContain("clientSecret");
		assertThat(response).doesNotContain("apiKey");
		assertThat(response).doesNotContain("webhookSecret");

		mockMvc.perform(get("/api/v1/admin/settings/{key}", "marketplace.default_commission_percent")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.key").value("marketplace.default_commission_percent"))
				.andExpect(jsonPath("$.data.value.value").value(25.00));
	}

	@Test
	void adminCanUpdateSettingAndAuditUser() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("settings-update-admin"));

		mockMvc.perform(patch("/api/v1/admin/settings/{key}", "marketplace.default_commission_percent")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": { "value": 18.50 }
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.value.value").value(18.50))
				.andExpect(jsonPath("$.data.updatedByUserId").isNumber())
				.andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

		JsonNode stored = objectMapper.readTree(jdbcTemplate.queryForObject(
				"select setting_value from platform_settings where setting_key = ?",
				String.class,
				"marketplace.default_commission_percent"));
		assertThat(stored.path("value").decimalValue()).isEqualByComparingTo("18.50");
		Long updatedBy = jdbcTemplate.queryForObject(
				"select updated_by_user_id from platform_settings where setting_key = ?",
				Long.class,
				"marketplace.default_commission_percent");
		assertThat(updatedBy).isNotNull();
	}

	@Test
	void nonAdminCannotUpdateSettings() throws Exception {
		String studentToken = registerStudent(uniqueEmail("settings-forbidden-student")).accessToken();

		mockMvc.perform(patch("/api/v1/admin/settings/{key}", "marketplace.default_commission_percent")
						.header("Authorization", "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "value": { "value": 18.50 } }
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void unknownSettingFailsWithStableCode() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("settings-unknown-admin"));

		mockMvc.perform(get("/api/v1/admin/settings/{key}", "does.not.exist")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("UNKNOWN_SETTING"));
	}

	@Test
	void invalidSettingShapeIsRejected() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("settings-invalid-admin"));

		mockMvc.perform(patch("/api/v1/admin/settings/{key}", "marketplace.default_commission_percent")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "value": { "value": 125.00 } }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void googleIntegrationCannotBeEnabledWithoutEnvironmentConfiguration() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("settings-google-admin"));

		mockMvc.perform(patch("/api/v1/admin/settings/{key}", "google.calendar_meet")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": {
								    "enabled": true,
								    "mode": "platform_calendar",
								    "calendarOwner": "platform"
								  }
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("INTEGRATION_CONFIGURATION_MISSING"));
	}

	@Test
	void googleIntegrationCannotBypassEnvironmentConfigurationWithUppercaseMode() throws Exception {
		String adminToken = registerAdmin(uniqueEmail("settings-google-upper-admin"));

		mockMvc.perform(patch("/api/v1/admin/settings/{key}", "google.calendar_meet")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": {
								    "enabled": true,
								    "mode": "PLATFORM_CALENDAR",
								    "calendarOwner": "PLATFORM"
								  }
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("INTEGRATION_CONFIGURATION_MISSING"));
	}

	private String registerAdmin(String email) throws Exception {
		Registration registration = registerStudent(email);
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select u.id, r.id
				from users u
				join roles r on r.code = 'admin'
				where u.email = ?
				""", email);
		return registration.accessToken();
	}

	private Registration registerStudent(String email) throws Exception {
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
		JsonNode root = objectMapper.readTree(response);
		return new Registration(
				root.at("/data/accessToken").asText(),
				root.at("/data/user/id").asLong());
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private record Registration(String accessToken, Long userId) {
	}
}
