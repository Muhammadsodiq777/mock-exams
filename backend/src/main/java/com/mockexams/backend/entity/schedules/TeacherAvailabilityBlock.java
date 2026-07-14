package com.mockexams.backend.entity.schedules;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.mockexams.backend.teachers.entity.TeacherProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher_availability_blocks")
public class TeacherAvailabilityBlock {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_profile_id")
	private TeacherProfile teacherProfile;

	@Column(nullable = false)
	private Integer weekday;

	@Column(nullable = false)
	private LocalTime startsAt;

	@Column(nullable = false)
	private LocalTime endsAt;

	@Column(nullable = false, length = 80)
	private String timezone;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected TeacherAvailabilityBlock() {
	}

	@PrePersist
	void prePersist() {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}
}
