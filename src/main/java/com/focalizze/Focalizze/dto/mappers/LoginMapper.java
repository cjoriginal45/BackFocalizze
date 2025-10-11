package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.LoginResponseDto;
import com.focalizze.Focalizze.models.User;
import org.springframework.stereotype.Component;

@Component
public class LoginMapper {

    public LoginResponseDto toDto(User user, String token) {
        if (user == null) {
            return null;
        }

        return new LoginResponseDto(
                user.getId(),
                token,
                user.getDisplayName()
        );
    }


}
