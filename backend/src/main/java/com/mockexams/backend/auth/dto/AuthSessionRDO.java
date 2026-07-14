package com.mockexams.backend.auth.dto;

import java.time.OffsetDateTime;

public record AuthSessionRDO(
		String accessToken,
		String tokenType,
		OffsetDateTime expiresAt,
		CurrentUserRDO user
) {
}
