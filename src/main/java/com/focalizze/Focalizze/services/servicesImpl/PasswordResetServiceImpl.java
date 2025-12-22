package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.EmailService;
import com.focalizze.Focalizze.services.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of the {@link PasswordResetService} interface.
 * Handles the logic for generating reset tokens and updating user passwords.
 * <p>
 * Implementación de la interfaz {@link PasswordResetService}.
 * Maneja la lógica para generar tokens de restablecimiento y actualizar contraseñas de usuario.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Initiates the password reset process.
     * Generates a token and sends an email if the user exists.
     * <p>
     * Inicia el proceso de restablecimiento de contraseña.
     * Genera un token y envía un correo si el usuario existe.
     *
     * @param email The email of the user requesting the reset.
     *              El correo del usuario que solicita el restablecimiento.
     */
    @Override
    @Transactional
    public void processForgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Expira en 15 mins
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    /**
     * Validates if a reset token is valid and not expired.
     * <p>
     * Valida si un token de restablecimiento es válido y no ha expirado.
     *
     * @param token The token to validate.
     *              El token a validar.
     * @return {@code true} if valid, {@code false} otherwise.
     *         {@code true} si es válido, {@code false} de lo contrario.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        return userRepository.findByResetPasswordToken(token)
                .map(user -> user.getResetPasswordTokenExpiry().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    /**
     * Resets the user's password using a valid token.
     * <p>
     * Restablece la contraseña del usuario utilizando un token válido.
     *
     * @param token       The validation token.
     *                    El token de validación.
     * @param newPassword The new password to set.
     *                    La nueva contraseña establecer.
     * @throws IllegalArgumentException If the token is invalid or expired.
     *                                  Si el token es inválido o ha expirado.
     */
    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .filter(u -> u.getResetPasswordTokenExpiry().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new RuntimeException("Token inválido o expirado"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }
}
