package com.mockexams.backend.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class FilePurposeConverter implements AttributeConverter<FilePurpose, String> {

	@Override
	public String convertToDatabaseColumn(FilePurpose attribute) {
		return attribute == null ? null : attribute.value();
	}

	@Override
	public FilePurpose convertToEntityAttribute(String dbData) {
		return dbData == null ? null : FilePurpose.from(dbData);
	}
}
