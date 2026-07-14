package com.mockexams.backend.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "platform_settings")
public class PlatformSetting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120, unique = true)
	private String settingKey;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String settingValue;

	private Long updatedByUserId;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected PlatformSetting() {
	}

	public PlatformSetting(String settingKey, String settingValue) {
		this.settingKey = settingKey;
		this.settingValue = settingValue;
		this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public void updateValue(String settingValue, Long updatedByUserId, OffsetDateTime updatedAt) {
		this.settingValue = settingValue;
		this.updatedByUserId = updatedByUserId;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public String getSettingKey() {
		return settingKey;
	}

	public String getSettingValue() {
		return settingValue;
	}

	public Long getUpdatedByUserId() {
		return updatedByUserId;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
