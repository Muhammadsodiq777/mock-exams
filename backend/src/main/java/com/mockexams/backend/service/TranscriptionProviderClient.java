package com.mockexams.backend.service;

public interface TranscriptionProviderClient {

	TranscriptionResult transcribe(TranscriptionRequest request);
}
