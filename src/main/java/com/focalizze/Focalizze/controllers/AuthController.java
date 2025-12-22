package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.*;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.AuthService;
import com.focalizze.Focalizze.services.EmailService;
import com.focalizze.Focalizze.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Controller handling authentication endpoints.
 * Includes Registration, Login, and Two-Factor Authentication (2FA) verification.
 * <p>
 * Controlador que maneja los endpoints de autenticación.
 * Incluye Registro, Inicio de sesión y Verificación de Autenticación de Dos Factores (2FA).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    // SecureRandom is better for security-sensitive random generation
    // SecureRandom es mejor para la generación aleatoria sensible a la seguridad
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Registers a new user.
     * <p>
     * Registra un nuevo usuario.
     *
     * @param registerRequest The registration details. / Los detalles de registro.
     * @return The registered user info. / La información del usuario registrado.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse response = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- PASO 1: LOGIN ---
    /**
     * Authenticates a user. Checks credentials and banned status.
     * If 2FA is enabled, sends a code instead of a JWT.
     * <p>
     * Autentica a un usuario. Verifica credenciales y estado de baneo.
     * Si 2FA está activado, envía un código en lugar de un JWT.
     *
     * @param request The login credentials. / Las credenciales de inicio de sesión.
     * @return Login response (JWT or 2FA requirement). / Respuesta de login (JWT o requerimiento 2FA).
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) {

        try {
            // Attempt authentication once
            // Intentar autenticación una vez
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.identifier(), request.password())
            );
        } catch (BadCredentialsException e) {
            log.warn("Login failed: Bad credentials for identifier: {}", request.identifier());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid credentials / Credenciales inválidas"));

        } catch (LockedException e) {
            // Handle Banned/Suspended Users
            // Manejar Usuarios Baneados/Suspendidos
            log.warn("Login failed: Account locked for identifier: {}", request.identifier());

            User user = userRepository.findByUsername(request.identifier()).orElse(null);
            String msg = "Your account has been suspended. / Tu cuenta ha sido suspendida.";

            if (user != null) {
                if (user.getBanExpiresAt() == null) {
                    msg = "Your account has been permanently suspended. Reason: " + user.getBanReason();
                } else {
                    msg = "Suspension until: " + user.getBanExpiresAt().toString() + ". Reason: " + user.getBanReason();
                }
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse(msg));
        }

        // 2. Fetch User (Authenticated successfully)
        // 2. Obtener Usuario (Autenticado exitosamente)
        User user = userRepository.findByUsername(request.identifier())
                .orElseThrow(() -> new UsernameNotFoundException("User not found / Usuario no encontrado"));


        // 3. Check 2FA
        // 3. Verificar 2FA
        if (user.isTwoFactorEnabled()) {
            // A. Generate Code (Secure)
            String code = String.valueOf(secureRandom.nextInt(900000) + 100000);

            // B. Save to DB
            user.setTwoFactorCode(code);
            user.setTwoFactorCodeExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            // C. Send Email
            emailService.sendTwoFactorCode(user.getEmail(), code);

            // D. Return "Requires 2FA" response
            return ResponseEntity.ok(new LoginResponseDto(
                    user.getId(),
                    null, null, null, null, null, null,
                    true, // isTwoFactorEnabled
                    true, // requiresTwoFactor
                    "Verification code sent to email. / Código de verificación enviado al correo."
            ));
        }

        // 4. No 2FA -> Direct Success
        // 4. No 2FA -> Éxito Directo
        String jwtToken = jwtUtil.generateToken(user);
        return ResponseEntity.ok(buildSuccessResponse(user, jwtToken));
    }

    // --- PASO 2: VERIFICAR CÓDIGO ---
    /**
     * Verifies the 2FA OTP code.
     * <p>
     * Verifica el código OTP de 2FA.
     *
     * @param request The verification details. / Los detalles de verificación.
     * @return Login response with JWT if successful. / Respuesta de login con JWT si es exitoso.
     */
    @PostMapping("/verify-2fa")
    public ResponseEntity<LoginResponseDto> verifyTwoFactor(@RequestBody VerifyOtpRequestDto request) {

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // 1. Validations
        if (user.getTwoFactorCode() == null || user.getTwoFactorCodeExpiry() == null) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("No hay una solicitud de 2FA pendiente."));
        }

        if (LocalDateTime.now().isAfter(user.getTwoFactorCodeExpiry())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("El código ha expirado."));
        }

        if (!user.getTwoFactorCode().equals(request.code())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Código incorrecto."));
        }

        // 2. Success: Clear code
        // 2. Éxito: Limpiar código
        user.setTwoFactorCode(null);
        user.setTwoFactorCodeExpiry(null);
        userRepository.save(user);

        // 3. Generar Token y Respuesta
        String jwtToken = jwtUtil.generateToken(user);

        return ResponseEntity.ok(buildSuccessResponse(user, jwtToken));
    }

    // --- HELPER METHODS / MÉTODOS DE AYUDA ---
    private LoginResponseDto buildSuccessResponse(User user, String token) {
        return new LoginResponseDto(
                user.getId(),
                token,
                user.getDisplayName(),
                user.getAvatarUrl(defaultAvatarUrl),
                user.getFollowingCount(),
                user.getFollowersCount(),
                user.getRole().name(),
                user.isTwoFactorEnabled(),
                false, // requiresTwoFactor = FALSE
                "Login exitoso"
        );
    }

    // Construye una respuesta de error rápida
    private LoginResponseDto createErrorResponse(String message) {
        return new LoginResponseDto(null, null, null, null, null, null, null,false, false, message);
    }
}
