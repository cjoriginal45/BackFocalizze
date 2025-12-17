package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.TwoFactorRequestDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for security settings and session management.
 * <p>
 * Controlador para configuraciones de seguridad y gestión de sesiones.
 */
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;

    /**
     * Toggles Two-Factor Authentication (2FA) for the user.
     * <p>
     * Alterna la Autenticación de Dos Factores (2FA) para el usuario.
     *
     * @param request     Request object containing the enabled status. / Objeto de solicitud con el estado habilitado.
     * @param currentUser The authenticated user. / El usuario autenticado.
     * @return Empty response. / Respuesta vacía.
     */
    @PatchMapping("/2fa")
    public ResponseEntity<Void> toggleTwoFactor(
            @RequestBody TwoFactorRequestDto request,
            @AuthenticationPrincipal User currentUser
    ) {
        securityService.toggleTwoFactor(request.enabled(), currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Invalidates all active sessions for the user (Global Logout).
     * <p>
     * Invalida todas las sesiones activas para el usuario (Cierre de sesión global).
     *
     * @param currentUser The authenticated user. / El usuario autenticado.
     * @return Empty response. / Respuesta vacía.
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAllDevices(
            @AuthenticationPrincipal User currentUser
    ) {
        securityService.logoutAllDevices(currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Validates if the provided password matches the current user's password.
     * Used for sensitive actions (sudo mode).
     * <p>
     * Valida si la contraseña proporcionada coincide con la contraseña del usuario actual.
     * Utilizado para acciones sensibles (modo sudo).
     *
     * @param request     Map containing the password key. / Mapa conteniendo la clave password.
     * @param currentUser The authenticated user. / El usuario autenticado.
     * @return Boolean indicating validity. / Booleano indicando validez.
     */
    @PostMapping("/validate-password")
    public ResponseEntity<Boolean> validatePassword(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User currentUser
    ) {
        String password = request.get("password");
        boolean isValid = securityService.validatePassword(password, currentUser);

        if (isValid) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(false);
        }
    }
}