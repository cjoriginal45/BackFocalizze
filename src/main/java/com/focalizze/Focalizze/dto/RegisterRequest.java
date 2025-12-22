package com.focalizze.Focalizze.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio") // Username is required
        @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres ") // Username must be between 3 and 50 characters
        String username,

        @NotBlank(message = "El correo electrónico es obligatorio") // Email is required
        @Email(message = "El formato del correo electrónico no es válido") // Email format is invalid
        String email,

        @NotBlank(message = "La contraseña es obligatoria") // Password is required
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") // Password must be at least 6 characters
        String password,

        @NotBlank(message = "La confirmación de la contraseña es obligatoria ") // Password confirmation is required
        String confirmPassword
)
{ }
