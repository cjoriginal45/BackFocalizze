package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.SecurityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SecurityServiceImpl securityService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("secureUser")
                .password("encodedPass")
                .tokenVersion(1)
                .isTwoFactorEnabled(false)
                .build();
    }

    @Test
    @DisplayName("toggleTwoFactor: Should update status and save user")
    void toggleTwoFactor_Success() {
        // Given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // When
        securityService.toggleTwoFactor(true, user);

        // Then
        assertThat(user.isTwoFactorEnabled()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("logoutAllDevices: Should increment token version")
    void logoutAllDevices_ShouldIncrementVersion() {
        // Given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        int initialVersion = user.getTokenVersion();

        // When
        securityService.logoutAllDevices(user);

        // Then
        assertThat(user.getTokenVersion()).isEqualTo(initialVersion + 1);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("validatePassword: Should return true if passwords match")
    void validatePassword_Match_ReturnsTrue() {
        // Given
        given(passwordEncoder.matches("rawPass", "encodedPass")).willReturn(true);

        // When
        boolean result = securityService.validatePassword("rawPass", user);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validatePassword: Should return false if passwords mismatch")
    void validatePassword_NoMatch_ReturnsFalse() {
        // Given
        given(passwordEncoder.matches("wrong", "encodedPass")).willReturn(false);

        // When
        boolean result = securityService.validatePassword("wrong", user);

        // Then
        assertThat(result).isFalse();
    }
}
