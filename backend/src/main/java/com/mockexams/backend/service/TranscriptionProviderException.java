package com.mockexams.backend.service;

public class TranscriptionProviderException extends RuntimeException {

	private final String failureCode;

	public TranscriptionProviderException(String failureCode, String message) {
		super(message);
		this.failureCode = failureCode;
	}

	public String getFailureCode() {
		return failureCode;
	}
}
