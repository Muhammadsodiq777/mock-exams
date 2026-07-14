package com.mockexams.backend.entity;

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
@Table(name = "question_answers")
public class QuestionAnswer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	@Column(nullable = false)
	private String answerValue;

	@Column(columnDefinition = "text")
	private String normalizedValue;

	@Column(name = "is_primary", nullable = false)
	private Boolean primary;

	@Column(nullable = false)
	private Integer answerOrder;

	@Column(columnDefinition = "text")
	private String explanation;

	@Column(columnDefinition = "text")
	private String explanationDocument;

	@Column(columnDefinition = "text")
	private String explanationRenderedHtml;

	protected QuestionAnswer() {
	}

	public static QuestionAnswer create(
			Question question,
			String answerValue,
			String normalizedValue,
			Boolean primary,
			Integer answerOrder,
			String explanation,
			String explanationDocument,
			String explanationRenderedHtml
	) {
		QuestionAnswer answer = new QuestionAnswer();
		answer.question = question;
		answer.answerValue = answerValue;
		answer.normalizedValue = normalizedValue;
		answer.primary = primary;
		answer.answerOrder = answerOrder;
		answer.explanation = explanation;
		answer.explanationDocument = explanationDocument;
		answer.explanationRenderedHtml = explanationRenderedHtml;
		return answer;
	}

	public Long getId() {
		return id;
	}

	public Question getQuestion() {
		return question;
	}

	public String getAnswerValue() {
		return answerValue;
	}

	public String getNormalizedValue() {
		return normalizedValue;
	}

	public Boolean isPrimary() {
		return primary;
	}

	public Integer getAnswerOrder() {
		return answerOrder;
	}

	public String getExplanation() {
		return explanation;
	}

	public String getExplanationDocument() {
		return explanationDocument;
	}

	public String getExplanationRenderedHtml() {
		return explanationRenderedHtml;
	}
}
