package com.mockexams.backend.users.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_roles")
public class UserRole {

	@EmbeddedId
	private UserRoleId id;

	@ManyToOne(optional = false)
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(optional = false)
	@MapsId("roleId")
	@JoinColumn(name = "role_id")
	private Role role;

	private OffsetDateTime assignedAt;

	protected UserRole() {
	}

	public UserRole(User user, Role role) {
		this.user = user;
		this.role = role;
		this.id = new UserRoleId(user.getId(), role.getId());
	}

	@PrePersist
	void prePersist() {
		if (assignedAt == null) {
			assignedAt = OffsetDateTime.now(ZoneOffset.UTC);
		}
	}

	public Role getRole() {
		return role;
	}
}
