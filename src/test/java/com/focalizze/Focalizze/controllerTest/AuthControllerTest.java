package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.dto.LoginRequestDto;
import com.focalizze.Focalizze.dto.RegisterRequest;
import com.focalizze.Focalizze.dto.RegisterResponse;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.AuthService;
import com.focalizze.Focalizze.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.focalizze.Focalizze.configurations.ApplicationConfig;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.AuthController;
import com.focalizze.Focalizze.utils.JwtRequestFilter;
import com.focalizze.Focalizze.utils.JwtUtil;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, JwtRequestFilter.class, JwtUtil.class, ApplicationConfig.class})
@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private EmailService emailService;


    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").email("test@email.com").role(UserRole.USER).build();
    }

    @Test
    @DisplayName("registerUser: Should return 201 Created")
    void registerUser_Success() throws Exception {
        RegisterRequest req = new RegisterRequest("validuser", "valid@email.com", "password123", "password123");
        RegisterResponse res = new RegisterResponse(1L, "user", "token","mail","message");

        given(authService.registerUser(any())).willReturn(res);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    @DisplayName("login: Should return JWT if credentials valid")
    void login_Success() throws Exception {
        LoginRequestDto req = new LoginRequestDto("user", "pass");

        // Simulamos autenticación exitosa (no lanza excepción)
        given(authenticationManager.authenticate(any())).willReturn(null);
        given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
        given(jwtUtil.generateToken(any())).willReturn("mock-jwt-token");

        // Inyectamos valor para @Value en controller (si usas MockMvc con WebMvcTest, los @Value a veces son nulos)
        // Pero Spring Boot Test suele resolverlos si están en application.properties.
        // Si falla, usa ReflectionTestUtils en el @BeforeEach del test.

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.requiresTwoFactor").value(false));
    }

    @Test
    @DisplayName("login: Should return 401 Unauthorized if bad credentials")
    void login_BadCredentials() throws Exception {
        LoginRequestDto req = new LoginRequestDto("user", "wrong");

        given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("Bad creds"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials / Credenciales inválidas"));
    }

    @Test
    @DisplayName("login: Should return 200 with 2FA required if enabled")
    void login_2FA_Enabled() throws Exception {
        user.setTwoFactorEnabled(true);
        LoginRequestDto req = new LoginRequestDto("user", "pass");

        given(authenticationManager.authenticate(any())).willReturn(null);
        given(userRepository.findByUsername("user")).willReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresTwoFactor").value(true))
                .andExpect(jsonPath("$.token").doesNotExist()); // No devuelve token aún
    }

}
