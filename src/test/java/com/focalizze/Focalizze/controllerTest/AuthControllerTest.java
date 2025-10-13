package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.dto.RegisterRequest;
import com.focalizze.Focalizze.dto.RegisterResponse;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.focalizze.Focalizze.configurations.ApplicationConfig;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.AuthController;
import com.focalizze.Focalizze.utils.JwtRequestFilter;
import com.focalizze.Focalizze.utils.JwtUtil;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

@Import({SecurityConfig.class, JwtRequestFilter.class, JwtUtil.class, ApplicationConfig.class})
@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private RegisterRequest validRequest;
    private RegisterResponse successResponse;

    /**
     * Configuración inicial antes de cada test
     * Inicializa los objetos de prueba que se usan en múltiples tests
     * /
     * Initial setup before each test.
     * Initializes test objects used in multiple tests.
     *
     */
    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest(
                "testuser",
                "test@example.com",
                "password123",
                "password123"
        );

        successResponse = new RegisterResponse(
                1L,
                "testuser",
                "testuser",
                "test@example.com",
                "User registered successfully"
        );
    }


}
