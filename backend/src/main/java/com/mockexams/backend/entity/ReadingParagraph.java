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
@Table(name = "reading_paragraphs")
public class ReadingParagraph {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "section_id", nullable = false)
	private TestSection section;

	@Column(length = 20)
	private String label;

	@Column(nullable = false)
	private String content;

	@Column(columnDefinition = "text")
	private String contentDocument;

	@Column(columnDefinition = "text")
	private String contentRenderedHtml;

	@Column(nullable = false)
	private Integer paragraphOrder;

	protected ReadingParagraph() {
	}

	public static ReadingParagraph create(
			TestSection section,
			String label,
			String content,
			String contentDocument,
			String contentRenderedHtml,
			Integer paragraphOrder
	) {
		ReadingParagraph paragraph = new ReadingParagraph();
		paragraph.section = section;
		paragraph.label = label;
		paragraph.content = content;
		paragraph.contentDocument = contentDocument;
		paragraph.contentRenderedHtml = contentRenderedHtml;
		paragraph.paragraphOrder = paragraphOrder;
		return paragraph;
	}

	public Long getId() {
		return id;
	}

	public TestSection getSection() {
		return section;
	}

	public String getLabel() {
		return label;
	}

	public String getContent() {
		return content;
	}

	public String getContentDocument() {
		return contentDocument;
	}

	public String getContentRenderedHtml() {
		return contentRenderedHtml;
	}

	public Integer getParagraphOrder() {
		return paragraphOrder;
	}

	public void update(String label, String content, String contentDocument, String contentRenderedHtml) {
		this.label = label;
		this.content = content;
		this.contentDocument = contentDocument;
		this.contentRenderedHtml = contentRenderedHtml;
	}

	public void setParagraphOrder(Integer paragraphOrder) {
		this.paragraphOrder = paragraphOrder;
	}
}
