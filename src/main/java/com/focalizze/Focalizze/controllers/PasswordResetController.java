package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.ForgotPasswordRequest;
import com.focalizze.Focalizze.dto.ResetPasswordRequest;
import com.focalizze.Focalizze.services.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling password reset operations.
 * Manages the flow: Request Link -> Validate Token -> Set New Password.
 * <p>
 * Controlador para manejar operaciones de restablecimiento de contraseña.
 * Gestiona el flujo: Solicitar Enlace -> Validar Token -> Establecer Nueva Contraseña.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    /**
     * Initiates the password reset process by sending an email with a token.
     * <p>
     * Inicia el proceso de restablecimiento de contraseña enviando un correo con un token.
     *
     * @param request Object containing the user's email. / Objeto que contiene el correo del usuario.
     * @return Empty response (200 OK). / Respuesta vacía (200 OK).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.processForgotPassword(request.email());
        return ResponseEntity.ok().build();
    }

    /**
     * Validates if a password reset token is valid and not expired.
     * <p>
     * Valida si un token de restablecimiento de contraseña es válido y no ha expirado.
     *
     * @param token The token to validate. / El token a validar.
     * @return 200 OK if valid, 400 Bad Request otherwise. / 200 OK si es válido, 400 Bad Request de lo contrario.
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<Void> validateToken(@RequestParam String token) {
        if (passwordResetService.validateResetToken(token)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Resets the user's password using the token and the new password.
     * <p>
     * Restablece la contraseña del usuario usando el token y la nueva contraseña.
     *
     * @param request Object containing the token and new password. / Objeto que contiene el token y la nueva contraseña.
     * @return 200 OK if successful, 400 Bad Request if token is invalid. / 200 OK si es exitoso, 400 Bad Request si el token es inválido.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.token(), request.newPassword());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

