package com.mockexams.backend.dto;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.JsonNode;

public record PlatformSettingUDO(
		@NotNull JsonNode value
) {
}
