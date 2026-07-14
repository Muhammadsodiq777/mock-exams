package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public final class TeacherQuestionUDO {

	@Size(max = 40)
	private String questionType;

	private String questionText;

	private JsonNode questionDocument;

	private String instructions;

	private JsonNode instructionsDocument;

	@DecimalMin("0.00")
	private BigDecimal score;

	@Size(max = 20)
	private String relatedParagraphLabel;

	private Long listeningSegmentId;

	@JsonIgnore
	private boolean listeningSegmentIdProvided;

	private List<@Valid TeacherQuestionOptionCDO> options;

	private List<@Valid TeacherQuestionAnswerCDO> answers;

	public String questionType() {
		return questionType;
	}

	public String questionText() {
		return questionText;
	}

	public JsonNode questionDocument() {
		return questionDocument;
	}

	public String instructions() {
		return instructions;
	}

	public JsonNode instructionsDocument() {
		return instructionsDocument;
	}

	public BigDecimal score() {
		return score;
	}

	public String relatedParagraphLabel() {
		return relatedParagraphLabel;
	}

	public Long listeningSegmentId() {
		return listeningSegmentId;
	}

	@JsonIgnore
	public boolean hasListeningSegmentId() {
		return listeningSegmentIdProvided;
	}

	public List<TeacherQuestionOptionCDO> options() {
		return options;
	}

	public List<TeacherQuestionAnswerCDO> answers() {
		return answers;
	}

	public void setQuestionType(String questionType) {
		this.questionType = questionType;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	public void setQuestionDocument(JsonNode questionDocument) {
		this.questionDocument = questionDocument;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	public void setInstructionsDocument(JsonNode instructionsDocument) {
		this.instructionsDocument = instructionsDocument;
	}

	public void setScore(BigDecimal score) {
		this.score = score;
	}

	public void setRelatedParagraphLabel(String relatedParagraphLabel) {
		this.relatedParagraphLabel = relatedParagraphLabel;
	}

	@JsonSetter("listeningSegmentId")
	public void setListeningSegmentId(Long listeningSegmentId) {
		this.listeningSegmentId = listeningSegmentId;
		this.listeningSegmentIdProvided = true;
	}

	public void setOptions(List<TeacherQuestionOptionCDO> options) {
		this.options = options;
	}

	public void setAnswers(List<TeacherQuestionAnswerCDO> answers) {
		this.answers = answers;
	}
}
