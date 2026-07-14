package com.mockexams.backend.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_answers")
public class StudentAnswer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attempt_id", nullable = false)
	private TestAttempt attempt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	@Column(columnDefinition = "text")
	private String answerText;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "selected_option_id")
	private QuestionOption selectedOption;

	private Long audioFileId;

	@Column(name = "is_correct")
	private Boolean correct;

	private java.math.BigDecimal scoreAwarded;

	private OffsetDateTime answeredAt;

	protected StudentAnswer() {
	}

	public static StudentAnswer create(
			TestAttempt attempt,
			Question question,
			String answerText,
			QuestionOption selectedOption,
			Long audioFileId,
			OffsetDateTime answeredAt
	) {
		StudentAnswer answer = new StudentAnswer();
		answer.attempt = attempt;
		answer.question = question;
		answer.update(answerText, selectedOption, audioFileId, answeredAt);
		return answer;
	}

	public void update(
			String answerText,
			QuestionOption selectedOption,
			Long audioFileId,
			OffsetDateTime answeredAt
	) {
		this.answerText = answerText;
		this.selectedOption = selectedOption;
		this.audioFileId = audioFileId;
		this.answeredAt = answeredAt;
		this.correct = null;
		this.scoreAwarded = null;
	}

	public void markScored(Boolean correct, java.math.BigDecimal scoreAwarded) {
		this.correct = correct;
		this.scoreAwarded = scoreAwarded;
	}

	public Long getId() {
		return id;
	}

	public TestAttempt getAttempt() {
		return attempt;
	}

	public Question getQuestion() {
		return question;
	}

	public String getAnswerText() {
		return answerText;
	}

	public QuestionOption getSelectedOption() {
		return selectedOption;
	}

	public Long getAudioFileId() {
		return audioFileId;
	}

	public Boolean getCorrect() {
		return correct;
	}

	public java.math.BigDecimal getScoreAwarded() {
		return scoreAwarded;
	}

	public OffsetDateTime getAnsweredAt() {
		return answeredAt;
	}
}
