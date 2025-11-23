package com.focalizze.Focalizze.dto;

// Este DTO representa un item en el feed de "Descubrir"
public record DiscoverItemDto(
        FeedThreadDto thread,
        boolean isRecommended,
        String recommendationReason,
        String recommendationType
) {
}
