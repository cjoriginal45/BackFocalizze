package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Mapper for User entities to DTOs and vice versa.
 * Handles context-aware mapping (isFollowing, isBlocked).
 * <p>
 * Mapper para entidades Usuario a DTOs y viceversa.
 * Maneja mapeo consciente del contexto (isFollowing, isBlocked).
 */
@Component
public class UserMapper {
    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    /**
     * Converts User to UserDto without interaction context (defaults to false).
     * Used for self-profile or admin views.
     * <p>
     * Convierte Usuario a UserDto sin contexto de interacción (por defecto falso).
     */
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(defaultAvatarUrl), // Usamos el método helper de la entidad
                user.getCalculatedThreadCount() != null ? user.getCalculatedThreadCount() : 0,
                false, // isFollowing por defecto
                user.getFollowingCount(),
                user.getFollowersCount(),
                false, // isBlocked por defecto
                user.getRole() != null ? user.getRole().name() : "USER",
                user.isTwoFactorEnabled(),
                user.getBackgroundType(),
                user.getBackgroundValue()
        );
    }

    /**
     * Converts User to UserDto with explicit interaction context.
     * <p>
     * Convierte Usuario a UserDto con contexto de interacción explícito.
     */
    public UserDto toDto(User user, boolean isFollowing, boolean isBlocked) {
        if (user == null) {
            return null;
        }

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(defaultAvatarUrl),
                user.getCalculatedThreadCount() != null ? user.getCalculatedThreadCount() : 0,
                isFollowing, // Valor calculado real
                user.getFollowingCount(),
                user.getFollowersCount(),
                isBlocked,   // Valor calculado real
                user.getRole() != null ? user.getRole().name() : "USER",
                user.isTwoFactorEnabled(),
                user.getBackgroundType(),
                user.getBackgroundValue()
        );
    }

    /**
     * Converts UserDto to Entity.
     * Note: Ignores transient fields.
     */
    public User toEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setId(dto.id());
        user.setUsername(dto.username());
        user.setDisplayName(dto.displayName());
        user.setAvatarUrl(dto.avatarUrl());

        // Convertimos el String del rol al Enum
        if (dto.role() != null) {
            try {
                user.setRole(UserRole.valueOf(dto.role()));
            } catch (IllegalArgumentException e) {
                user.setRole(UserRole.USER); // Fallback por defecto
            }
        }

        // Nuevos campos
        user.setTwoFactorEnabled(dto.isTwoFactorEnabled());
        user.setBackgroundType(dto.backgroundType());
        user.setBackgroundValue(dto.backgroundValue());

        return user;
    }
}
