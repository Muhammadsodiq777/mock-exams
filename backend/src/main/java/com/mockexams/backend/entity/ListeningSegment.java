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
@Table(name = "listening_segments")
public class ListeningSegment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "section_id", nullable = false)
	private TestSection section;

	@Column(length = 160)
	private String title;

	@Column(nullable = false)
	private Integer startSeconds;

	@Column(nullable = false)
	private Integer endSeconds;

	@Column(columnDefinition = "text")
	private String transcript;

	@Column(nullable = false)
	private Integer segmentOrder;

	protected ListeningSegment() {
	}

	public static ListeningSegment create(
			TestSection section,
			String title,
			Integer startSeconds,
			Integer endSeconds,
			String transcript,
			Integer segmentOrder
	) {
		ListeningSegment segment = new ListeningSegment();
		segment.section = section;
		segment.title = title;
		segment.startSeconds = startSeconds;
		segment.endSeconds = endSeconds;
		segment.transcript = transcript;
		segment.segmentOrder = segmentOrder;
		return segment;
	}

	public Long getId() {
		return id;
	}

	public TestSection getSection() {
		return section;
	}

	public String getTitle() {
		return title;
	}

	public Integer getStartSeconds() {
		return startSeconds;
	}

	public Integer getEndSeconds() {
		return endSeconds;
	}

	public String getTranscript() {
		return transcript;
	}

	public Integer getSegmentOrder() {
		return segmentOrder;
	}

	public void update(String title, Integer startSeconds, Integer endSeconds, String transcript) {
		this.title = title;
		this.startSeconds = startSeconds;
		this.endSeconds = endSeconds;
		this.transcript = transcript;
	}

	public void setSegmentOrder(Integer segmentOrder) {
		this.segmentOrder = segmentOrder;
	}
}
