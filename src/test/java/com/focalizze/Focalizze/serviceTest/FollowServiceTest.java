package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.UserSummaryDto;
import com.focalizze.Focalizze.models.Follow;
import com.focalizze.Focalizze.models.NotificationType;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.NotificationService;
import com.focalizze.Focalizze.services.servicesImpl.FollowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FollowServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private FollowRepository followRepository;
    @Mock private NotificationService notificationService;
    @Mock private BlockRepository blockRepository;

    @InjectMocks
    private FollowServiceImpl followService;

    private User currentUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("current").followingCount(0).build();
        targetUser = User.builder().id(2L).username("target").followersCount(0).build();
    }

    @Test
    @DisplayName("toggleFollowUser: Deberia seguir usuario si no lo sigue y no esta bloqueado")
    void toggle_Follow_Success() {
        // Given
        given(userRepository.findByUsername("target")).willReturn(Optional.of(targetUser));
        given(blockRepository.existsByBlockerAndBlocked(any(), any())).willReturn(false); // No bloqueos
        given(followRepository.findByUserFollowerAndUserFollowed(currentUser, targetUser))
                .willReturn(Optional.empty()); // No sigue actualmente

        // When
        followService.toggleFollowUser("target", currentUser);

        // Then
        verify(followRepository).save(any(Follow.class));
        verify(userRepository).incrementFollowingCount(currentUser.getId());
        verify(userRepository).incrementFollowersCount(targetUser.getId());
        verify(notificationService).createAndSendNotification(eq(targetUser), eq(NotificationType.NEW_FOLLOWER), eq(currentUser), isNull());
    }

    @Test
    @DisplayName("toggleFollowUser: Deberia dejar de seguir usuario si lo esta siguiendo")
    void toggle_Unfollow_Success() {
        // Given
        Follow existingFollow = Follow.builder().userFollower(currentUser).userFollowed(targetUser).build();

        given(userRepository.findByUsername("target")).willReturn(Optional.of(targetUser));
        given(blockRepository.existsByBlockerAndBlocked(any(), any())).willReturn(false);
        given(followRepository.findByUserFollowerAndUserFollowed(currentUser, targetUser))
                .willReturn(Optional.of(existingFollow));

        // When
        followService.toggleFollowUser("target", currentUser);

        // Then
        verify(followRepository).delete(existingFollow);
        verify(userRepository).decrementFollowingCount(currentUser.getId());
        verify(userRepository).decrementFollowersCount(targetUser.getId());
    }

    @Test
    @DisplayName("toggleFollowUser: Deberia lanzar excepcion usuario bloqueado")
    void toggle_Blocked_ThrowsException() {
        // Given
        given(userRepository.findByUsername("target")).willReturn(Optional.of(targetUser));
        // Simulamos bloqueo
        given(blockRepository.existsByBlockerAndBlocked(currentUser, targetUser)).willReturn(true);

        // When & Then
        assertThrows(AccessDeniedException.class, () ->
                followService.toggleFollowUser("target", currentUser)
        );
        verify(followRepository, never()).save(any());
    }

    @Test
    @DisplayName("getFollowers: Deberia retornar una lista mapeada a DTO")
    void getFollowers_ShouldReturnList() {
        // Given
        List<User> followers = List.of(User.builder().id(3L).username("follower1").build());
        given(followRepository.findFollowersByUsername("target")).willReturn(followers);

        // Simular que currentUser sigue a ese follower
        given(followRepository.findFollowedUserIdsByFollower(eq(currentUser), anySet())).willReturn(Set.of(3L));

        // When
        List<UserSummaryDto> result = followService.getFollowers("target", currentUser);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("follower1");
        assertThat(result.get(0).isFollowing()).isTrue();
    }
}
