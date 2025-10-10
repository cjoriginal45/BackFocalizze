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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegisterMapper registerMapper;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           RegisterMapper registerMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.registerMapper = registerMapper;
    }

    @Override
    @Transactional
    public RegisterResponse registerUser(RegisterRequest registerRequest) {

        // Validate passwords match / Validar la coincidencia de contraseñas
        if (!registerRequest.password().equals(registerRequest.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match / Las contraseñas no coinciden");
        }

        // Check if user already exists / Compruebe si el usuario ya existe
        if (userRepository.findByUsername(registerRequest.username()).isPresent()) {
            throw new UserAlreadyExistsException("Username is already taken / El nombre de usuario ya está en uso");
        }

        if (userRepository.findByEmail(registerRequest.email()).isPresent()) {
            throw new UserAlreadyExistsException("Email is already registered / El correo electrónico ya está registrado");
        }

        // Create and save user / Crear y guardar usuario
        User user = createUserFromRequest(registerRequest);
        User savedUser = userRepository.save(user);

        // Convert to Response using RegisterMapper / Convertir a respuesta usando RegisterMapper
        return registerMapper.toRegisterResponse(savedUser);
    }

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
