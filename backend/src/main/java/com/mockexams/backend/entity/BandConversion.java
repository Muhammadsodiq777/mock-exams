package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "band_conversions")
public class BandConversion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 32)
	private String testType;

	@Column(nullable = false, precision = 8, scale = 2)
	private BigDecimal minRawScore;

	@Column(nullable = false, precision = 8, scale = 2)
	private BigDecimal maxRawScore;

	@Column(nullable = false, precision = 2, scale = 1)
	private BigDecimal estimatedBand;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	protected BandConversion() {
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

	public String getTestType() {
		return testType;
	}

	public BigDecimal getMinRawScore() {
		return minRawScore;
	}

	public BigDecimal getMaxRawScore() {
		return maxRawScore;
	}

	public BigDecimal getEstimatedBand() {
		return estimatedBand;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
