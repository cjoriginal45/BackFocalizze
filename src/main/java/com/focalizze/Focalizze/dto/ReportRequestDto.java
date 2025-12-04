package com.focalizze.Focalizze.dto;

import com.focalizze.Focalizze.models.ReportReason;
import jakarta.validation.constraints.NotNull;

public record ReportRequestDto(
        @NotNull
        ReportReason reason,
        String description
) {
}
