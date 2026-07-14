package com.mockexams.backend.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.mockexams.backend.users.entity.User;
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

@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false, length = 32)
	private String type;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(nullable = false)
	private String body;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private Map<String, Object> payload;

	private OffsetDateTime readAt;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	protected Notification() {
	}

	public Notification(User user, String type, String title, String body, Map<String, Object> payload) {
		this.user = user;
		this.type = type;
		this.title = title;
		this.body = body;
		this.payload = copyPayload(payload);
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

	public User getUser() {
		return user;
	}

	public String getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

	public Map<String, Object> getPayload() {
		return payload;
	}

	public OffsetDateTime getReadAt() {
		return readAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public boolean markRead(OffsetDateTime readAt) {
		if (this.readAt != null) {
			return false;
		}
		this.readAt = readAt;
		return true;
	}

	private Map<String, Object> copyPayload(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return Map.of();
		}
		return new LinkedHashMap<>(payload);
	}
}
