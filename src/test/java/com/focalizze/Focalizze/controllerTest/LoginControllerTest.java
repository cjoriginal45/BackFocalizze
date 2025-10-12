package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.configurations.ApplicationConfig;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.LoginController;
import com.focalizze.Focalizze.dto.LoginRequestDto;
import com.focalizze.Focalizze.dto.LoginResponseDto;
import com.focalizze.Focalizze.dto.mappers.LoginMapper;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.utils.JwtRequestFilter;
import com.focalizze.Focalizze.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@Import({SecurityConfig.class, JwtRequestFilter.class, JwtUtil.class, ApplicationConfig.class})
@WebMvcTest(LoginController.class)
public class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- USANDO LA NUEVA ANOTACIÓN @MockitoBean ---
    // --- USING THE NEW @MockitoBean ANNOTATION ---
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private LoginMapper loginMapper;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;
    private LoginRequestDto loginRequest;
    private LoginResponseDto loginResponse;
    private String fakeToken;

    @BeforeEach
    void setUp() {
        // Given: Configuración de datos de prueba comunes
        // Given: Configuration of common test data
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@email.com")
                .password("hashedpassword")
                .displayName("Test User")
                .role(UserRole.USER)
                .build();

        loginRequest = new LoginRequestDto("testuser", "password");
        fakeToken = "fake.jwt.token.string";
        loginResponse = new LoginResponseDto(testUser.getId(), fakeToken, testUser.getDisplayName());
    }


    @Test
    @DisplayName("Debería devolver 200 OK con un token JWT cuando las credenciales son válidas")
    void login_WhenCredentialsAreValid_ShouldReturn200OkAndToken() throws Exception {
        // Given: Configuramos los mocks para el escenario de éxito
        // Given: We configure the mocks for the success scenario
        given(userDetailsService.loadUserByUsername(loginRequest.identifier())).willReturn(testUser);
        given(jwtUtil.generateToken(testUser)).willReturn(fakeToken);
        given(loginMapper.toDto(testUser, fakeToken)).willReturn(loginResponse);
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(null);

        // When: Ejecutamos la petición POST a /api/auth/login
        // When: We execute the POST request to /api/auth/login
        ResultActions response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .with(csrf()));

        // Then: Verificamos que la respuesta es la esperada
        // Then: We verify that the response is as expected
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(fakeToken))
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.displayName").value(testUser.getDisplayName()));
    }


    @Test
    @DisplayName("Debería devolver 401 Unauthorized cuando las credenciales son inválidas")
    void login_WhenCredentialsAreInvalid_ShouldReturn401Unauthorized() throws Exception {
        // Given: Configuramos el mock de AuthenticationManager para que lance una excepción
        // Given: We configure the AuthenticationManager mock to throw an exception
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("Credenciales inválidas"));

        // When: Ejecutamos la petición POST con las credenciales incorrectas
        // When: We execute the POST request with the incorrect credentials
        ResultActions response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .with(csrf()));

        // Then: Verificamos que la respuesta es un error 401
        // Then: We verify that the response is a 401 error
        response.andExpect(status().isUnauthorized());
    }
}
