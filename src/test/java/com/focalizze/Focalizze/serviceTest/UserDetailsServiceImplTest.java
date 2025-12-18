package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@email.com")
                .password("password")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("loadUserByUsername: Should return UserDetails when user exists")
    void loadUserByUsername_WhenExists_ShouldReturnUser() {
        // Given
        String identifier = "testuser";
        given(userRepository.findByUsernameOrEmail(identifier, identifier)).willReturn(Optional.of(user));

        // When
        UserDetails result = userDetailsService.loadUserByUsername(identifier);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @DisplayName("loadUserByUsername: Should throw exception when user does not exist")
    void loadUserByUsername_WhenNotExists_ShouldThrowException() {
        // Given
        String identifier = "unknown";
        given(userRepository.findByUsernameOrEmail(identifier, identifier)).willReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername(identifier)
        );
    }
}
