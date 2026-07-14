package com.mockexams.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;

import com.mockexams.backend.dto.StudyPlanCDO;
import com.mockexams.backend.dto.StudyPlanRDO;
import com.mockexams.backend.dto.StudyPlanTaskRDO;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.service.studyplan.StudyPlanPrompt;
import com.mockexams.backend.service.studyplan.StudyPlanProvider;
import com.mockexams.backend.service.studyplan.StudyPlanProviderException;
import com.mockexams.backend.service.studyplan.StudyPlanProviderResponse;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"app.ai.study-plan.provider=mock",
		"app.ai.usage.free-requests-limit=10",
		"app.ai.usage.hard-limit-requests=10"
})
@ActiveProfiles("test")
class StudyPlanServiceTest {

	@Autowired
	private StudyPlanService studyPlanService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ControlledStudyPlanProvider provider;

	@BeforeEach
	void resetProvider() {
		provider.reset();
		jdbcTemplate.update("delete from study_plan_tasks");
		jdbcTemplate.update("delete from study_plans");
		jdbcTemplate.update("delete from ai_feedbacks");
		jdbcTemplate.update("delete from ai_usage_limits");
		jdbcTemplate.update("delete from purchases");
		jdbcTemplate.update("delete from subscriptions");
		jdbcTemplate.update("delete from marketplace_listings");
		jdbcTemplate.update("delete from test_results");
		jdbcTemplate.update("delete from test_attempts");
		jdbcTemplate.update("delete from tests");
		jdbcTemplate.update("delete from teacher_profiles");
	}

	@Test
	void planGenerationRequiresTargetBandAndDuration() {
		User student = createUser("study-plan-validation");

		assertThatThrownBy(() -> studyPlanService.create(student, new StudyPlanCDO(null, 30, 60, null)))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("VALIDATION_FAILED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
				});
		assertThatThrownBy(() -> studyPlanService.create(student,
				new StudyPlanCDO(new BigDecimal("7.0"), null, 60, null)))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("VALIDATION_FAILED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
				});
		assertThat(provider.calls()).isZero();
	}

	@Test
	void createdTasksAreStoredOrderedByDayAndOrder() {
		User student = createUser("study-plan-ordered");
		Long authorId = createUserId("study-plan-ordered-author");
		assignRole(authorId, "admin");
		Long readingTestId = createTest(authorId, "Reading Diagnostic", TestTypes.READING, true, "published");
		provider.validPlanWithTest(readingTestId);

		StudyPlanRDO plan = studyPlanService.create(student, validCommand());

		assertThat(plan.targetBand()).isEqualByComparingTo("7.0");
		assertThat(plan.durationDays()).isEqualTo(3);
		assertThat(plan.tasks()).extracting(StudyPlanTaskRDO::taskDate)
				.containsExactly(
						LocalDate.of(2026, 7, 1),
						LocalDate.of(2026, 7, 1),
						LocalDate.of(2026, 7, 2));
		assertThat(plan.tasks()).extracting(StudyPlanTaskRDO::taskOrder)
				.containsExactly(1, 2, 1);
		assertThat(plan.tasks().getFirst().skill()).isEqualTo(TestTypes.READING);
		assertThat(plan.tasks().getFirst().estimatedMinutes()).isEqualTo(45);
		assertThat(plan.tasks().getFirst().recommendedResource().type()).isEqualTo("test");
		assertThat(plan.tasks().getFirst().recommendedResource().id()).isEqualTo(readingTestId);

		String feedbackType = jdbcTemplate.queryForObject(
				"select feedback_type from ai_feedbacks where id = ?",
				String.class,
				plan.createdByFeedbackId());
		assertThat(feedbackType).isEqualTo("study_plan");
	}

	@Test
	void planWithPartialDataLabelsMissingSkillsInsteadOfFabricatingScores() {
		User student = createUser("study-plan-partial");
		provider.partialDataPlan();

		StudyPlanRDO plan = studyPlanService.create(student, validCommand());

		assertThat(plan.currentBand()).isNull();
		assertThat(plan.planSummary())
				.containsEntry("uncertaintyNote", "Only limited recent evidence is available; complete diagnostics first.");
		assertThat(plan.planSummary().get("missingSkills").toString()).contains("reading", "listening", "writing", "speaking");
		assertThat(provider.lastPrompt().userPrompt()).contains("Missing skills: [listening, reading, writing, speaking]");
	}

	@Test
	void activePlanConflictPreventsSecondActivePlan() {
		User student = createUser("study-plan-conflict");
		provider.validPlan();
		studyPlanService.create(student, validCommand());

		assertThatThrownBy(() -> studyPlanService.create(student, validCommand()))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("STUDY_PLAN_ACTIVE_CONFLICT");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				});
	}

	@Test
	void inactiveListingRecommendationIsRejectedAndNotSaved() {
		User student = createUser("study-plan-inactive-listing");
		Long listingId = createListing("Inactive paid pack", false, new BigDecimal("15000.00"));
		provider.validPlanWithListing(listingId);

		assertThatThrownBy(() -> studyPlanService.create(student, validCommand()))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("INACCESSIBLE_RECOMMENDED_CONTENT");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				});

		Integer taskRows = jdbcTemplate.queryForObject("select count(*) from study_plan_tasks", Integer.class);
		Integer failedFeedbackRows = jdbcTemplate.queryForObject("""
				select count(*)
				from ai_feedbacks
				where feedback_type = 'study_plan' and status = 'failed' and failure_code = 'VALIDATION_FAILED'
				""", Integer.class);
		assertThat(taskRows).isZero();
		assertThat(failedFeedbackRows).isEqualTo(1);
	}

	@Test
	void suspendedTeacherListingIsNotOfferedOrAcceptedAsRecommendation() {
		User student = createUser("study-plan-suspended-listing");
		String listingTitle = "Suspended teacher free pack";
		Long listingId = createListingWithTeacherStatus(listingTitle, true, BigDecimal.ZERO, "suspended");
		provider.validPlanWithListing(listingId);

		assertInaccessibleRecommendationRejected(student);
		assertThat(provider.lastPrompt().userPrompt()).doesNotContain(listingTitle);
	}

	@Test
	void revokedPurchaseRecommendationIsRejectedAndNotSaved() {
		User student = createUser("study-plan-revoked-purchase");
		Long listingId = createListing("Revoked paid pack", true, new BigDecimal("15000.00"));
		createPurchase(student.getId(), listingId, "revoked", null);
		provider.validPlanWithListing(listingId);

		assertInaccessibleRecommendationRejected(student);
	}

	@Test
	void futureSubscriptionRecommendationIsRejectedAndNotSaved() {
		User student = createUser("study-plan-future-subscription");
		Long listingId = createListing("Future subscription pack", true, new BigDecimal("15000.00"));
		createSubscription(
				student.getId(),
				listingId,
				"active",
				OffsetDateTime.now(ZoneOffset.UTC).plusDays(1),
				OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
		provider.validPlanWithListing(listingId);

		assertInaccessibleRecommendationRejected(student);
	}

	@Test
	void unsupportedProviderSkillFailsSafelyWithoutSavingPlan() {
		User student = createUser("study-plan-bad-skill");
		provider.unsupportedSkillPlan();

		assertThatThrownBy(() -> studyPlanService.create(student, validCommand()))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("AI_STUDY_PLAN_VALIDATION_FAILED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
				});

		Integer planRows = jdbcTemplate.queryForObject("select count(*) from study_plans", Integer.class);
		Integer taskRows = jdbcTemplate.queryForObject("select count(*) from study_plan_tasks", Integer.class);
		Integer failedFeedbackRows = jdbcTemplate.queryForObject("""
				select count(*)
				from ai_feedbacks
				where feedback_type = 'study_plan' and status = 'failed' and failure_code = 'VALIDATION_FAILED'
				""", Integer.class);
		assertThat(planRows).isZero();
		assertThat(taskRows).isZero();
		assertThat(failedFeedbackRows).isEqualTo(1);
	}

	@Test
	void providerFailureDoesNotSavePlanOrTasksAndReleasesUsageReservation() {
		User student = createUser("study-plan-provider-failure");
		provider.failNext();

		assertThatThrownBy(() -> studyPlanService.create(student, validCommand()))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("SERVICE_UNAVAILABLE");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
				});

		Integer planRows = jdbcTemplate.queryForObject("select count(*) from study_plans", Integer.class);
		Integer taskRows = jdbcTemplate.queryForObject("select count(*) from study_plan_tasks", Integer.class);
		Integer failedFeedbackRows = jdbcTemplate.queryForObject("""
				select count(*)
				from ai_feedbacks
				where feedback_type = 'study_plan' and status = 'failed' and failure_code = 'AI_PROVIDER_FAILURE'
				""", Integer.class);
		Integer usedRequests = jdbcTemplate.queryForObject(
				"select used_requests from ai_usage_limits where user_id = ?",
				Integer.class,
				student.getId());
		assertThat(planRows).isZero();
		assertThat(taskRows).isZero();
		assertThat(failedFeedbackRows).isEqualTo(1);
		assertThat(usedRequests).isZero();
	}

	@Test
	void regenerationPreservesCompletedTasksAndDropsInactiveListingRecommendation() {
		User student = createUser("study-plan-regenerate");
		Long authorId = createUserId("study-plan-regenerate-author");
		assignRole(authorId, "admin");
		Long readingTestId = createTest(authorId, "Reading Refresh", TestTypes.READING, true, "published");
		provider.validPlanWithTest(readingTestId);
		StudyPlanRDO plan = studyPlanService.create(student, validCommand());
		StudyPlanTaskRDO completed = studyPlanService.completeTask(student, plan.id(), plan.tasks().getFirst().id());
		Long inactiveListingId = createListing("Inactive pack", false, BigDecimal.ZERO);
		provider.validPlanWithListing(inactiveListingId);

		assertThatThrownBy(() -> studyPlanService.regenerate(student, plan.id()))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("INACCESSIBLE_RECOMMENDED_CONTENT");
				});

		StudyPlanRDO unchanged = studyPlanService.get(student, plan.id());
		assertThat(unchanged.tasks()).extracting(StudyPlanTaskRDO::id).contains(completed.id());
		assertThat(unchanged.tasks().stream().filter(StudyPlanTaskRDO::completed)).hasSize(1);

		provider.validRegenerationWithTest(readingTestId);
		StudyPlanRDO regenerated = studyPlanService.regenerate(student, plan.id());

		assertThat(regenerated.tasks()).anySatisfy(task -> {
			assertThat(task.id()).isEqualTo(completed.id());
			assertThat(task.completed()).isTrue();
		});
		assertThat(regenerated.tasks()).noneSatisfy(task ->
				assertThat(task.recommendedResource()).satisfies(resource -> {
					assertThat(resource.type()).isEqualTo("listing");
					assertThat(resource.id()).isEqualTo(inactiveListingId);
				}));
		assertThat(regenerated.tasks().stream().filter(task -> !task.completed())).hasSize(2);
	}

	private StudyPlanCDO validCommand() {
		return new StudyPlanCDO(new BigDecimal("7.0"), 3, 60, LocalDate.of(2026, 7, 1));
	}

	private Long createTest(Long authorUserId, String title, String type, boolean free, String visibility) {
		return insertAndReturnId("""
				insert into tests (
				  teacher_profile_id, title, description, type, difficulty, estimated_band_min,
				  estimated_band_max, duration_minutes, price_amount, currency, is_free, language,
				  visibility, published_at, created_by_user_id
				)
				values (null, ?, 'Study plan test fixture.', ?, 'intermediate', 5.5, 7.0, 60, ?, 'UZS', ?, 'en', ?, ?, ?)
				""",
				title,
				type,
				free ? BigDecimal.ZERO : new BigDecimal("15000.00"),
				free,
				visibility,
				OffsetDateTime.of(2026, 6, 30, 12, 0, 0, 0, ZoneOffset.UTC),
				authorUserId);
	}

	private Long createListing(String title, boolean active, BigDecimal priceAmount) {
		return createListingWithTeacherStatus(title, active, priceAmount, "approved");
	}

	private Long createListingWithTeacherStatus(String title, boolean active, BigDecimal priceAmount, String teacherStatus) {
		Long teacherUserId = createUserId("study-plan-listing-teacher");
		Long teacherProfileId = createTeacherProfile(teacherUserId, title, teacherStatus);
		return insertAndReturnId("""
				insert into marketplace_listings (
				  teacher_profile_id, test_id, title, description, listing_type, price_amount, currency, is_active
				)
				values (?, null, ?, 'Study plan listing fixture.', 'vocabulary_pack', ?, 'UZS', ?)
				""", teacherProfileId, title, priceAmount, active);
	}

	private Long createTeacherProfile(Long userId, String displayName, String status) {
		OffsetDateTime approvedAt = OffsetDateTime.of(2026, 6, 30, 12, 0, 0, 0, ZoneOffset.UTC);
		return insertAndReturnId("""
				insert into teacher_profiles (
				  user_id, display_name, headline, bio, status, approved_by_user_id, approved_at
				)
				values (?, ?, 'IELTS teacher', 'Experienced teacher.', ?, ?, ?)
				""",
				userId,
				displayName,
				status,
				"approved".equals(status) ? userId : null,
				"approved".equals(status) ? approvedAt : null);
	}

	private Long createPurchase(Long studentId, Long listingId, String status, OffsetDateTime accessExpiresAt) {
		return insertAndReturnId("""
				insert into purchases (
				  student_user_id, listing_id, status, source_type, amount, currency, purchased_at,
				  access_expires_at, updated_at
				)
				values (?, ?, ?, 'payment', 15000.00, 'UZS', ?, ?, ?)
				""",
				studentId,
				listingId,
				status,
				OffsetDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC),
				accessExpiresAt,
				OffsetDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC));
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

	private void assertInaccessibleRecommendationRejected(User student) {
		assertThatThrownBy(() -> studyPlanService.create(student, validCommand()))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("INACCESSIBLE_RECOMMENDED_CONTENT");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				});

		Integer taskRows = jdbcTemplate.queryForObject("select count(*) from study_plan_tasks", Integer.class);
		Integer failedFeedbackRows = jdbcTemplate.queryForObject("""
				select count(*)
				from ai_feedbacks
				where feedback_type = 'study_plan' and status = 'failed' and failure_code = 'VALIDATION_FAILED'
				""", Integer.class);
		assertThat(taskRows).isZero();
		assertThat(failedFeedbackRows).isEqualTo(1);
	}

	private User createUser(String prefix) {
		return userRepository.findById(createUserId(prefix)).orElseThrow();
	}

	private Long createUserId(String prefix) {
		String email = prefix + "-" + UUID.randomUUID() + "@example.test";
		jdbcTemplate.update("""
				insert into users (email, password_hash, full_name)
				values (?, 'hash', 'Study Plan Student')
				""", email);
		return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
	}

	private void assignRole(Long userId, String roleCode) {
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select ?, role.id
				from roles role
				where role.code = ?
				""", userId, roleCode);
	}

	private Long insertAndReturnId(String sql, Object... args) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql, new String[] { "id" });
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				if (arg instanceof LocalDate localDate) {
					statement.setDate(i + 1, Date.valueOf(localDate));
				}
				else if (arg instanceof OffsetDateTime offsetDateTime) {
					statement.setTimestamp(i + 1, Timestamp.from(offsetDateTime.toInstant()));
				}
				else {
					statement.setObject(i + 1, arg);
				}
			}
			return statement;
		}, keyHolder);
		return keyHolder.getKey().longValue();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class StudyPlanServiceTestConfig {

		@Bean
		@Primary
		ControlledStudyPlanProvider controlledStudyPlanProvider() {
			return new ControlledStudyPlanProvider();
		}

		@Bean
		@Primary
		Clock studyPlanClock() {
			return Clock.fixed(Instant.parse("2026-07-01T10:00:00Z"), ZoneId.of("UTC"));
		}
	}

	static class ControlledStudyPlanProvider implements StudyPlanProvider {

		private final AtomicInteger calls = new AtomicInteger();
		private Mode mode = Mode.VALID;
		private Long resourceId;
		private String resourceType = "none";
		private StudyPlanPrompt lastPrompt;

		@Override
		public StudyPlanProviderResponse generate(StudyPlanPrompt prompt) {
			calls.incrementAndGet();
			lastPrompt = prompt;
			if (mode == Mode.FAIL) {
				throw new StudyPlanProviderException("Provider failed.");
			}
			String content = switch (mode) {
				case BAD_SKILL -> badSkillJson();
				case PARTIAL -> partialJson();
				case REGENERATE -> regenerationJson(resourceId);
				default -> validJson(resourceType, resourceId);
			};
			mode = Mode.VALID;
			resourceType = "none";
			resourceId = null;
			return new StudyPlanProviderResponse("mock", "mock-study-plan-test", content, 40, 30, BigDecimal.ZERO);
		}

		void reset() {
			calls.set(0);
			mode = Mode.VALID;
			resourceType = "none";
			resourceId = null;
			lastPrompt = null;
		}

		void validPlan() {
			mode = Mode.VALID;
		}

		void validPlanWithTest(Long testId) {
			mode = Mode.VALID;
			resourceType = "test";
			resourceId = testId;
		}

		void validPlanWithListing(Long listingId) {
			mode = Mode.VALID;
			resourceType = "listing";
			resourceId = listingId;
		}

		void partialDataPlan() {
			mode = Mode.PARTIAL;
		}

		void unsupportedSkillPlan() {
			mode = Mode.BAD_SKILL;
		}

		void failNext() {
			mode = Mode.FAIL;
		}

		void validRegenerationWithTest(Long testId) {
			mode = Mode.REGENERATE;
			resourceId = testId;
		}

		int calls() {
			return calls.get();
		}

		StudyPlanPrompt lastPrompt() {
			return lastPrompt;
		}

		private String validJson(String type, Long id) {
			String resource = resourceJson(type, id);
			return """
					{
					  "summary": {
					    "overview": "A focused three-day study plan.",
					    "uncertaintyNote": "Evidence is sufficient for initial planning.",
					    "missingSkills": [],
					    "evidence": ["Recent practice data and feedback were reviewed."]
					  },
					  "tasks": [
					    {
					      "date": "2026-07-01",
					      "skill": "reading",
					      "title": "Reading diagnostic review",
					      "description": "Review recent reading mistakes and complete a targeted passage.",
					      "estimatedMinutes": 45,
					      "recommendedResource": %s,
					      "rationale": "Reading needs immediate diagnostic evidence."
					    },
					    {
					      "date": "2026-07-01",
					      "skill": "listening",
					      "title": "Listening gap-fill practice",
					      "description": "Practice one listening section and review missed spellings.",
					      "estimatedMinutes": 30,
					      "recommendedResource": {"type": "diagnostic", "title": "Listening diagnostic"},
					      "rationale": "Listening is missing recent objective evidence."
					    },
					    {
					      "date": "2026-07-02",
					      "skill": "writing",
					      "title": "Task 2 planning drill",
					      "description": "Outline two opinion essays before writing one introduction.",
					      "estimatedMinutes": 40,
					      "recommendedResource": {"type": "diagnostic", "title": "Writing diagnostic"},
					      "rationale": "Writing feedback should be converted into a repeatable plan."
					    }
					  ]
					}
					""".formatted(resource);
		}

		private String partialJson() {
			return """
					{
					  "summary": {
					    "overview": "Start with diagnostics before making score-specific claims.",
					    "uncertaintyNote": "Only limited recent evidence is available; complete diagnostics first.",
					    "missingSkills": ["reading", "listening", "writing", "speaking"],
					    "evidence": ["No recent scored attempts were found."]
					  },
					  "tasks": [
						    {
						      "date": "2026-07-01",
						      "skill": "reading",
						      "title": "Complete a baseline diagnostic",
						      "description": "Take one short reading or listening diagnostic and save the result.",
						      "estimatedMinutes": 45,
					      "recommendedResource": {"type": "diagnostic", "title": "Baseline diagnostic"},
					      "rationale": "Missing skills must be measured before detailed score advice."
					    }
					  ]
					}
					""";
		}

		private String badSkillJson() {
			return """
					{
					  "summary": {
					    "overview": "This plan contains an unsupported skill.",
					    "uncertaintyNote": "Invalid provider response.",
					    "missingSkills": [],
					    "evidence": []
					  },
					  "tasks": [
					    {
					      "date": "2026-07-01",
					      "skill": "grammar",
					      "title": "Invalid skill task",
					      "description": "This task should not be accepted.",
					      "estimatedMinutes": 30,
					      "recommendedResource": {"type": "diagnostic", "title": "Diagnostic practice"},
					      "rationale": "Unsupported skill must fail schema validation."
					    }
					  ]
					}
					""";
		}

		private String regenerationJson(Long testId) {
			return """
					{
					  "summary": {
					    "overview": "Refresh the remaining plan based on preserved completed work.",
					    "uncertaintyNote": "Future work was regenerated.",
					    "missingSkills": [],
					    "evidence": ["Completed tasks were preserved."]
					  },
					  "tasks": [
					    {
					      "date": "2026-07-01",
					      "skill": "reading",
					      "title": "Fresh reading drill",
					      "description": "Complete a fresh reading task.",
					      "estimatedMinutes": 35,
					      "recommendedResource": %s,
					      "rationale": "Regenerated work should focus on next evidence."
					    },
					    {
					      "date": "2026-07-02",
					      "skill": "speaking",
					      "title": "Speaking answer expansion",
					      "description": "Record two extended answers.",
					      "estimatedMinutes": 30,
					      "recommendedResource": {"type": "diagnostic", "title": "Speaking diagnostic"},
					      "rationale": "Speaking needs a refreshed transcript sample."
					    }
					  ]
					}
					""".formatted(resourceJson("test", testId));
		}

		private String resourceJson(String type, Long id) {
			if ("test".equals(type)) {
				return "{\"type\": \"test\", \"id\": %d, \"title\": \"Recommended test\"}".formatted(id);
			}
			if ("listing".equals(type)) {
				return "{\"type\": \"listing\", \"id\": %d, \"title\": \"Recommended listing\"}".formatted(id);
			}
			return "{\"type\": \"diagnostic\", \"title\": \"Diagnostic practice\"}";
		}

		private enum Mode {
			VALID,
			PARTIAL,
			REGENERATE,
			BAD_SKILL,
			FAIL
		}
	}
}
