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
@Table(name = "question_options")
public class QuestionOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	@Column(nullable = false, length = 20)
	private String optionLabel;

	@Column(nullable = false)
	private String optionText;

	@Column(columnDefinition = "text")
	private String optionDocument;

	@Column(columnDefinition = "text")
	private String optionRenderedHtml;

	@Column(nullable = false)
	private Integer optionOrder;

	protected QuestionOption() {
	}

	public static QuestionOption create(
			Question question,
			String optionLabel,
			String optionText,
			String optionDocument,
			String optionRenderedHtml,
			Integer optionOrder
	) {
		QuestionOption option = new QuestionOption();
		option.question = question;
		option.optionLabel = optionLabel;
		option.optionText = optionText;
		option.optionDocument = optionDocument;
		option.optionRenderedHtml = optionRenderedHtml;
		option.optionOrder = optionOrder;
		return option;
	}

	public Long getId() {
		return id;
	}

	public Question getQuestion() {
		return question;
	}

	public String getOptionLabel() {
		return optionLabel;
	}

	public String getOptionText() {
		return optionText;
	}

	public String getOptionDocument() {
		return optionDocument;
	}

	public String getOptionRenderedHtml() {
		return optionRenderedHtml;
	}

	public Integer getOptionOrder() {
		return optionOrder;
	}
}
