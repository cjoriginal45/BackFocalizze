package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    /**
     * Convierte una entidad User a UserDto.
     * Este método se usa cuando NO tenemos contexto de interacción (ej. endpoint /me),
     * por lo que isFollowing e isBlocked se ponen en false por defecto.
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
     * Versión sobrecargada para cuando SÍ tenemos contexto de interacción.
     * Útil para perfiles de otros usuarios, feeds, buscadores, etc.
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
     * Convierte un UserDto a una entidad User.
     * NOTA: Los campos calculados (contadores, booleanos de estado) se ignoran
     * porque no se deben persistir directamente desde un DTO de entrada.
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
