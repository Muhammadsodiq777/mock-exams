package com.mockexams.backend.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.mockexams.backend.service.FileStorageService;
import com.mockexams.backend.service.StoredFileObject;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileControllerTest {

	private static final String PASSWORD = "correct-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockBean
	private FileStorageService fileStorageService;

	@BeforeEach
	void setUp() {
		when(fileStorageService.store(anyString(), any(MultipartFile.class)))
				.thenAnswer(invocation -> new StoredFileObject("mock-exams-test", invocation.getArgument(0), "etag"));
		when(fileStorageService.createDownloadUrl(any()))
				.thenReturn("http://localhost:9000/mock-exams-test/signed-url");
	}

	@Test
	void teacherCanUploadListeningAudio() throws Exception {
		String email = uniqueEmail("teacher-file");
		String accessToken = registerTeacher(email);

		mockMvc.perform(multipart("/api/v1/teacher/files")
						.file(upload("file", "listening.mp3", "audio/mpeg", "audio"))
						.param("purpose", "listening_audio")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.id").isNumber())
				.andExpect(jsonPath("$.data.ownerUserId").value(userIdFor(email)))
				.andExpect(jsonPath("$.data.purpose").value("listening_audio"))
				.andExpect(jsonPath("$.data.originalName").value("listening.mp3"))
				.andExpect(jsonPath("$.data.contentType").value("audio/mpeg"))
				.andExpect(jsonPath("$.data.sizeBytes").value(5))
				.andExpect(jsonPath("$.data.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	@Test
	void studentCanUploadSpeakingAudio() throws Exception {
		String accessToken = registerStudent(uniqueEmail("student-file"));

		mockMvc.perform(multipart("/api/v1/student/files")
						.file(upload("file", "speaking.webm", "audio/webm", "audio"))
						.param("purpose", "speaking_audio")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.purpose").value("speaking_audio"))
				.andExpect(jsonPath("$.data.originalName").value("speaking.webm"))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	@Test
	void adminCanUploadBannerImage() throws Exception {
		String accessToken = registerAdmin(uniqueEmail("admin-file"));

		mockMvc.perform(multipart("/api/v1/admin/files")
						.file(upload("file", "banner.png", "image/png", "image"))
						.param("purpose", "banner_image")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.purpose").value("banner_image"))
				.andExpect(jsonPath("$.data.originalName").value("banner.png"))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	@Test
	void userCannotAccessAnotherUsersPrivateSpeakingAudio() throws Exception {
		String ownerToken = registerStudent(uniqueEmail("speaking-owner"));
		String otherToken = registerStudent(uniqueEmail("speaking-other"));
		String uploadResponse = mockMvc.perform(multipart("/api/v1/student/files")
						.file(upload("file", "speaking.webm", "audio/webm", "audio"))
						.param("purpose", "speaking_audio")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Long fileId = objectMapper.readTree(uploadResponse).at("/data/id").asLong();

		mockMvc.perform(get("/api/v1/files/{fileId}/download-url", fileId)
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	@Test
	void ownerCanCreateDownloadUrlForPrivateSpeakingAudio() throws Exception {
		String ownerToken = registerStudent(uniqueEmail("speaking-url-owner"));
		String uploadResponse = mockMvc.perform(multipart("/api/v1/student/files")
						.file(upload("file", "speaking.webm", "audio/webm", "audio"))
						.param("purpose", "speaking_audio")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Long fileId = objectMapper.readTree(uploadResponse).at("/data/id").asLong();

		mockMvc.perform(get("/api/v1/files/{fileId}/download-url", fileId)
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fileId").value(fileId))
				.andExpect(jsonPath("$.data.url", startsWith("http://localhost:9000/")))
				.andExpect(jsonPath("$.data.expiresInSeconds").value(900))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	private MockMultipartFile upload(String partName, String fileName, String contentType, String content) {
		return new MockMultipartFile(partName, fileName, contentType, content.getBytes());
	}

	private String registerAdmin(String email) throws Exception {
		String accessToken = registerStudent(email);
		jdbcTemplate.update("""
				insert into user_roles (user_id, role_id)
				select u.id, r.id
				from users u
				join roles r on r.code = 'admin'
				where u.email = ?
				""", email);
		return accessToken;
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

	private String registerTeacher(String email) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/register/teacher")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s",
								  "fullName": "Teacher Example",
								  "displayName": "Teacher Example"
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

	private Long userIdFor(String email) {
		return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
	}

	private String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}
}
