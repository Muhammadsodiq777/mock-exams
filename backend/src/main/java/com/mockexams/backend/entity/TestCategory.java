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
@Table(name = "test_categories")
public class TestCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private TestCategory parent;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 120)
	private String slug;

	@Column(nullable = false)
	private Integer sortOrder;

	@Column(name = "is_active", nullable = false)
	private Boolean active;

	protected TestCategory() {
	}

	public Long getId() {
		return id;
	}

	public TestCategory getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public String getSlug() {
		return slug;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public Boolean isActive() {
		return active;
	}
}
