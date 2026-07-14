package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
@Table(name = "questions")
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "section_id", nullable = false)
	private TestSection section;

	@Column(nullable = false, length = 40)
	private String questionType;

	@Column(nullable = false)
	private String questionText;

	@Column(columnDefinition = "text")
	private String questionDocument;

	@Column(columnDefinition = "text")
	private String questionRenderedHtml;

	private String instructions;

	@Column(columnDefinition = "text")
	private String instructionsDocument;

	@Column(columnDefinition = "text")
	private String instructionsText;

	@Column(columnDefinition = "text")
	private String instructionsRenderedHtml;

	@Column(nullable = false, precision = 6, scale = 2)
	private BigDecimal score;

	@Column(nullable = false)
	private Integer questionOrder;

	@Column(length = 20)
	private String relatedParagraphLabel;

	private Long listeningSegmentId;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected Question() {
	}

	public static Question create(
			TestSection section,
			String questionType,
			String questionText,
			String questionDocument,
			String questionRenderedHtml,
			String instructions,
			String instructionsDocument,
			String instructionsText,
			String instructionsRenderedHtml,
			BigDecimal score,
			Integer questionOrder,
			String relatedParagraphLabel,
			Long listeningSegmentId
	) {
		Question question = new Question();
		question.section = section;
		question.questionType = questionType;
		question.questionText = questionText;
		question.questionDocument = questionDocument;
		question.questionRenderedHtml = questionRenderedHtml;
		question.instructions = instructions;
		question.instructionsDocument = instructionsDocument;
		question.instructionsText = instructionsText;
		question.instructionsRenderedHtml = instructionsRenderedHtml;
		question.score = score;
		question.questionOrder = questionOrder;
		question.relatedParagraphLabel = relatedParagraphLabel;
		question.listeningSegmentId = listeningSegmentId;
		return question;
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

	public Long getId() {
		return id;
	}

	public TestSection getSection() {
		return section;
	}

	public String getQuestionType() {
		return questionType;
	}

	public String getQuestionText() {
		return questionText;
	}

	public String getQuestionDocument() {
		return questionDocument;
	}

	public String getQuestionRenderedHtml() {
		return questionRenderedHtml;
	}

	public String getInstructions() {
		return instructions;
	}

	public String getInstructionsDocument() {
		return instructionsDocument;
	}

	public String getInstructionsText() {
		return instructionsText;
	}

	public String getInstructionsRenderedHtml() {
		return instructionsRenderedHtml;
	}

	public BigDecimal getScore() {
		return score;
	}

	public Integer getQuestionOrder() {
		return questionOrder;
	}

	public String getRelatedParagraphLabel() {
		return relatedParagraphLabel;
	}

	public Long getListeningSegmentId() {
		return listeningSegmentId;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void update(
			String questionType,
			String questionText,
			String questionDocument,
			String questionRenderedHtml,
			String instructions,
			String instructionsDocument,
			String instructionsText,
			String instructionsRenderedHtml,
			BigDecimal score,
			String relatedParagraphLabel,
			Long listeningSegmentId
	) {
		this.questionType = questionType;
		this.questionText = questionText;
		this.questionDocument = questionDocument;
		this.questionRenderedHtml = questionRenderedHtml;
		this.instructions = instructions;
		this.instructionsDocument = instructionsDocument;
		this.instructionsText = instructionsText;
		this.instructionsRenderedHtml = instructionsRenderedHtml;
		this.score = score;
		this.relatedParagraphLabel = relatedParagraphLabel;
		this.listeningSegmentId = listeningSegmentId;
	}

	public void setRelatedParagraphLabel(String relatedParagraphLabel) {
		this.relatedParagraphLabel = relatedParagraphLabel;
	}

	public void setQuestionOrder(Integer questionOrder) {
		this.questionOrder = questionOrder;
	}
}
