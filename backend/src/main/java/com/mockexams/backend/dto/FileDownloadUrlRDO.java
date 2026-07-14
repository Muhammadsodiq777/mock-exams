package com.mockexams.backend.dto;

public record FileDownloadUrlRDO(
		Long fileId,
		String url,
		int expiresInSeconds
) {
}
