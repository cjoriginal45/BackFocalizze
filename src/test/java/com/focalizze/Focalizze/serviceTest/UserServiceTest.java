package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.UpdateThemeDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.UserServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private FollowRepository followRepository;
    @Mock private BlockRepository blockRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Inyectar el valor de @Value manualmente
        ReflectionTestUtils.setField(userService, "defaultAvatarUrl", "default.png");

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@email.com")
                .role(UserRole.USER)
                .build();
    }

    // --- Tests de Búsqueda Básica ---

    @Test
    @DisplayName("findUserByUserName: Should return user if exists")
    void findUserByUserName_Found() {
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
        assertThat(userService.findUserByUserName("testuser")).isPresent();
    }

    @Test
    @DisplayName("findUserByEmail: Should return user if exists")
    void findUserByEmail_Found() {
        given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(testUser));
        assertThat(userService.findUserByEmail("test@email.com")).isPresent();
    }

    // --- Tests de Validación ---

    @Test
    @DisplayName("validateEmail: Should return true for valid emails")
    void validateEmail_Valid_ReturnsTrue() {
        assertThat(userService.validateEmail("user@domain.com")).isTrue();
        assertThat(userService.validateEmail("name.surname@company.co.uk")).isTrue();
    }

    @Test
    @DisplayName("validateEmail: Should return false for invalid emails")
    void validateEmail_Invalid_ReturnsFalse() {
        assertThat(userService.validateEmail("invalid-email")).isFalse();
        assertThat(userService.validateEmail("user@domain")).isFalse();
        assertThat(userService.validateEmail(null)).isFalse();
        assertThat(userService.validateEmail("")).isFalse();
    }

    // --- Tests de getUserProfile (Lógica compleja) ---

    @Test
    @DisplayName("getUserProfile: Should throw exception if user not found")
    void getUserProfile_NotFound_ThrowsException() {
        given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                userService.getUserProfile("unknown", null)
        );
    }

    @Test
    @DisplayName("getUserProfile: Should return DTO with false flags if currentUser is NULL (Guest)")
    void getUserProfile_Guest_ShouldReturnBaseProfile() {
        // Given
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

        // When
        UserDto result = userService.getUserProfile("testuser", null);

        // Then
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.isFollowing()).isFalse();
        assertThat(result.isBlocked()).isFalse();

        // Verificar que no se llamaron los repositorios de interacción
        verify(followRepository, never()).existsByUserFollowerAndUserFollowed(any(), any());
    }

    @Test
    @DisplayName("getUserProfile: Should return false flags if viewing OWN profile")
    void getUserProfile_SelfView_ShouldReturnBaseProfile() {
        // Given
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

        // When (currentUser tiene el mismo ID que testUser)
        UserDto result = userService.getUserProfile("testuser", testUser);

        // Then
        assertThat(result.isFollowing()).isFalse();
        assertThat(result.isBlocked()).isFalse();

        verify(followRepository, never()).existsByUserFollowerAndUserFollowed(any(), any());
    }

    @Test
    @DisplayName("getUserProfile: Should check interaction status if viewing OTHER profile")
    void getUserProfile_OtherUser_ShouldCheckInteractions() {
        // Given
        User otherUser = User.builder().id(99L).username("other").build();

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

        // Simulamos que lo sigue y lo tiene bloqueado (caso raro pero posible para testear la lógica)
        given(followRepository.existsByUserFollowerAndUserFollowed(otherUser, testUser)).willReturn(true);
        given(blockRepository.existsByBlockerAndBlocked(otherUser, testUser)).willReturn(true);

        // When
        UserDto result = userService.getUserProfile("testuser", otherUser);

        // Then
        assertThat(result.isFollowing()).isTrue();
        assertThat(result.isBlocked()).isTrue();
    }

    // --- Tests de updateThemePreferences ---

    @Test
    @DisplayName("updateThemePreferences: Should update user fields and save")
    void updateThemePreferences_Success() {
        // Given
        UpdateThemeDto dto = new UpdateThemeDto("DARK", "#000000");
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

        // When
        userService.updateThemePreferences("testuser", dto);

        // Then
        assertThat(testUser.getBackgroundType()).isEqualTo("DARK");
        assertThat(testUser.getBackgroundValue()).isEqualTo("#000000");
        verify(userRepository).save(testUser);
    }
}
