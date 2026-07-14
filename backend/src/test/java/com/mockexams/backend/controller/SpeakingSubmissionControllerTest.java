package com.mockexams.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.mockexams.backend.service.FileStorageService;
import com.mockexams.backend.service.StoredFileContent;
import com.mockexams.backend.service.StoredFileObject;
import com.mockexams.backend.service.TranscriptionProviderClient;
import com.mockexams.backend.service.TranscriptionProviderException;
import com.mockexams.backend.service.TranscriptionResult;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"app.transcription.provider=mock",
		"app.transcription.max-audio-duration=PT1M"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpeakingSubmissionControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockBean
	private FileStorageService fileStorageService;

	@MockBean
	private TranscriptionProviderClient transcriptionProviderClient;

	@BeforeEach
	void setUp() {
		reset(fileStorageService, transcriptionProviderClient);
		when(fileStorageService.store(any(String.class), any(MultipartFile.class)))
				.thenAnswer(invocation -> new StoredFileObject("mock-exams-test", invocation.getArgument(0), "etag"));
		when(fileStorageService.read(any()))
				.thenReturn(new StoredFileContent("answer.webm", "audio/webm", "audio".getBytes(StandardCharsets.UTF_8)));
		when(transcriptionProviderClient.transcribe(any()))
				.thenReturn(new TranscriptionResult(
						"Mock transcript from provider.",
						"mock",
						"mock-transcriber",
						BigDecimal.ZERO));
	}

	@AfterEach
	void cleanUpSpeakingFixtures() {
		jdbcTemplate.update("delete from speaking_submissions");
	}

	@Test
	void nonSpeakingAudioFileIsRejected() throws Exception {
		String accessToken = registerStudent(uniqueEmail("speaking-purpose"));
		Long studentUserId = currentUserId(accessToken);
		Long audioFileId = insertFile(studentUserId, "listening_audio", 30);

		mockMvc.perform(post("/api/v1/student/speaking-submissions")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(submitJson("speaking_part_1", "Describe your hometown.", audioFileId, 30)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		verifyNoInteractions(transcriptionProviderClient);
	}

	@Test
	void missingPromptIsRejected() throws Exception {
		String accessToken = registerStudent(uniqueEmail("speaking-prompt"));
		Long audioFileId = uploadSpeakingAudio(accessToken);

		mockMvc.perform(post("/api/v1/student/speaking-submissions")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(submitJson("speaking_part_1", "   ", audioFileId, 30)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		verifyNoInteractions(transcriptionProviderClient);
	}

	@Test
	void mockTranscriptIsSavedAndReadable() throws Exception {
		String accessToken = registerStudent(uniqueEmail("speaking-success"));
		Long audioFileId = uploadSpeakingAudio(accessToken);

		String response = mockMvc.perform(post("/api/v1/student/speaking-submissions")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(submitJson("speaking_part_2", "Talk about a memorable journey.", audioFileId, 45)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.id").isNumber())
				.andExpect(jsonPath("$.data.audioFileId").value(audioFileId))
				.andExpect(jsonPath("$.data.transcriptionStatus").value("completed"))
				.andExpect(jsonPath("$.data.transcript").value("Mock transcript from provider."))
				.andExpect(jsonPath("$.data.provider").value("mock"))
				.andExpect(jsonPath("$.data.model").value("mock-transcriber"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		Long submissionId = objectMapper.readTree(response).at("/data/id").asLong();

		mockMvc.perform(get("/api/v1/student/speaking-submissions/{submissionId}", submissionId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(submissionId))
				.andExpect(jsonPath("$.data.transcriptionStatus").value("completed"))
				.andExpect(jsonPath("$.data.transcript").value("Mock transcript from provider."));
	}

	@Test
	void providerFailureIsVisibleWithoutLosingAudio() throws Exception {
		when(transcriptionProviderClient.transcribe(any()))
				.thenThrow(new TranscriptionProviderException("AI_PROVIDER_FAILURE", "Provider timed out."));
		String accessToken = registerStudent(uniqueEmail("speaking-failure"));
		Long audioFileId = uploadSpeakingAudio(accessToken);

		String response = mockMvc.perform(post("/api/v1/student/speaking-submissions")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(submitJson("speaking_part_3", "Should cities invest in public transport?", audioFileId, 50)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.transcriptionStatus").value("failed"))
				.andExpect(jsonPath("$.data.failureCode").value("AI_PROVIDER_FAILURE"))
				.andExpect(jsonPath("$.data.audioFileId").value(audioFileId))
				.andReturn()
				.getResponse()
				.getContentAsString();
		Long submissionId = objectMapper.readTree(response).at("/data/id").asLong();

		Integer fileCount = jdbcTemplate.queryForObject("select count(*) from files where id = ?", Integer.class, audioFileId);
		Integer submissionCount = jdbcTemplate.queryForObject(
				"select count(*) from speaking_submissions where id = ?", Integer.class, submissionId);
		org.assertj.core.api.Assertions.assertThat(fileCount).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(submissionCount).isEqualTo(1);
	}

	@Test
	void duplicateSubmitDoesNotCreateDuplicateTranscriptionJob() throws Exception {
		String accessToken = registerStudent(uniqueEmail("speaking-duplicate"));
		Long audioFileId = uploadSpeakingAudio(accessToken);
		String request = submitJson("speaking_part_1", "Describe a useful skill.", audioFileId, 30);

		String first = mockMvc.perform(post("/api/v1/student/speaking-submissions")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Long firstId = objectMapper.readTree(first).at("/data/id").asLong();

		String second = mockMvc.perform(post("/api/v1/student/speaking-submissions")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Long secondId = objectMapper.readTree(second).at("/data/id").asLong();

		org.assertj.core.api.Assertions.assertThat(secondId).isEqualTo(firstId);
		verify(transcriptionProviderClient, times(1)).transcribe(any());
	}

	@Test
	void audioOverMaxDurationIsRejectedBeforeProviderCall() throws Exception {
		String accessToken = registerStudent(uniqueEmail("speaking-duration"));
		Long audioFileId = uploadSpeakingAudio(accessToken);

		mockMvc.perform(post("/api/v1/student/speaking-submissions")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(submitJson("speaking_part_1", "Tell me about your studies.", audioFileId, 61)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		verifyNoInteractions(transcriptionProviderClient);
	}

	@Test
	void storedAudioDurationIsUsedWhenRequestUnderReportsDuration() throws Exception {
		String accessToken = registerStudent(uniqueEmail("speaking-stored-duration"));
		Long studentUserId = currentUserId(accessToken);
		Long audioFileId = insertFile(studentUserId, "speaking_audio", 120);

		mockMvc.perform(post("/api/v1/student/speaking-submissions")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(submitJson("speaking_part_2", "Describe a useful object.", audioFileId, 30)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

		verifyNoInteractions(transcriptionProviderClient);
	}

	private Long uploadSpeakingAudio(String accessToken) throws Exception {
		String uploadResponse = mockMvc.perform(multipart("/api/v1/student/files")
						.file(new MockMultipartFile("file", "answer.webm", "audio/webm", "audio".getBytes()))
						.param("purpose", "speaking_audio")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(uploadResponse).at("/data/id").asLong();
	}

	private Long insertFile(Long ownerUserId, String purpose, Integer durationSeconds) {
		String objectKey = "test/" + UUID.randomUUID();
		jdbcTemplate.update("""
				insert into files (
				  owner_user_id, purpose, bucket, object_key, original_name, content_type, size_bytes, duration_seconds
				)
				values (?, ?, 'mock-exams-test', ?, 'audio.mp3', 'audio/mpeg', 5, ?)
				""", ownerUserId, purpose, objectKey, durationSeconds);
		return jdbcTemplate.queryForObject("select id from files where object_key = ?", Long.class, objectKey);
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

	private Long currentUserId(String accessToken) throws Exception {
		String response = mockMvc.perform(get("/api/v1/auth/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).at("/data/id").asLong();
	}

	private String submitJson(String partType, String prompt, Long audioFileId, Integer durationSeconds) {
		return """
				{
				  "partType": "%s",
				  "prompt": "%s",
				  "audioFileId": %d,
				  "durationSeconds": %d
				}
				""".formatted(partType, prompt, audioFileId, durationSeconds);
	}

	private String accessTokenFrom(String response) throws Exception {
		JsonNode root = objectMapper.readTree(response);
		return root.at("/data/accessToken").asText();
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}
}
