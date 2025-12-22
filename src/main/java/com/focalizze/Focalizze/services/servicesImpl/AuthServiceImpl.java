package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.RegisterRequest;
import com.focalizze.Focalizze.dto.RegisterResponse;
import com.focalizze.Focalizze.dto.mappers.RegisterMapper;
import com.focalizze.Focalizze.exceptions.UserAlreadyExistsException;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementation of the {@link AuthService} interface.
 * Handles user registration and authentication logic.
 * <p>
 * Implementación de la interfaz {@link AuthService}.
 * Maneja la lógica de registro y autenticación de usuarios.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegisterMapper registerMapper;

    /**
     * Registers a new user in the system.
     * Performs validations for password matching and username/email availability.
     * <p>
     * Registra un nuevo usuario en el sistema.
     * Realiza validaciones para la coincidencia de contraseñas y disponibilidad de nombre de usuario/correo.
     *
     * @param registerRequest The registration data.
     *                        Los datos de registro.
     * @return The response containing basic user info.
     *         La respuesta que contiene información básica del usuario.
     * @throws IllegalArgumentException   If passwords do not match.
     *                                    Si las contraseñas no coinciden.
     * @throws UserAlreadyExistsException If username or email is taken.
     *                                    Si el nombre de usuario o correo ya está en uso.
     */
    @Override
    @Transactional
    public RegisterResponse registerUser(RegisterRequest registerRequest) {

        if (!registerRequest.password().equals(registerRequest.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        if (!userRepository.findUserNameAvailable(registerRequest.username())) {
            throw new UserAlreadyExistsException("El nombre de usuario ya está en uso");
        }

        if (userRepository.findByEmail(registerRequest.email()).isPresent()) {
            throw new UserAlreadyExistsException("El correo electrónico ya está registrado");
        }

        User user = createUserFromRequest(registerRequest);
        User savedUser = userRepository.save(user);

        return registerMapper.toRegisterResponse(savedUser);
    }

    /**
     * Helper method to build a User entity from the request.
     * <p>
     * Método auxiliar para construir una entidad User a partir de la solicitud.
     *
     * @param request The request DTO. / El DTO de solicitud.
     * @return The built User entity. / La entidad User construida.
     */
    private User createUserFromRequest(RegisterRequest request) {
        return User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .displayName(request.username()) // Use username as displayName / Utilice el nombre de usuario como nombre para mostrar
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .dailyThreadsRemaining(5)
                .dailyInteractionsRemaining(50)
                .followersCount(0)
                .followingCount(0)
                .biography("")
                .avatarUrl("")
                .build();
    }
}
