package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record AdminModerationQueueMetaRDO(
		long pendingCount,
		OffsetDateTime oldestSubmittedAt
) {
}
