package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.LoginResponseDto;
import com.focalizze.Focalizze.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Mapper for login responses.
 * <p>
 * Mapper para respuestas de inicio de sesión.
 */
@Component
public class LoginMapper {

    @Value("${app.default-avatar-url}") // Inyecta el valor desde application.properties
    private String defaultAvatarUrl;

    /**
     * Creates a login response DTO from user entity and token.
     * <p>
     * Crea un DTO de respuesta de inicio de sesión a partir de la entidad usuario y el token.
     *
     * @param user  The authenticated user. / El usuario autenticado.
     * @param token The generated JWT token. / El token JWT generado.
     * @return The response DTO. / El DTO de respuesta.
     */
    public LoginResponseDto toDto(User user, String token) {
        if (user == null) {
            return null;
        }

        return new LoginResponseDto(
                user.getId(),
                token,
                user.getDisplayName(),
                user.getAvatarUrl(defaultAvatarUrl),
                user.getFollowingCount(),
                user.getFollowersCount(),
                user.getRole().name(),
                user.isTwoFactorEnabled(),
                false,
                "¡Login exitoso!"
        );
    }


}
