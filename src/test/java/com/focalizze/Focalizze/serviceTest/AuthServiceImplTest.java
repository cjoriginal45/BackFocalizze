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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RegisterMapper registerMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User user;

    @BeforeEach
    void setUp() {
        // Inicializamos un request y un usuario de prueba para usar en varios tests
        // We initialize a request and a test user to use in several tests
        registerRequest = new RegisterRequest(
                "testuser",
                "test@example.com",
                "password123",
                "password123"
        );

        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .displayName("testuser")
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

    /**
     * Prueba: Registro exitoso cuando el usuario no existe
     * Verifica que cuando todos los datos son válidos y el usuario no existe,
     * el servicio crea el usuario correctamente y retorna la respuesta esperada
     * /
     * Test: Successful registration when the user does not exist
     * Verify that when all data is valid and the user does not exist,
     * the service creates the user correctly and returns the expected response
     */
    @Test
    void registerUser_WhenUserDoesNotExist_ShouldReturnRegisterResponse() {
        // Arrange: Configurar mocks para simular un registro exitoso
        // Set up mocks to simulate a successful registration
        when(userRepository.findUserNameAvailable(registerRequest.username())).thenReturn(true);
        when(userRepository.findByEmail(registerRequest.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(registerMapper.toRegisterResponse(any(User.class))).thenReturn(
                new RegisterResponse(1L, "testuser", "testuser", "test@example.com", "User registered successfully")
        );

        // Act: Ejecutar el método a testear
        // Execute the method to be tested
        RegisterResponse response = authService.registerUser(registerRequest);

        // Assert: Verificar que la respuesta es la esperada
        // Verify that the response is as expected
        assertNotNull(response);
        assertEquals(1L, response.userId());
        assertEquals("testuser", response.username());
        assertEquals("testuser", response.displayName());
        assertEquals("test@example.com", response.email());
        assertEquals("User registered successfully", response.message());

        // Verify: Verificar que se llamaron los métodos necesarios
        // Verify that the necessary methods were called
        verify(userRepository, times(1)).findUserNameAvailable(registerRequest.username());
        verify(userRepository, times(1)).findByEmail(registerRequest.email());
        verify(passwordEncoder, times(1)).encode(registerRequest.password());
        verify(userRepository, times(1)).save(any(User.class));
        verify(registerMapper, times(1)).toRegisterResponse(any(User.class));
    }

    /**
     * Prueba: Error cuando el nombre de usuario ya existe
     * Verifica que se lanza la excepción correcta cuando el username está en uso
     * /
     * Test: Error when username already exists
     * Verify that the correct exception is thrown when the username is in use
     */
    @Test
    void registerUser_WhenUsernameAlreadyExists_ShouldThrowException() {
        // Arrange: Simular que el username ya existe
        // Pretend that the username already exists
        when(userRepository.findUserNameAvailable(registerRequest.username())).thenReturn(false);

        // Act & Assert: Verificar que se lanza la excepción esperada
        // Verify that the expected exception is thrown
        assertThrows(UserAlreadyExistsException.class, () -> authService.registerUser(registerRequest));

        // Verify: Verificar que no se llamó a guardar el usuario
        // Verify that the user was not called to save
        verify(userRepository, times(1)).findUserNameAvailable(registerRequest.username());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Prueba: Error cuando el email ya existe
     * Verifica que se lanza la excepción correcta cuando el email está registrado
     * /
     * Test: Error when email already exists
     * Verify that the correct exception is thrown when the email is registered
     */
    @Test
    void registerUser_WhenEmailAlreadyExists_ShouldThrowException() {
        // Arrange: Simular que el username está disponible pero el email ya existe
        // Simulate that the username is available but the email already exists
        when(userRepository.findUserNameAvailable(registerRequest.username())).thenReturn(true);
        when(userRepository.findByEmail(registerRequest.email())).thenReturn(Optional.of(user));

        // Act & Assert: Verificar que se lanza la excepción esperada
        // Verify that the expected exception is thrown
        assertThrows(UserAlreadyExistsException.class, () -> authService.registerUser(registerRequest));

        // Verify: Verificar que se verificaron ambos campos pero no se guardó
        // Verify that both fields were checked but not saved
        verify(userRepository, times(1)).findUserNameAvailable(registerRequest.username());
        verify(userRepository, times(1)).findByEmail(registerRequest.email());
        verify(userRepository, never()).save(any(User.class));
    }


}
