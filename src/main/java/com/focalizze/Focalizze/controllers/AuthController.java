package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.*;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.AuthService;
import com.focalizze.Focalizze.services.EmailService;
import com.focalizze.Focalizze.utils.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil; // Tu utilidad corregida
    private final EmailService emailService;

    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager, UserRepository userRepository, JwtUtil jwtUtil, EmailService emailService) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse response = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- PASO 1: LOGIN ---
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) {

        System.out.println(">>> LOGIN INTENTO DESDE FRONTEND <<<");
        System.out.println("Username recibido: '" + request.identifier() + "'");
        System.out.println("Password recibido: '" + request.password() + "'");

        // 1. Validar Credenciales
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.identifier(), request.password())
            );
        } catch (BadCredentialsException e) {
            System.out.println(">>> ERROR: BadCredentialsException saltó.");
            // Devolvemos un DTO con mensaje de error (o puedes lanzar excepción y manejarla con ControllerAdvice)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponseDto(null, null, null, null, null, null, false, false, "Credenciales inválidas"));
        }

        // 2. Obtener Usuario
        User user = userRepository.findByUsername(request.identifier())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // 3. Verificar si tiene 2FA activado
        if (user.isTwoFactorEnabled()) {
            // A. Generar Código
            String code = String.valueOf(new Random().nextInt(900000) + 100000);

            // B. Guardar en BD
            user.setTwoFactorCode(code);
            user.setTwoFactorCodeExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            // C. Enviar Email
            emailService.sendTwoFactorCode(user.getEmail(), code);

            // D. Responder "Requiere 2FA" (Sin token, Sin datos de usuario)
            return ResponseEntity.ok(new LoginResponseDto(
                    user.getId(), // Podemos devolver el ID para facilitar el siguiente paso
                    null, null, null, null, null, true, // isTwoFactorEnabled = true
                    true, // requiresTwoFactor = TRUE
                    "Código de verificación enviado al correo."
            ));
        }

        // 4. Si NO tiene 2FA, Login Exitoso Directo
        String jwtToken = jwtUtil.generateToken(user);
        return ResponseEntity.ok(buildSuccessResponse(user, jwtToken));
    }

    // --- PASO 2: VERIFICAR CÓDIGO ---
    @PostMapping("/verify-2fa")
    public ResponseEntity<LoginResponseDto> verifyTwoFactor(@RequestBody VerifyOtpRequestDto request) {

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // 1. Validaciones
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

        // 2. Éxito: Limpiar código
        user.setTwoFactorCode(null);
        user.setTwoFactorCodeExpiry(null);
        userRepository.save(user);

        // 3. Generar Token y Respuesta
        String jwtToken = jwtUtil.generateToken(user);

        return ResponseEntity.ok(buildSuccessResponse(user, jwtToken));
    }

    // --- MÉTODOS PRIVADOS DE AYUDA (Para no repetir código) ---

    // Construye la respuesta exitosa completa con todos los datos que necesita el frontend
    private LoginResponseDto buildSuccessResponse(User user, String token) {
        return new LoginResponseDto(
                user.getId(),
                token,
                user.getDisplayName(),
                user.getAvatarUrl(defaultAvatarUrl),
                user.getFollowingCount(),
                user.getFollowersCount(),
                user.isTwoFactorEnabled(),
                false, // requiresTwoFactor = FALSE
                "Login exitoso"
        );
    }

    // Construye una respuesta de error rápida
    private LoginResponseDto createErrorResponse(String message) {
        return new LoginResponseDto(null, null, null, null, null, null, false, false, message);
    }
}
