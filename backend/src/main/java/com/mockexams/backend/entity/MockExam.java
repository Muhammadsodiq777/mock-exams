package com.mockexams.backend.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "mock_exam")
public class MockExam {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(nullable = false, length = 120)
	private String subject;

	@Column(nullable = false)
	private Integer durationMinutes;

	@Column(nullable = false)
	private Integer questionCount;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	protected MockExam() {
	}

	public MockExam(String title, String subject, Integer durationMinutes, Integer questionCount) {
		this.title = title;
		this.subject = subject;
		this.durationMinutes = durationMinutes;
		this.questionCount = questionCount;
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

	public String getTitle() {
		return title;
	}

	public String getSubject() {
		return subject;
	}

	public Integer getDurationMinutes() {
		return durationMinutes;
	}

	public Integer getQuestionCount() {
		return questionCount;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
