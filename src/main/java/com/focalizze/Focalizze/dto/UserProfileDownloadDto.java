package com.focalizze.Focalizze.dto;

public record UserProfileDownloadDto(
        String username,
        String avatarUrl,
        String biography
) {
}
