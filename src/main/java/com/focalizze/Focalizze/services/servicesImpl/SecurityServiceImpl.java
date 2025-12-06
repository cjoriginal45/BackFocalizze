package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void toggleTwoFactor(boolean enabled, User currentUser) {
        // Recargamos el usuario desde la BD para asegurar consistencia
        // (aunque currentUser viene del contexto, es mejor asegurar la transacción con la entidad viva)
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setTwoFactorEnabled(enabled);

        // No es estrictamente necesario llamar a save() dentro de una transacción
        // gestionada por JPA (Dirty Checking), pero es buena práctica explícita.
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void logoutAllDevices(User currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Lógica de invalidación de tokens:
        // Incrementamos la versión. Cualquier token con la versión vieja (versión actual - 1)
        // fallará en el JwtFilter al compararse.
        user.setTokenVersion(user.getTokenVersion() + 1);

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validatePassword(String rawPassword, User currentUser) {
        // Obtenemos la contraseña real (hasheada) de la DB o del contexto
        return passwordEncoder.matches(rawPassword, currentUser.getPassword());
    }
}
