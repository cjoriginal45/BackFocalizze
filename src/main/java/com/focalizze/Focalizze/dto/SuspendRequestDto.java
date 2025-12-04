package com.focalizze.Focalizze.dto;

public record SuspendRequestDto(
        Long reportId,
        String action,
        Integer suspensionDays
) {
}
