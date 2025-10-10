package com.focalizze.Focalizze.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required") //El nombre de usuario es obligatorio
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") //El nombre de usuario debe tener entre 3 y 50 caracteres.
        String username,

        @NotBlank(message = "Email is required") // El correo electrónico es obligatorio
        @Email(message = "Email format is invalid") // El formato del correo electrónico no es válido
        String email,

        @NotBlank(message = "Password is required") // La contraseña es obligatoria
        @Size(min = 6, message = "Password must be at least 6 characters") // La contraseña debe tener al menos 6 caracteres
        String password,

        @NotBlank(message = "Password confirmation is required") // La confirmación de la contraseña es obligatoria
        String confirmPassword
)
{ }
