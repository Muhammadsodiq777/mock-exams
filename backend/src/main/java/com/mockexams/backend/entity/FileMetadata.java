package com.mockexams.backend.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.mockexams.backend.users.entity.User;

@Entity
@Table(name = "files")
public class FileMetadata {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_user_id")
	private User owner;

	@Column(nullable = false, length = 40)
	private FilePurpose purpose;

	@Column(nullable = false, length = 120)
	private String bucket;

	@Column(nullable = false, unique = true, length = 500)
	private String objectKey;

	@Column(nullable = false, length = 255)
	private String originalName;

	@Column(nullable = false, length = 120)
	private String contentType;

	@Column(nullable = false)
	private long sizeBytes;

	private Integer durationSeconds;

	@Column(length = 128)
	private String checksum;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	protected FileMetadata() {
	}

	public FileMetadata(
			User owner,
			FilePurpose purpose,
			String bucket,
			String objectKey,
			String originalName,
			String contentType,
			long sizeBytes,
			Integer durationSeconds,
			String checksum
	) {
		this.owner = owner;
		this.purpose = purpose;
		this.bucket = bucket;
		this.objectKey = objectKey;
		this.originalName = originalName;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.durationSeconds = durationSeconds;
		this.checksum = checksum;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now(ZoneOffset.UTC);
		}
	}

	public Long getId() {
		return id;
	}

	public User getOwner() {
		return owner;
	}

	public FilePurpose getPurpose() {
		return purpose;
	}

	public String getBucket() {
		return bucket;
	}

	public String getObjectKey() {
		return objectKey;
	}

	public String getOriginalName() {
		return originalName;
	}

	public String getContentType() {
		return contentType;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public Integer getDurationSeconds() {
		return durationSeconds;
	}

	public String getChecksum() {
		return checksum;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
