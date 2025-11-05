package com.focalizze.Focalizze.dto;

public record UserDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        boolean isFollowing
) {

}
