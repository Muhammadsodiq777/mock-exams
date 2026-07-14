package com.mockexams.backend.service;

import org.springframework.web.multipart.MultipartFile;

import com.mockexams.backend.entity.FileMetadata;

public interface FileStorageService {

	StoredFileObject store(String objectKey, MultipartFile file);

	StoredFileContent read(FileMetadata fileMetadata);

	String createDownloadUrl(FileMetadata fileMetadata);
}
