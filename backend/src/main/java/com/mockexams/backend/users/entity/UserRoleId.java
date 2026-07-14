package com.mockexams.backend.users.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserRoleId implements Serializable {

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "role_id")
	private Short roleId;

	protected UserRoleId() {
	}

	UserRoleId(Long userId, Short roleId) {
		this.userId = userId;
		this.roleId = roleId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UserRoleId that)) {
			return false;
		}
		return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, roleId);
	}
}
