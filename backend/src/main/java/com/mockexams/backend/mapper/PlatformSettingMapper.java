package com.mockexams.backend.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.PlatformSettingRDO;
import com.mockexams.backend.entity.PlatformSetting;

@Component
public class PlatformSettingMapper {

	private final ObjectMapper objectMapper;

	public PlatformSettingMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public PlatformSettingRDO toRDO(PlatformSetting setting, JsonNode fallbackValue) {
		JsonNode value = setting == null ? fallbackValue : parse(setting.getSettingValue());
		return new PlatformSettingRDO(
				setting == null ? null : setting.getSettingKey(),
				value,
				setting == null ? null : setting.getUpdatedByUserId(),
				setting == null ? null : setting.getUpdatedAt());
	}

	public PlatformSettingRDO toRDO(String key, JsonNode value) {
		return new PlatformSettingRDO(key, value, null, null);
	}

	public PlatformSettingRDO toRDO(PlatformSetting setting) {
		return toRDO(setting, null);
	}

	private JsonNode parse(String value) {
		try {
			JsonNode root = objectMapper.readTree(value);
			if (root.isTextual()) {
				return objectMapper.readTree(root.asText());
			}
			return root;
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Stored platform setting JSON is invalid.", ex);
		}
	}
}
