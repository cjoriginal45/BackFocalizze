package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.dto.RegisterRequest;
import com.focalizze.Focalizze.dto.RegisterResponse;
import com.focalizze.Focalizze.exceptions.UserAlreadyExistsException;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        // Request válido para pruebas exitosas
        // Valid request for successful tests
        validRequest = new RegisterRequest(
                "testuser",
                "test@example.com",
                "password123",
                "password123"
        );

        // Response exitoso mock
        // Response successful mock
        successResponse = new RegisterResponse(
                1L,
                "testuser",
                "testuser",
                "test@example.com",
                "User registered successfully"
        );
    }

    /**
     * Prueba: Registro exitoso con datos válidos
     * Verifica que cuando se envían datos válidos al endpoint /api/auth/register:
     * - Retorna status HTTP 201 (Created)
     * - Retorna el RegisterResponse con todos los datos correctos
     * - El servicio AuthService es llamado correctamente y retorna la respuesta esperada
     * /
     * Test: Successful registration with valid data
     * Verify that when valid data is sent to the /api/auth/register endpoint:
     * - Returns HTTP status 201 (Created)
     * - Returns the RegisterResponse with all the correct data
     * - The AuthService is called correctly and returns the expected response
     */
    @Test
    @DisplayName("Debería devolver 201 Created cuando el registro es exitoso")
    void registerUser_WhenDataIsValid_ShouldReturn201Created() throws Exception {
        // Given: Configurar el mock del servicio para retornar respuesta exitosa
        // Configure the service mock to return a successful response
        given(authService.registerUser(any(RegisterRequest.class))).willReturn(successResponse);

        // When & Then: Ejecutar petición POST y verificar respuesta
        // Execute POST request and verify response
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.displayName").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    /**
     * Prueba: Error cuando el nombre de usuario ya existe
     * Verifica que cuando el servicio lanza UserAlreadyExistsException por username duplicado:
     * - Retorna status HTTP 409 (Conflict)
     * - Retorna el mensaje de error específico en el cuerpo de la respuesta
     * - El GlobalExceptionHandler maneja correctamente la excepción
     * /
     * Test: Error when username already exists
     * Verify that when the service throws a UserAlreadyExistsException for a duplicate username:
     * - Return HTTP status 409 (Conflict)
     * - Return the specific error message in the response body
     * - The GlobalExceptionHandler correctly handles the exception
     */
    @Test
    @DisplayName("Debería devolver 409 Conflict cuando el nombre de usuario ya existe")
    void registerUser_WhenUsernameAlreadyExists_ShouldReturn409Conflict() throws Exception {
        // Given: Configurar el mock del servicio para lanzar excepción por username existente
        // Configure the service mock to throw an exception for an existing username
        given(authService.registerUser(any(RegisterRequest.class)))
                .willThrow(new UserAlreadyExistsException("El nombre de usuario ya está en uso"));

        // When & Then: Ejecutar petición y verificar error 409 Conflict
        // Execute request and check for 409 Conflict error
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El nombre de usuario ya está en uso"));
    }

    /**
     * Prueba: Error cuando el email ya existe
     * Verifica que cuando el servicio lanza UserAlreadyExistsException por email duplicado:
     * - Retorna status HTTP 409 (Conflict)
     * - Retorna el mensaje de error específico en el cuerpo de la respuesta
     * - El GlobalExceptionHandler maneja correctamente la excepción
     * /
     * Test: Error when email already exists
     * Verify that when the service throws a UserAlreadyExistsException for a duplicate email:
     * - Returns HTTP status 409 (Conflict)
     * - Returns the specific error message in the response body
     * - The GlobalExceptionHandler correctly handles the exception
     */
    @Test
    @DisplayName("Debería devolver 409 Conflict cuando el email ya existe")
    void registerUser_WhenEmailAlreadyExists_ShouldReturn409Conflict() throws Exception {
        // Given: Configurar el mock del servicio para lanzar excepción por email existente
        // Configure the service mock to throw an exception for an existing email
        given(authService.registerUser(any(RegisterRequest.class)))
                .willThrow(new UserAlreadyExistsException("El email ya está registrado"));

        // When & Then: Ejecutar petición y verificar error 409 Conflict
        // Execute request and check for 409 Conflict error
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El email ya está registrado"));
    }

    /**
     * Prueba: Error cuando las contraseñas no coinciden
     * Verifica que cuando el servicio lanza IllegalArgumentException por contraseñas diferentes:
     * - Retorna status HTTP 400 (Bad Request)
     * - Retorna el mensaje de error específico en el cuerpo de la respuesta
     * - El GlobalExceptionHandler maneja correctamente la excepción
     * /
     * Test: Error when passwords do not match
     * Verify that when the service throws an IllegalArgumentException for different passwords:
     * - Return HTTP status 400 (Bad Request)
     * - Return the specific error message in the response body
     * - The GlobalExceptionHandler correctly handles the exception
     */
    @Test
    @DisplayName("Debería devolver 400 Bad Request cuando las contraseñas no coinciden")
    void registerUser_WhenPasswordsDoNotMatch_ShouldReturn400BadRequest() throws Exception {
        // Given: Configurar el mock del servicio para lanzar excepción por contraseñas no coincidentes
        // Configure the service mock to throw an exception for mismatched passwords
        given(authService.registerUser(any(RegisterRequest.class)))
                .willThrow(new IllegalArgumentException("Las contraseñas no coinciden"));

        // When & Then: Ejecutar petición y verificar error 400 Bad Request
        // Execute request and check for 400 Bad Request error
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Las contraseñas no coinciden"));
    }

    /**
     * Prueba: Error cuando los datos de registro son inválidos (validación de Spring)
     * Verifica que cuando se envían datos que violan las validaciones (@NotBlank, @Email, @Size):
     * - Retorna status HTTP 400 (Bad Request)
     * - Las validaciones de Spring se ejecutan antes de llamar al servicio
     * - El servicio AuthService NO es llamado (la validación falla a nivel de controller)
     * /
     * * Test: Error when registration data is invalid (Spring validation)
     * Verify that when data is sent that violates validations (@NotBlank, @Email, @Size):
     * - Returns HTTP status 400 (Bad Request)
     * - Spring validations are executed before calling the service
     * - The AuthService service is NOT called (validation fails at the controller level)
     */
    @Test
    @DisplayName("Debería devolver 400 Bad Request cuando los datos de registro son inválidos")
    void registerUser_WhenDataIsInvalid_ShouldReturn400BadRequest() throws Exception {
        // Given: Crear request con datos inválidos que violan las validaciones
        // Create request with invalid data that violates validations
        RegisterRequest invalidRequest = new RegisterRequest(
                "",
                "invalid-email",
                "123",
                "456"
        );

        // When & Then: Ejecutar petición y verificar error 400 por validación fallida
        // No es necesario configurar el mock del servicio porque la validación falla antes de llamarlo
        // Execute request and check for 400 error due to failed validation
        // It is not necessary to configure the service mock because validation fails before calling it
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

}
