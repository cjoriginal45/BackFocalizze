package com.focalizze.Focalizze.serviceTest;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.FeedServiceImpl;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FeedServiceTest {

    @Mock private ThreadRepository threadRepository;
    @Mock private ThreadEnricher threadEnricher;
    @Mock private UserRepository userRepository;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private FeedServiceImpl feedService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        // Inicializamos las listas para evitar NullPointerException en el servicio
        currentUser = User.builder()
                .id(1L)
                .username("currentUser")
                .following(new ArrayList<>())
                .followedCategories(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("getFeed: Debería generar feed correctamente con usuario logueado y sin bloqueos")
    void getFeed_Success_NoBlocks() {
        // Given
        // 1. Auth Mock
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("currentUser");
        given(userRepository.findByUsername("currentUser")).willReturn(Optional.of(currentUser));

        // 2. Simular Seguimientos
        User followedUser = User.builder().id(2L).build();
        CategoryClass followedCategory = new CategoryClass(); followedCategory.setId(10L);

        currentUser.getFollowing().add(Follow.builder().userFollowed(followedUser).build());
        currentUser.getFollowedCategories().add(CategoryFollow.builder().category(followedCategory).build());

        // 3. Simular Bloqueos (Vacíos)
        given(userRepository.findBlockedUserIdsByBlocker(1L)).willReturn(Collections.emptySet());
        given(userRepository.findUserIdsWhoBlockedUser(1L)).willReturn(Collections.emptySet());

        // 4. Simular Repositorio y Enriquecedor
        List<ThreadClass> threads = List.of(new ThreadClass());
        Page<ThreadClass> page = new PageImpl<>(threads);

        given(threadRepository.findFollowingFeed(anyList(), anyList(), eq(1L), anySet(), any(Pageable.class)))
                .willReturn(page);

        List<FeedThreadDto> enrichedDtos = List.of(new FeedThreadDto(1L, null, null, null, null, false, false, "Cat"));
        given(threadEnricher.enrichList(threads, currentUser)).willReturn(enrichedDtos);

        // When
        Page<FeedThreadDto> result = feedService.getFeed(Pageable.unpaged());

        // Then
        assertThat(result.getContent()).hasSize(1);

        // Verificamos que se pasaron los IDs correctos al repositorio
        verify(threadRepository).findFollowingFeed(
                argThat(list -> list.contains(2L)), // Followed User ID
                argThat(list -> list.contains(10L)), // Followed Category ID
                eq(1L), // Current User ID
                argThat(set -> set.contains(-1L)), // Blocked IDs (vacío -> -1L)
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("getFeed: Debería manejar listas vacías (añadir -1L) y unir bloqueos")
    void getFeed_EmptyFollows_WithBlocks() {
        // Given
        // 1. Auth
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("currentUser");
        given(userRepository.findByUsername("currentUser")).willReturn(Optional.of(currentUser));

        // 2. Seguimientos Vacíos (currentUser por defecto en setUp)

        // 3. Simular Bloqueos (Bidireccionales)
        // Yo bloqueé al ID 5, el ID 6 me bloqueó a mí.
        given(userRepository.findBlockedUserIdsByBlocker(1L)).willReturn(Set.of(5L));
        given(userRepository.findUserIdsWhoBlockedUser(1L)).willReturn(Set.of(6L));

        // 4. Repositorio
        given(threadRepository.findFollowingFeed(anyList(), anyList(), eq(1L), anySet(), any(Pageable.class)))
                .willReturn(Page.empty());
        given(threadEnricher.enrichList(anyList(), any())).willReturn(Collections.emptyList());

        // When
        feedService.getFeed(Pageable.unpaged());

        // Then
        verify(threadRepository).findFollowingFeed(
                argThat(list -> list.contains(-1L)), // Lista vacía -> -1L
                argThat(list -> list.contains(-1L)), // Lista vacía -> -1L
                eq(1L),
                // Verificamos que el Set de bloqueos contenga ambos IDs
                argThat(set -> set.contains(5L) && set.contains(6L)),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("getFeed: Debería lanzar excepción si el usuario no se encuentra")
    void getFeed_UserNotFound_ThrowsException() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("unknown");
        given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () ->
                feedService.getFeed(Pageable.unpaged())
        );
    }
}
