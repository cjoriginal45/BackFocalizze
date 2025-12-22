package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.EmailService;
import com.focalizze.Focalizze.services.servicesImpl.PasswordResetServiceImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@email.com")
                .password("oldPass")
                .build();
    }

    // --- processForgotPassword ---

    @Test
    @DisplayName("processForgotPassword: Should generate token and send email if user exists")
    void processForgot_UserExists_ShouldSendEmail() {
        // Given
        given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(user));

        // When
        passwordResetService.processForgotPassword("test@email.com");

        // Then
        assertThat(user.getResetPasswordToken()).isNotNull();
        assertThat(user.getResetPasswordTokenExpiry()).isNotNull();

        verify(userRepository).save(user);
        verify(emailService).sendPasswordResetEmail(eq("test@email.com"), any(String.class));
    }

    @Test
    @DisplayName("processForgotPassword: Should do nothing if user not found (Security)")
    void processForgot_UserNotFound_ShouldDoNothing() {
        // Given
        given(userRepository.findByEmail("unknown@email.com")).willReturn(Optional.empty());

        // When
        passwordResetService.processForgotPassword("unknown@email.com");

        // Then
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    // --- validateResetToken ---

    @Test
    @DisplayName("validateResetToken: Should return true if valid and not expired")
    void validateToken_Valid_ReturnsTrue() {
        // Given
        user.setResetPasswordToken("validToken");
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(10)); // Expira en el futuro

        given(userRepository.findByResetPasswordToken("validToken")).willReturn(Optional.of(user));

        // When
        boolean isValid = passwordResetService.validateResetToken("validToken");

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("validateResetToken: Should return false if expired")
    void validateToken_Expired_ReturnsFalse() {
        // Given
        user.setResetPasswordToken("expiredToken");
        user.setResetPasswordTokenExpiry(LocalDateTime.now().minusMinutes(1)); // Expiró hace 1 min

        given(userRepository.findByResetPasswordToken("expiredToken")).willReturn(Optional.of(user));

        // When
        boolean isValid = passwordResetService.validateResetToken("expiredToken");

        // Then
        assertThat(isValid).isFalse();
    }

    // --- resetPassword ---

    @Test
    @DisplayName("resetPassword: Should update password and clear token")
    void resetPassword_Success() {
        // Given
        user.setResetPasswordToken("token");
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(5));

        given(userRepository.findByResetPasswordToken("token")).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newPass")).willReturn("encodedNewPass");

        // When
        passwordResetService.resetPassword("token", "newPass");

        // Then
        assertThat(user.getPassword()).isEqualTo("encodedNewPass");
        assertThat(user.getResetPasswordToken()).isNull(); // Token debe limpiarse
        assertThat(user.getResetPasswordTokenExpiry()).isNull();

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("resetPassword: Should throw exception if token invalid or expired")
    void resetPassword_Invalid_ThrowsException() {
        // Given
        // El repo devuelve empty (token no existe)
        given(userRepository.findByResetPasswordToken("badToken")).willReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () ->
                passwordResetService.resetPassword("badToken", "newPass")
        );

        verify(userRepository, never()).save(any());
    }
}
