package com.mockexams.backend.service;

import org.springframework.web.multipart.MultipartFile;

import com.mockexams.backend.dto.FileDownloadUrlRDO;
import com.mockexams.backend.dto.FileRDO;
import com.mockexams.backend.users.entity.User;

public interface FileMetadataService {

	FileRDO uploadForTeacher(User owner, String purpose, MultipartFile file);

	FileRDO uploadForStudent(User owner, String purpose, MultipartFile file);

	FileRDO uploadForAdmin(User owner, String purpose, MultipartFile file);

	FileDownloadUrlRDO createDownloadUrl(User requester, Long fileId);
}
