package com.focalizze.Focalizze.dto;

public record CategoryDto(
        Long id, // Necesitamos el ID para la acción de seguir
        String name,
        String description,
        Integer followersCount,
        boolean isFollowedByCurrentUser
) {
}
