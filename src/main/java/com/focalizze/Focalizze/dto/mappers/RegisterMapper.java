package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.RegisterResponse;
import com.focalizze.Focalizze.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RegisterMapper {

    public RegisterResponse toRegisterResponse(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                "¡Usuario registrado exitosamente!"

        );
    }
}
