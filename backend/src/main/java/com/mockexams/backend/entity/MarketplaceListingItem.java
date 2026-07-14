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
@Table(name = "marketplace_listing_items")
public class MarketplaceListingItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "listing_id")
	private MarketplaceListing listing;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "test_id")
	private CatalogTest test;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "file_id")
	private FileMetadata file;

	@Column(length = 180)
	private String title;

	private String description;

	@Column(nullable = false)
	private Integer itemOrder;

	protected MarketplaceListingItem() {
	}

	public MarketplaceListingItem(
			MarketplaceListing listing,
			CatalogTest test,
			FileMetadata file,
			String title,
			String description,
			Integer itemOrder
	) {
		this.listing = listing;
		update(test, file, title, description, itemOrder);
	}

	public Long getId() {
		return id;
	}

	public MarketplaceListing getListing() {
		return listing;
	}

	public CatalogTest getTest() {
		return test;
	}

	public Long getTestId() {
		return test == null ? null : test.getId();
	}

	public FileMetadata getFile() {
		return file;
	}

	public Long getFileId() {
		return file == null ? null : file.getId();
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public Integer getItemOrder() {
		return itemOrder;
	}

	public void update(CatalogTest test, FileMetadata file, String title, String description, Integer itemOrder) {
		this.test = test;
		this.file = file;
		this.title = title;
		this.description = description;
		this.itemOrder = itemOrder;
	}
}
