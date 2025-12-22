package com.focalizze.Focalizze.dto;

public record CategoryDetailsDto(
        Long id,
        String name,
        String description,
        String imageUrl,
        Integer followersCount,
        Long threadsCount,
        boolean isFollowing) {
}
