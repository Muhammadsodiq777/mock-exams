package com.mockexams.backend.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
@Table(name = "writing_submissions")
public class WritingSubmission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_user_id", nullable = false)
	private User studentUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "test_attempt_id")
	private TestAttempt testAttempt;

	@Column(nullable = false, length = 32)
	private String taskType;

	@Column(nullable = false, columnDefinition = "text")
	private String prompt;

	@Column(nullable = false, columnDefinition = "text")
	private String answerText;

	private Integer wordCount;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	protected WritingSubmission() {
	}

	public static WritingSubmission create(
			User studentUser,
			TestAttempt testAttempt,
			String taskType,
			String prompt,
			String answerText,
			Integer wordCount
	) {
		WritingSubmission submission = new WritingSubmission();
		submission.studentUser = studentUser;
		submission.testAttempt = testAttempt;
		submission.taskType = taskType;
		submission.prompt = prompt;
		submission.answerText = answerText;
		submission.wordCount = wordCount;
		return submission;
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

	public User getStudentUser() {
		return studentUser;
	}

	public TestAttempt getTestAttempt() {
		return testAttempt;
	}

	public String getTaskType() {
		return taskType;
	}

	public String getPrompt() {
		return prompt;
	}

	public String getAnswerText() {
		return answerText;
	}

	public Integer getWordCount() {
		return wordCount;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
