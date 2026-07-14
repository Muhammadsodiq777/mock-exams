package com.mockexams.backend.service.impl;

import java.io.InputStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mockexams.backend.config.BackendEnvironmentProperties;
import com.mockexams.backend.entity.FileMetadata;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.service.FileStorageService;
import com.mockexams.backend.service.StoredFileContent;
import com.mockexams.backend.service.StoredFileObject;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;

@Service
class MinioFileStorageService implements FileStorageService {

	private static final int DOWNLOAD_URL_EXPIRES_SECONDS = 900;

	private final MinioClient minioClient;
	private final String bucket;

	MinioFileStorageService(MinioClient minioClient, BackendEnvironmentProperties properties) {
		this.minioClient = minioClient;
		this.bucket = properties.minio().bucket();
	}

	@Override
	public StoredFileObject store(String objectKey, MultipartFile file) {
		try (InputStream inputStream = file.getInputStream()) {
			ObjectWriteResponse response = minioClient.putObject(PutObjectArgs.builder()
					.bucket(bucket)
					.object(objectKey)
					.stream(inputStream, file.getSize(), -1L)
					.contentType(file.getContentType())
					.build());
			return new StoredFileObject(bucket, objectKey, response.checksumSHA256());
		}
		catch (Exception ex) {
			throw new BusinessException(
					"SERVICE_UNAVAILABLE",
					"File storage is unavailable.",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	@Override
	public StoredFileContent read(FileMetadata fileMetadata) {
		try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
				.bucket(fileMetadata.getBucket())
				.object(fileMetadata.getObjectKey())
				.build())) {
			return new StoredFileContent(
					fileMetadata.getOriginalName(),
					fileMetadata.getContentType(),
					inputStream.readAllBytes());
		}
		catch (Exception ex) {
			throw new BusinessException(
					"SERVICE_UNAVAILABLE",
					"File storage is unavailable.",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	@Override
	public String createDownloadUrl(FileMetadata fileMetadata) {
		try {
			return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
					.method(Http.Method.GET)
					.bucket(fileMetadata.getBucket())
					.object(fileMetadata.getObjectKey())
					.expiry(DOWNLOAD_URL_EXPIRES_SECONDS)
					.build());
		}
		catch (Exception ex) {
			throw new BusinessException(
					"SERVICE_UNAVAILABLE",
					"File storage is unavailable.",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
	}
}
