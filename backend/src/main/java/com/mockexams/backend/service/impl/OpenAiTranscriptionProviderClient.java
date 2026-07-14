package com.mockexams.backend.service.impl;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.mockexams.backend.config.BackendEnvironmentProperties;
import com.mockexams.backend.entity.AiFailureCodes;
import com.mockexams.backend.service.TranscriptionProviderClient;
import com.mockexams.backend.service.TranscriptionProviderException;
import com.mockexams.backend.service.TranscriptionRequest;
import com.mockexams.backend.service.TranscriptionResult;

@Service
@ConditionalOnProperty(prefix = "app.transcription", name = "provider", havingValue = "openai")
class OpenAiTranscriptionProviderClient implements TranscriptionProviderClient {

	private final RestClient restClient;
	private final String model;

	OpenAiTranscriptionProviderClient(BackendEnvironmentProperties properties) {
		this.model = properties.transcription().model();
		this.restClient = RestClient.builder()
				.baseUrl(properties.openai().baseUrl())
				.defaultHeader("Authorization", "Bearer " + properties.openai().apiKey())
				.build();
	}

	@Override
	public TranscriptionResult transcribe(TranscriptionRequest request) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("model", model);
		body.add("response_format", "json");
		body.add("file", new NamedByteArrayResource(request.audioBytes(), request.filename()));
		try {
			OpenAiTranscriptionResponse response = restClient.post()
					.uri("/audio/transcriptions")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(body)
					.retrieve()
					.body(OpenAiTranscriptionResponse.class);
			if (response == null || response.text() == null || response.text().isBlank()) {
				throw new TranscriptionProviderException(AiFailureCodes.PROVIDER_FAILURE,
						"Transcription provider returned an empty transcript.");
			}
			return new TranscriptionResult(response.text().trim(), "openai", model, BigDecimal.ZERO);
		}
		catch (TranscriptionProviderException ex) {
			throw ex;
		}
		catch (RestClientException ex) {
			throw new TranscriptionProviderException(AiFailureCodes.PROVIDER_FAILURE,
					"Transcription provider request failed.");
		}
	}

	private static final class NamedByteArrayResource extends ByteArrayResource {

		private final String filename;

		private NamedByteArrayResource(byte[] byteArray, String filename) {
			super(byteArray == null ? new byte[0] : byteArray);
			this.filename = filename == null || filename.isBlank() ? "audio" : filename;
		}

		@Override
		public String getFilename() {
			return filename;
		}
	}

	private record OpenAiTranscriptionResponse(@JsonProperty("text") String text) {
	}
}
