package com.focalizze.Focalizze.dto;

public record AdminThreadActionDto(
        Long reportId,
        String action,
        String newContentPost1,
        String newContentPost2,
        String newContentPost3
) {
}
