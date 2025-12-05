package com.focalizze.Focalizze.dto;

import com.focalizze.Focalizze.models.ReportReason;

import java.time.LocalDateTime;

public record ReportResponseDto(
        Long id,
        String reporterUsername,
        String reporterAvatarUrl,
        String reportedUsername,
        String reportedAvatarUrl,
        ReportReason reason,
        String description,
        LocalDateTime createdAt,
        Long reportedThreadId,
        String reportedThreadPreview
) {
}
