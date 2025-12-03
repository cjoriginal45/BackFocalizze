package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.LoginResponseDto;
import com.focalizze.Focalizze.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginMapper {

    @Value("${app.default-avatar-url}") // Inyecta el valor desde application.properties
    private String defaultAvatarUrl;

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
                user.isTwoFactorEnabled()
        );
    }


}
