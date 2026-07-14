package com.mockexams.backend.service.impl;

import java.math.BigDecimal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.mockexams.backend.config.BackendEnvironmentProperties;
import com.mockexams.backend.service.TranscriptionProviderClient;
import com.mockexams.backend.service.TranscriptionRequest;
import com.mockexams.backend.service.TranscriptionResult;

@Service
@ConditionalOnProperty(prefix = "app.transcription", name = "provider", havingValue = "mock", matchIfMissing = true)
class MockTranscriptionProviderClient implements TranscriptionProviderClient {

	private final String model;

	MockTranscriptionProviderClient(BackendEnvironmentProperties properties) {
		this.model = properties.transcription().model();
	}

	@Override
	public TranscriptionResult transcribe(TranscriptionRequest request) {
		String prompt = request.prompt() == null || request.prompt().isBlank() ? "speaking prompt" : request.prompt();
		return new TranscriptionResult("Mock transcript for: " + prompt, "mock", model, BigDecimal.ZERO);
	}
}
