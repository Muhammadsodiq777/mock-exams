package com.mockexams.backend.users.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.UserStatuses;

@Entity
@Table(name = "users")
public class User {

	public static final String STATUS_ACTIVE = UserStatuses.ACTIVE;
	public static final String STATUS_SUSPENDED = UserStatuses.SUSPENDED;
	public static final String STATUS_BANNED = UserStatuses.BANNED;
	public static final String STATUS_DELETED = UserStatuses.DELETED;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false, length = 160)
	private String fullName;

	@Column(length = 40)
	private String phone;

	@Column(nullable = false, length = 24)
	private String status = STATUS_ACTIVE;

	@Column(nullable = false, length = 20)
	private String preferredLanguage = "en";

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	private OffsetDateTime deletedAt;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<UserRole> userRoles = new LinkedHashSet<>();

	protected User() {
	}

	public User(String email, String passwordHash, String fullName, String phone, String preferredLanguage) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.phone = phone;
		if (preferredLanguage != null && !preferredLanguage.isBlank()) {
			this.preferredLanguage = preferredLanguage;
		}
	}

	@PrePersist
	void prePersist() {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public boolean canLogin() {
		return STATUS_ACTIVE.equals(status) && deletedAt == null;
	}

	public boolean isActiveAdmin() {
		return STATUS_ACTIVE.equals(status) && hasRole(RoleCodes.ADMIN);
	}

	public boolean hasRole(String roleCode) {
		return userRoles.stream().anyMatch(userRole -> userRole.getRole().getCode().equals(roleCode));
	}

	public void suspend() {
		transitionTo(UserStatuses.SUSPENDED);
	}

	public void ban() {
		transitionTo(UserStatuses.BANNED);
	}

	public void reactivate() {
		transitionTo(UserStatuses.ACTIVE);
	}

	public void softDelete() {
		transitionTo(UserStatuses.DELETED);
	}

	private void transitionTo(String status) {
		this.status = status;
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (STATUS_DELETED.equals(status)) {
			if (deletedAt == null) {
				deletedAt = now;
			}
		}
		else {
			deletedAt = null;
		}
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getFullName() {
		return fullName;
	}

	public String getPhone() {
		return phone;
	}

	public String getStatus() {
		return status;
	}

	public String getPreferredLanguage() {
		return preferredLanguage;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public OffsetDateTime getDeletedAt() {
		return deletedAt;
	}

	public Set<UserRole> getUserRoles() {
		return userRoles;
	}
}
