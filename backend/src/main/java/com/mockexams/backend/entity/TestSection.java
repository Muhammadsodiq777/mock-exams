package com.mockexams.backend.entity;

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
@Table(name = "test_sections")
public class TestSection {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "test_id", nullable = false)
	private CatalogTest test;

	@Column(nullable = false, length = 180)
	private String title;

	private String instructions;

	@Column(columnDefinition = "text")
	private String instructionsDocument;

	@Column(columnDefinition = "text")
	private String instructionsText;

	@Column(columnDefinition = "text")
	private String instructionsRenderedHtml;

	private String passageText;

	@Column(columnDefinition = "text")
	private String passageDocument;

	@Column(columnDefinition = "text")
	private String passageTextRenderedHtml;

	private Long audioFileId;

	@Column(nullable = false)
	private Integer sectionOrder;

	private Integer durationMinutes;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected TestSection() {
	}

	public static TestSection create(
			CatalogTest test,
			String title,
			String instructions,
			String instructionsDocument,
			String instructionsText,
			String instructionsRenderedHtml,
			String passageText,
			String passageDocument,
			String passageTextRenderedHtml,
			Long audioFileId,
			Integer sectionOrder,
			Integer durationMinutes
	) {
		TestSection section = new TestSection();
		section.test = test;
		section.title = title;
		section.instructions = instructions;
		section.instructionsDocument = instructionsDocument;
		section.instructionsText = instructionsText;
		section.instructionsRenderedHtml = instructionsRenderedHtml;
		section.passageText = passageText;
		section.passageDocument = passageDocument;
		section.passageTextRenderedHtml = passageTextRenderedHtml;
		section.audioFileId = audioFileId;
		section.sectionOrder = sectionOrder;
		section.durationMinutes = durationMinutes;
		return section;
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

	public CatalogTest getTest() {
		return test;
	}

	public String getTitle() {
		return title;
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

	public String getPassageText() {
		return passageText;
	}

	public String getPassageDocument() {
		return passageDocument;
	}

	public String getPassageTextRenderedHtml() {
		return passageTextRenderedHtml;
	}

	public Long getAudioFileId() {
		return audioFileId;
	}

	public Integer getSectionOrder() {
		return sectionOrder;
	}

	public Integer getDurationMinutes() {
		return durationMinutes;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void update(
			String title,
			String instructions,
			String instructionsDocument,
			String instructionsText,
			String instructionsRenderedHtml,
			String passageText,
			String passageDocument,
			String passageTextRenderedHtml,
			Long audioFileId,
			Integer durationMinutes
	) {
		this.title = title;
		this.instructions = instructions;
		this.instructionsDocument = instructionsDocument;
		this.instructionsText = instructionsText;
		this.instructionsRenderedHtml = instructionsRenderedHtml;
		this.passageText = passageText;
		this.passageDocument = passageDocument;
		this.passageTextRenderedHtml = passageTextRenderedHtml;
		this.audioFileId = audioFileId;
		this.durationMinutes = durationMinutes;
	}

	public void setSectionOrder(Integer sectionOrder) {
		this.sectionOrder = sectionOrder;
	}
}
