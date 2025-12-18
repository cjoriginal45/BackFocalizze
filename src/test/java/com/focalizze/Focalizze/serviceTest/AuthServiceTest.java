package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.RegisterRequest;
import com.focalizze.Focalizze.dto.RegisterResponse;
import com.focalizze.Focalizze.dto.mappers.RegisterMapper;
import com.focalizze.Focalizze.exceptions.UserAlreadyExistsException;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RegisterMapper registerMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRequest;
    private User savedUser;
    private RegisterResponse expectedResponse;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest(
                "newuser",
                "new@email.com",
                "pass123",
                "pass123"
        );

        savedUser = User.builder()
                .id(1L)
                .username("newuser")
                .email("new@email.com")
                .role(UserRole.USER)
                .build();

        expectedResponse = new RegisterResponse(1L, "newuser", "token_mock",
                "new@email.com","");
    }

    @Test
    @DisplayName("registerUser: Debería registrar usuario si los datos son válidos")
    void registerUser_WhenValid_ShouldReturnResponse() {
        // Given
        given(userRepository.findUserNameAvailable("newuser")).willReturn(true); // Disponible = true (no existe)
        given(userRepository.findByEmail("new@email.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("pass123")).willReturn("encodedPass");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(registerMapper.toRegisterResponse(savedUser)).willReturn(expectedResponse);

        // When
        RegisterResponse result = authService.registerUser(validRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("newuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("registerUser: Debería lanzar excepción si las contraseñas no coinciden")
    void registerUser_PasswordsMismatch_ThrowsException() {
        // Given
        RegisterRequest badRequest = new RegisterRequest("user", "mail", "pass1", "pass2");

        // When & Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.registerUser(badRequest)
        );
        assertThat(ex.getMessage()).isEqualTo("Las contraseñas no coinciden");
    }

    @Test
    @DisplayName("registerUser: Debería lanzar excepción si el nombre de usuario ya existe")
    void registerUser_UsernameTaken_ThrowsException() {
        // Given
        given(userRepository.findUserNameAvailable("newuser")).willReturn(false); // NO disponible

        // When & Then
        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class, () ->
                authService.registerUser(validRequest)
        );
        assertThat(ex.getMessage()).contains("nombre de usuario ya está en uso");
    }

    @Test
    @DisplayName("registerUser: Debería lanzar excepción si el email ya existe")
    void registerUser_EmailTaken_ThrowsException() {
        // Given
        given(userRepository.findUserNameAvailable("newuser")).willReturn(true);
        given(userRepository.findByEmail("new@email.com")).willReturn(Optional.of(new User()));

        // When & Then
        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class, () ->
                authService.registerUser(validRequest)
        );
        assertThat(ex.getMessage()).contains("correo electrónico ya está registrado");
    }
}
