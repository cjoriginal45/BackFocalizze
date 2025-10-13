package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.RegisterRequest;
import com.focalizze.Focalizze.dto.mappers.RegisterMapper;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

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
}
