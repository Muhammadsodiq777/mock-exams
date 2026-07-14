package com.mockexams.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.mockexams.backend.entity.FileMetadata;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.FileMetadataMapper;
import com.mockexams.backend.repository.FileMetadataRepository;
import com.mockexams.backend.service.AccessControlService;
import com.mockexams.backend.service.impl.FileMetadataServiceImpl;
import com.mockexams.backend.users.entity.User;

@ExtendWith(MockitoExtension.class)
class FileMetadataServiceTest {

	@Mock
	private FileMetadataRepository fileMetadataRepository;

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private AccessControlService accessControlService;

	private FileMetadataService fileMetadataService;

	@BeforeEach
	void setUp() {
		fileMetadataService = new FileMetadataServiceImpl(
				fileMetadataRepository,
				fileStorageService,
				new FileMetadataMapper(),
				accessControlService);
	}

	@Test
	void invalidContentTypeIsRejectedBeforeStorage() {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"listening.txt",
				"text/plain",
				"not audio".getBytes());

		assertThatThrownBy(() -> fileMetadataService.uploadForTeacher(owner(42L), "listening_audio", file))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("VALIDATION_FAILED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
				});

		verifyNoInteractions(fileStorageService, fileMetadataRepository);
	}

	@Test
	void oversizedFileIsRejectedBeforeStorage() {
		MultipartFile file = mock(MultipartFile.class);
		when(file.isEmpty()).thenReturn(false);
		when(file.getSize()).thenReturn(26L * 1024L * 1024L);
		when(file.getContentType()).thenReturn("audio/mpeg");

		assertThatThrownBy(() -> fileMetadataService.uploadForStudent(owner(42L), "speaking_audio", file))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo("VALIDATION_FAILED");
					assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
				});

		verifyNoInteractions(fileStorageService, fileMetadataRepository);
	}

	@Test
	void metadataIsNotSavedWhenStorageFails() {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"listening.mp3",
				"audio/mpeg",
				"audio".getBytes());
		when(fileStorageService.store(anyString(), any(MultipartFile.class)))
				.thenThrow(new BusinessException("SERVICE_UNAVAILABLE", "File storage is unavailable.",
						HttpStatus.SERVICE_UNAVAILABLE));

		assertThatThrownBy(() -> fileMetadataService.uploadForTeacher(owner(42L), "listening_audio", file))
				.isInstanceOfSatisfying(BusinessException.class,
						ex -> assertThat(ex.getCode()).isEqualTo("SERVICE_UNAVAILABLE"));

		verify(fileMetadataRepository, never()).save(any(FileMetadata.class));
	}

	@Test
	void metadataIsSavedAfterStorageSucceeds() {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"listening.mp3",
				"audio/mpeg",
				"audio".getBytes());
		when(fileStorageService.store(anyString(), any(MultipartFile.class)))
				.thenAnswer(invocation -> new StoredFileObject("mock-exams-test", invocation.getArgument(0), "etag"));
		when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

		fileMetadataService.uploadForTeacher(owner(42L), "listening_audio", file);

		InOrder inOrder = inOrder(fileStorageService, fileMetadataRepository);
		inOrder.verify(fileStorageService).store(anyString(), any(MultipartFile.class));
		inOrder.verify(fileMetadataRepository).save(any(FileMetadata.class));
	}

	private User owner(Long userId) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(userId);
		return user;
	}
}
