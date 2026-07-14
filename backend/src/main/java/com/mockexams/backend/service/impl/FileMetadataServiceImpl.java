package com.mockexams.backend.service.impl;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.mockexams.backend.dto.FileDownloadUrlRDO;
import com.mockexams.backend.dto.FileRDO;
import com.mockexams.backend.entity.FileMetadata;
import com.mockexams.backend.entity.FilePurpose;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.FileMetadataMapper;
import com.mockexams.backend.repository.FileMetadataRepository;
import com.mockexams.backend.service.AccessControlService;
import com.mockexams.backend.service.FileMetadataService;
import com.mockexams.backend.service.FileStorageService;
import com.mockexams.backend.service.StoredFileObject;
import com.mockexams.backend.users.entity.User;

@Service
public class FileMetadataServiceImpl implements FileMetadataService {

	private static final int DOWNLOAD_URL_EXPIRES_SECONDS = 900;
	private static final long MIB = 1024L * 1024L;

	private static final Set<String> AUDIO_CONTENT_TYPES = Set.of(
			"audio/aac",
			"audio/m4a",
			"audio/mp3",
			"audio/mp4",
			"audio/mpeg",
			"audio/ogg",
			"audio/wav",
			"audio/webm",
			"audio/x-m4a",
			"audio/x-wav");

	private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/webp");

	private static final Set<String> DOCUMENT_CONTENT_TYPES = Set.of(
			"application/pdf",
			"application/msword",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document");

	private static final Map<FilePurpose, FileRule> RULES = Map.of(
			FilePurpose.LISTENING_AUDIO, new FileRule(AUDIO_CONTENT_TYPES, 100L * MIB),
			FilePurpose.SPEAKING_AUDIO, new FileRule(AUDIO_CONTENT_TYPES, 25L * MIB),
			FilePurpose.TEACHER_AVATAR, new FileRule(IMAGE_CONTENT_TYPES, 5L * MIB),
			FilePurpose.PASSAGE_IMAGE, new FileRule(IMAGE_CONTENT_TYPES, 10L * MIB),
			FilePurpose.DIAGRAM, new FileRule(IMAGE_CONTENT_TYPES, 10L * MIB),
			FilePurpose.DOCUMENT, new FileRule(DOCUMENT_CONTENT_TYPES, 25L * MIB),
			FilePurpose.COURSE_MATERIAL, new FileRule(union(DOCUMENT_CONTENT_TYPES, AUDIO_CONTENT_TYPES), 100L * MIB),
			FilePurpose.BANNER_IMAGE, new FileRule(IMAGE_CONTENT_TYPES, 8L * MIB));

	private static final Set<FilePurpose> TEACHER_UPLOAD_PURPOSES = Set.of(
			FilePurpose.LISTENING_AUDIO,
			FilePurpose.TEACHER_AVATAR,
			FilePurpose.PASSAGE_IMAGE,
			FilePurpose.DIAGRAM,
			FilePurpose.DOCUMENT,
			FilePurpose.COURSE_MATERIAL);

	private static final Set<FilePurpose> STUDENT_UPLOAD_PURPOSES = Set.of(FilePurpose.SPEAKING_AUDIO);
	private static final Set<FilePurpose> ADMIN_UPLOAD_PURPOSES = Set.of(FilePurpose.BANNER_IMAGE);

	private final FileMetadataRepository fileMetadataRepository;
	private final FileStorageService fileStorageService;
	private final FileMetadataMapper fileMetadataMapper;
	private final AccessControlService accessControlService;

	public FileMetadataServiceImpl(
			FileMetadataRepository fileMetadataRepository,
			FileStorageService fileStorageService,
			FileMetadataMapper fileMetadataMapper,
			AccessControlService accessControlService
	) {
		this.fileMetadataRepository = fileMetadataRepository;
		this.fileStorageService = fileStorageService;
		this.fileMetadataMapper = fileMetadataMapper;
		this.accessControlService = accessControlService;
	}

	@Override
	@Transactional
	public FileRDO uploadForTeacher(User owner, String purpose, MultipartFile file) {
		return upload(owner, FilePurpose.from(purpose), file, TEACHER_UPLOAD_PURPOSES);
	}

	@Override
	@Transactional
	public FileRDO uploadForStudent(User owner, String purpose, MultipartFile file) {
		return upload(owner, FilePurpose.from(purpose), file, STUDENT_UPLOAD_PURPOSES);
	}

	@Override
	@Transactional
	public FileRDO uploadForAdmin(User owner, String purpose, MultipartFile file) {
		return upload(owner, FilePurpose.from(purpose), file, ADMIN_UPLOAD_PURPOSES);
	}

	@Override
	@Transactional(readOnly = true)
	public FileDownloadUrlRDO createDownloadUrl(User requester, Long fileId) {
		FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
				.orElseThrow(() -> notFound());
		if (FilePurpose.SPEAKING_AUDIO.equals(fileMetadata.getPurpose()) && !isOwner(requester, fileMetadata)) {
			throw notFound();
		}
		accessControlService.requireFileDownloadAccess(requester, fileMetadata);
		return new FileDownloadUrlRDO(
				fileMetadata.getId(),
				fileStorageService.createDownloadUrl(fileMetadata),
				DOWNLOAD_URL_EXPIRES_SECONDS);
	}

	private FileRDO upload(User owner, FilePurpose purpose, MultipartFile file, Set<FilePurpose> allowedPurposes) {
		if (!allowedPurposes.contains(purpose)) {
			throw new BusinessException("VALIDATION_FAILED", "File purpose is not supported for this route.",
					HttpStatus.BAD_REQUEST);
		}
		validate(owner, purpose, file);

		String objectKey = objectKey(owner.getId(), purpose, file.getOriginalFilename());
		StoredFileObject storedFile = fileStorageService.store(objectKey, file);
		FileMetadata fileMetadata = new FileMetadata(
				owner,
				purpose,
				storedFile.bucket(),
				storedFile.objectKey(),
				originalName(file.getOriginalFilename()),
				normalizedContentType(file.getContentType()),
				file.getSize(),
				null,
				storedFile.checksum());
		return fileMetadataMapper.toRdo(fileMetadataRepository.save(fileMetadata));
	}

	private void validate(User owner, FilePurpose purpose, MultipartFile file) {
		if (owner == null || owner.getId() == null) {
			throw new BusinessException("AUTHENTICATION_REQUIRED", "Authenticated user is required.",
					HttpStatus.UNAUTHORIZED);
		}
		if (file == null || file.isEmpty() || file.getSize() <= 0) {
			throw new BusinessException("VALIDATION_FAILED", "Uploaded file is required.", HttpStatus.BAD_REQUEST);
		}
		FileRule rule = RULES.get(purpose);
		String contentType = normalizedContentType(file.getContentType());
		if (!rule.contentTypes().contains(contentType)) {
			throw new BusinessException("VALIDATION_FAILED", "File content type is not allowed for this purpose.",
					HttpStatus.BAD_REQUEST);
		}
		if (file.getSize() > rule.maxSizeBytes()) {
			throw new BusinessException("VALIDATION_FAILED", "File exceeds the maximum allowed size.",
					HttpStatus.BAD_REQUEST);
		}
	}

	private boolean isOwner(User requester, FileMetadata fileMetadata) {
		return requester != null
				&& requester.getId() != null
				&& fileMetadata.getOwner() != null
				&& requester.getId().equals(fileMetadata.getOwner().getId());
	}

	private String objectKey(Long ownerId, FilePurpose purpose, String originalFilename) {
		return purpose.value() + "/" + ownerId + "/" + UUID.randomUUID() + extension(originalFilename);
	}

	private String extension(String originalFilename) {
		String filename = originalName(originalFilename);
		int dot = filename.lastIndexOf('.');
		if (dot < 0 || dot == filename.length() - 1) {
			return "";
		}
		String extension = filename.substring(dot).toLowerCase();
		if (!extension.matches("\\.[a-z0-9]{1,12}")) {
			return "";
		}
		return extension;
	}

	private String originalName(String originalFilename) {
		String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
		if (cleaned.isBlank() || cleaned.contains("..")) {
			return "upload";
		}
		return cleaned.length() > 255 ? cleaned.substring(cleaned.length() - 255) : cleaned;
	}

	private String normalizedContentType(String contentType) {
		return contentType == null ? "" : contentType.trim().toLowerCase();
	}

	private BusinessException notFound() {
		return new BusinessException("NOT_FOUND", "File was not found.", HttpStatus.NOT_FOUND);
	}

	private static Set<String> union(Set<String> first, Set<String> second) {
		java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>(first);
		values.addAll(second);
		return Set.copyOf(values);
	}

	private record FileRule(Set<String> contentTypes, long maxSizeBytes) {
	}
}
