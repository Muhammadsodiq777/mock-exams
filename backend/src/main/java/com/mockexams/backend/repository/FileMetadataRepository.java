package com.mockexams.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.FileMetadata;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

	Optional<FileMetadata> findByIdAndOwner_Id(Long id, Long ownerId);
}
