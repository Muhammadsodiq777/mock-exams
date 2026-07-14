package com.mockexams.backend.api;

public record ApiEnvelope<T>(
		T data,
		ApiMeta meta
) {
	public static <T> ApiEnvelope<T> of(T data) {
		return new ApiEnvelope<>(data, ApiMeta.create());
	}

	public static <T> ApiEnvelope<T> of(T data, ApiMeta meta) {
		return new ApiEnvelope<>(data, meta);
	}
}
