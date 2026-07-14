package com.mockexams.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"app.openai.enabled=false",
		"app.ai.study-plan.provider=mock",
		"app.ai.usage.free-requests-limit=1",
		"app.ai.usage.hard-limit-requests=2"
})
@ActiveProfiles("test")
class AiUsageDisabledProviderServiceTest {

	@Autowired
	private AiUsageService aiUsageService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void mockStudyPlanProviderCanReserveWhenOpenAiIsDisabled() {
		Long userId = createUser();

		AiUsageReservation reservation = aiUsageService.guardAndReserve(userId, AiUsageRequest.studyPlan());

		assertThat(aiUsageService.remainingUsage(userId).usedRequests()).isEqualTo(1);
		String status = jdbcTemplate.queryForObject("""
				select status
				from ai_feedbacks
				where id = ?
				""", String.class, reservation.feedbackId());
		assertThat(status).isEqualTo("processing");
	}

	private Long createUser() {
		String email = "ai-disabled-" + UUID.randomUUID() + "@example.test";
		jdbcTemplate.update("""
				insert into users (email, password_hash, full_name)
				values (?, 'hash', 'AI Disabled User')
				""", email);
		return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
	}
}
