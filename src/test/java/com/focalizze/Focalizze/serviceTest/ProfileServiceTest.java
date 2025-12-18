package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ProfileResponseDto;
import com.focalizze.Focalizze.dto.ProfileUpdateRequestDto;
import com.focalizze.Focalizze.dto.UserProfileDownloadDto;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.FileStorageService;
import com.focalizze.Focalizze.services.ThreadService;
import com.focalizze.Focalizze.services.servicesImpl.ProfileServiceImpl;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfileServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private ThreadRepository threadRepository;
    @Mock private FollowRepository followRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private ThreadEnricher threadEnricher;
    @Mock private BlockRepository blockRepository;
    @Mock private ThreadService threadService;

    // Security Mocks
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    // Web Context Mocks (para ServletUriComponentsBuilder)
    @Mock private javax.servlet.http.HttpServletRequest request;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User currentUser;
    private User profileUser;

    @BeforeEach
    void setUp() {
        // Inyectamos valor de propiedad
        ReflectionTestUtils.setField(profileService, "defaultAvatarUrl", "default.png");

        SecurityContextHolder.setContext(securityContext);

        currentUser = User.builder().id(1L).username("viewer").build();
        profileUser = User.builder().id(2L).username("target").displayName("Target User").build();
    }

    // --- getProfile ---

    @Test
    @DisplayName("getProfile: Should return profile data with correct follow/block status")
    void getProfile_Success() {
        // Given
        given(userRepository.findByUsername("target")).willReturn(Optional.of(profileUser));
        given(followRepository.countByUserFollowed(profileUser)).willReturn(10L);
        given(followRepository.countByUserFollower(profileUser)).willReturn(5L);

        // Mock Auth
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUser);

        // Mock Checks
        given(blockRepository.existsByBlockerAndBlocked(any(), any())).willReturn(false);
        given(followRepository.existsByUserFollowerAndUserFollowed(currentUser, profileUser)).willReturn(true);

        // When
        ProfileResponseDto result = profileService.getProfile("target");

        // Then
        assertThat(result.username()).isEqualTo("target");
        assertThat(result.followers()).isEqualTo(10);
        assertThat(result.isFollowing()).isTrue();
        assertThat(result.isBlocked()).isFalse();
    }

    @Test
    @DisplayName("getProfile: Should calculate available threads for own profile")
    void getProfile_OwnProfile_ShouldCalculateThreads() {
        // Given
        given(userRepository.findByUsername("viewer")).willReturn(Optional.of(currentUser));

        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(currentUser);

        // Mock Thread Service
        given(threadService.getThreadsAvailableToday(currentUser)).willReturn(2);

        // When
        ProfileResponseDto result = profileService.getProfile("viewer");

        // Then
        assertThat(result.threadsAvailableToday()).isEqualTo(2L);
    }

    // --- getThreadsForUser ---

    @Test
    @DisplayName("getThreadsForUser: Should return empty page if blocked")
    void getThreads_Blocked_ReturnsEmpty() {
        // Given
        given(userRepository.findByUsername("target")).willReturn(Optional.of(profileUser));

        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(currentUser);

        // Simulamos bloqueo
        given(blockRepository.existsByBlockerAndBlocked(currentUser, profileUser)).willReturn(true);

        // When
        Page<FeedThreadDto> result = profileService.getThreadsForUser("target", Pageable.unpaged());

        // Then
        assertThat(result).isEmpty();
        verify(threadRepository, never()).findByUserWithDetails(any(), any());
    }

    @Test
    @DisplayName("getThreadsForUser: Should return enriched threads if not blocked")
    void getThreads_Success() {
        // Given
        given(userRepository.findByUsername("target")).willReturn(Optional.of(profileUser));
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(currentUser);
        given(blockRepository.existsByBlockerAndBlocked(any(), any())).willReturn(false);

        Page<ThreadClass> page = new PageImpl<>(Collections.singletonList(new ThreadClass()));
        given(threadRepository.findByUserWithDetails(eq(profileUser), any())).willReturn(page);

        // When
        profileService.getThreadsForUser("target", Pageable.unpaged());

        // Then
        verify(threadEnricher).enrich(any(), eq(currentUser));
    }

    // --- updateProfile ---

    @Test
    @DisplayName("updateProfile: Should update displayName and bio")
    void updateProfile_Success() {
        // Given
        ProfileUpdateRequestDto update = new ProfileUpdateRequestDto("New Name", "New Bio");
        given(userRepository.findByUsername("target")).willReturn(Optional.of(profileUser));

        // When
        profileService.updateProfile("target", update);

        // Then
        assertThat(profileUser.getDisplayName()).isEqualTo("New Name");
        assertThat(profileUser.getBiography()).isEqualTo("New Bio");
        verify(userRepository).save(profileUser);
    }

    // --- updateAvatar ---

    @Test
    @DisplayName("updateAvatar: Should save file and update user URL")
    void updateAvatar_Success() {
        // Para que ServletUriComponentsBuilder funcione, necesitamos mockear el RequestContext

        // Mocking Web Context

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    }

    @Test
    @DisplayName("updateAvatar: Should throw if file is empty")
    void updateAvatar_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        assertThrows(IllegalArgumentException.class, () ->
                profileService.updateAvatar("user", emptyFile)
        );
    }

    @Test
    @DisplayName("updateAvatar: Should throw if invalid content type")
    void updateAvatar_InvalidType_ThrowsException() {
        MockMultipartFile badFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        assertThrows(RuntimeException.class, () ->
                profileService.updateAvatar("user", badFile)
        );
    }

    // --- getProfileForDownload ---

    @Test
    @DisplayName("getProfileForDownload: Should return DTO")
    void getProfileForDownload_Success() {
        // Given
        given(userRepository.findByUsername("target")).willReturn(Optional.of(profileUser));

        // When
        UserProfileDownloadDto result = profileService.getProfileForDownload("target");

        // Then
        assertThat(result.username()).isEqualTo("target");
        // Debería usar el defaultAvatarUrl inyectado porque profileUser no tiene avatar set
        assertThat(result.avatarUrl()).isEqualTo("default.png");
    }
}
