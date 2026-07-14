package com.mockexams.backend.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.FileRDO;
import com.mockexams.backend.entity.FileMetadata;

@Component
public class FileMetadataMapper {

	public FileRDO toRdo(FileMetadata fileMetadata) {
		Long ownerUserId = fileMetadata.getOwner() == null ? null : fileMetadata.getOwner().getId();
		return new FileRDO(
				fileMetadata.getId(),
				ownerUserId,
				fileMetadata.getPurpose().value(),
				fileMetadata.getOriginalName(),
				fileMetadata.getContentType(),
				fileMetadata.getSizeBytes(),
				fileMetadata.getDurationSeconds(),
				fileMetadata.getChecksum(),
				fileMetadata.getCreatedAt());
	}
}
