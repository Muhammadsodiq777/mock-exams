package com.mockexams.backend.service.speaking;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.mockexams.backend.config.BackendEnvironmentProperties;
import com.mockexams.backend.service.AiUsageService;

public class OpenAiSpeakingFeedbackProvider implements SpeakingFeedbackProvider {

	private final BackendEnvironmentProperties properties;
	private final AiUsageService aiUsageService;
	private final RestClient restClient;

	public OpenAiSpeakingFeedbackProvider(
			BackendEnvironmentProperties properties,
			AiUsageService aiUsageService
	) {
		this.properties = properties;
		this.aiUsageService = aiUsageService;
		this.restClient = RestClient.builder()
				.baseUrl(properties.openai().baseUrl())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.openai().apiKey())
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	@Override
	public SpeakingFeedbackProviderResponse evaluate(SpeakingFeedbackPrompt prompt) {
		try {
			Map<String, Object> request = Map.of(
					"model", properties.openai().model(),
					"temperature", 0.2,
					"response_format", Map.of("type", "json_object"),
					"messages", List.of(
							Map.of("role", "system", "content", prompt.systemPrompt()),
							Map.of("role", "user", "content", prompt.userPrompt())));
			JsonNode response = restClient.post()
					.uri("/chat/completions")
					.body(request)
					.retrieve()
					.body(JsonNode.class);
			if (response == null) {
				throw new SpeakingFeedbackProviderException("OpenAI returned an empty response.");
			}
			String content = response.at("/choices/0/message/content").asText(null);
			if (content == null || content.isBlank()) {
				throw new SpeakingFeedbackProviderException("OpenAI returned no feedback content.");
			}
			int promptTokens = response.at("/usage/prompt_tokens").asInt(0);
			int completionTokens = response.at("/usage/completion_tokens").asInt(0);
			BigDecimal cost = aiUsageService.estimateCost(promptTokens, completionTokens);
			return new SpeakingFeedbackProviderResponse(
					"openai",
					response.path("model").asText(properties.openai().model()),
					content,
					promptTokens,
					completionTokens,
					cost);
		}
		catch (RestClientException ex) {
			throw new SpeakingFeedbackProviderException("OpenAI speaking feedback request failed.", ex);
		}
		catch (IllegalArgumentException ex) {
			throw new SpeakingFeedbackProviderException("OpenAI speaking feedback response could not be parsed.", ex);
		}
	}
}
