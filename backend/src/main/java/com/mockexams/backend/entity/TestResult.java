package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_results")
public class TestResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attempt_id", nullable = false)
	private TestAttempt attempt;

	@Column(nullable = false, precision = 8, scale = 2)
	private BigDecimal rawScore;

	@Column(nullable = false, precision = 8, scale = 2)
	private BigDecimal maxScore;

	@Column(precision = 2, scale = 1)
	private BigDecimal estimatedBand;

	@Column(nullable = false)
	private Integer correctCount = 0;

	@Column(nullable = false)
	private Integer incorrectCount = 0;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private Map<String, Object> weakQuestionTypes = new LinkedHashMap<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private Map<String, Object> summary = new LinkedHashMap<>();

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	protected TestResult() {
	}

	public static TestResult create(
			TestAttempt attempt,
			BigDecimal rawScore,
			BigDecimal maxScore,
			BigDecimal estimatedBand,
			Integer correctCount,
			Integer incorrectCount,
			Map<String, Object> weakQuestionTypes,
			Map<String, Object> summary
	) {
		TestResult result = new TestResult();
		result.attempt = attempt;
		result.rawScore = rawScore;
		result.maxScore = maxScore;
		result.estimatedBand = estimatedBand;
		result.correctCount = correctCount;
		result.incorrectCount = incorrectCount;
		result.weakQuestionTypes = copyMap(weakQuestionTypes);
		result.summary = copyMap(summary);
		return result;
	}

	private static Map<String, Object> copyMap(Map<String, Object> value) {
		return value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
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

	public TestAttempt getAttempt() {
		return attempt;
	}

	public BigDecimal getRawScore() {
		return rawScore;
	}

	public BigDecimal getMaxScore() {
		return maxScore;
	}

	public BigDecimal getEstimatedBand() {
		return estimatedBand;
	}

	public Integer getCorrectCount() {
		return correctCount;
	}

	public Integer getIncorrectCount() {
		return incorrectCount;
	}

	public Map<String, Object> getWeakQuestionTypes() {
		return weakQuestionTypes;
	}

	public Map<String, Object> getSummary() {
		return summary;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
