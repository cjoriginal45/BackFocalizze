package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.SecurityService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link SecurityService} interface.
 * Handles security settings updates like 2FA and session management.
 * <p>
 * Implementación de la interfaz {@link SecurityService}.
 * Maneja actualizaciones de configuración de seguridad como 2FA y gestión de sesiones.
 */
@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Enables or disables Two-Factor Authentication for the user.
     * <p>
     * Habilita o deshabilita la Autenticación de Dos Factores para el usuario.
     *
     * @param enabled     Desired state. / Estado deseado.
     * @param currentUser The authenticated user. / El usuario autenticado.
     */
    @Override
    @Transactional
    public void toggleTwoFactor(boolean enabled, User currentUser) {
        // Reload user to ensure entity attachment
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found / Usuario no encontrado"));

        user.setTwoFactorEnabled(enabled);

        // Explicit save (good practice)
        userRepository.save(user);
    }

    /**
     * Invalidates all existing JWT tokens for the user by incrementing the token version.
     * Forces logout on all devices.
     * <p>
     * Invalida todos los tokens JWT existentes para el usuario incrementando la versión del token.
     * Fuerza el cierre de sesión en todos los dispositivos.
     *
     * @param currentUser The authenticated user. / El usuario autenticado.
     */
    @Override
    @Transactional
    public void logoutAllDevices(User currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found / Usuario no encontrado"));

        // Token invalidation logic: Increment version
        // Lógica de invalidación de token: Incrementar versión
        user.setTokenVersion(user.getTokenVersion() + 1);

        userRepository.save(user);
    }

    /**
     * Validates a raw password against the stored hash.
     * <p>
     * Valida una contraseña en texto plano contra el hash almacenado.
     *
     * @param rawPassword The password to check. / La contraseña a comprobar.
     * @param currentUser The user with the stored hash. / El usuario con el hash almacenado.
     * @return true if matches.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean validatePassword(String rawPassword, User currentUser) {
        // Obtenemos la contraseña real (hasheada) de la DB o del contexto
        return passwordEncoder.matches(rawPassword, currentUser.getPassword());
    }
}
