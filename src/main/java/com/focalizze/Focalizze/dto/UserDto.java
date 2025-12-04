package com.focalizze.Focalizze.dto;

public record UserDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        Integer calculatedThreadCount,
        boolean isFollowing,
        Integer followingCount,
        Integer followersCount,
        boolean isBlocked,
        String role,
        boolean isTwoFactorEnabled

) {

}