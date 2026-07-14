package com.mockexams.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockexams.backend.entity.FileMetadata;
import com.mockexams.backend.entity.FilePurpose;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.repository.FileMetadataRepository;
import com.mockexams.backend.service.PlatformSettingService;

class RichTextServiceImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final FileMetadataRepository fileMetadataRepository = mock(FileMetadataRepository.class);
	private final PlatformSettingService platformSettingService = mock(PlatformSettingService.class);
	private final RichTextServiceImpl service = new RichTextServiceImpl(
			fileMetadataRepository,
			platformSettingService,
			objectMapper);

	@Test
	void rejectsDocumentsLargerThanConfiguredRichTextLimit() {
		when(platformSettingService.richTextMaxDocumentBytes()).thenReturn(1024);

		JsonNode document = documentWithText("x".repeat(1500));

		assertValidationFailed(() -> service.fromDocument(document, true, "Passage content"));
	}

	@Test
	void rejectsEmbedsWhoseFilePurposeIsNotAllowedBySetting() {
		when(platformSettingService.richTextMaxDocumentBytes()).thenReturn(65536);
		when(platformSettingService.richTextAllowedEmbedPurposes()).thenReturn(Set.of(FilePurpose.DIAGRAM));
		FileMetadata file = mock(FileMetadata.class);
		when(file.getId()).thenReturn(10L);
		when(file.getPurpose()).thenReturn(FilePurpose.PASSAGE_IMAGE);
		when(fileMetadataRepository.findAllById(any())).thenReturn(List.of(file));

		JsonNode document = imageDocument(10L);

		assertValidationFailed(() -> service.fromDocument(document, true, "Passage content"));
	}

	private JsonNode documentWithText(String text) {
		return objectMapper.createObjectNode()
				.put("version", 1)
				.put("type", "doc")
				.set("content", objectMapper.createArrayNode()
						.add(objectMapper.createObjectNode()
								.put("type", "paragraph")
								.set("content", objectMapper.createArrayNode()
										.add(objectMapper.createObjectNode()
												.put("type", "text")
												.put("text", text)))));
	}

	private JsonNode imageDocument(Long fileId) {
		return objectMapper.createObjectNode()
				.put("version", 1)
				.put("type", "doc")
				.set("content", objectMapper.createArrayNode()
						.add(objectMapper.createObjectNode()
								.put("type", "image")
								.set("attrs", objectMapper.createObjectNode()
										.put("fileId", fileId)
										.put("alt", "Diagram"))));
	}

	private void assertValidationFailed(ThrowingRunnable runnable) {
		assertThatThrownBy(runnable::run)
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("VALIDATION_FAILED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
				});
	}

	private interface ThrowingRunnable {

		void run();
	}
}
