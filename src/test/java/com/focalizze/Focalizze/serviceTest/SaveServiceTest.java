package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.SavedThreads;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.SavedThreadRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.SaveServiceImpl;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SaveServiceTest {
    @Mock private ThreadRepository threadRepository;
    @Mock private SavedThreadRepository savedThreadRepository;
    @Mock private ThreadEnricher threadEnricher;
    @Mock private UserRepository userRepository;
    @Mock private BlockRepository blockRepository;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private SaveServiceImpl saveService;

    private User currentUser;
    private ThreadClass thread;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("user").build();
        thread = new ThreadClass();
        thread.setId(10L);
        thread.setSaveCount(0);
    }

    // --- toggleSave ---

    @Test
    @DisplayName("toggleSave: Should save if not saved")
    void toggleSave_Save_Success() {
        // Given
        given(threadRepository.findById(10L)).willReturn(Optional.of(thread));
        given(savedThreadRepository.findByUserAndThread(currentUser, thread)).willReturn(Optional.empty());

        // When
        saveService.toggleSave(10L, currentUser);

        // Then
        verify(savedThreadRepository).save(any(SavedThreads.class));
        assertThat(thread.getSaveCount()).isEqualTo(1); // Incrementado
        verify(threadRepository).save(thread);
    }

    @Test
    @DisplayName("toggleSave: Should delete if already saved")
    void toggleSave_Unsave_Success() {
        // Given
        thread.setSaveCount(1);
        SavedThreads existingSave = new SavedThreads();
        given(threadRepository.findById(10L)).willReturn(Optional.of(thread));
        given(savedThreadRepository.findByUserAndThread(currentUser, thread)).willReturn(Optional.of(existingSave));

        // When
        saveService.toggleSave(10L, currentUser);

        // Then
        verify(savedThreadRepository).delete(existingSave);
        assertThat(thread.getSaveCount()).isEqualTo(0); // Decrementado
        verify(threadRepository).save(thread);
    }

    @Test
    @DisplayName("toggleSave: Should throw exception if thread not found")
    void toggleSave_NotFound_ThrowsException() {
        given(threadRepository.findById(99L)).willReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                saveService.toggleSave(99L, currentUser)
        );
    }

    // --- getSavedThreadsForCurrentUser ---

    @Test
    @DisplayName("getSavedThreads: Should filter out blocked users")
    void getSavedThreads_WithBlocks_ShouldFilter() {
        // Given
        SecurityContextHolder.setContext(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(currentUser);

        // Hilos guardados: Uno de un usuario normal (ID 2), otro de un bloqueado (ID 99)
        User normalUser = User.builder().id(2L).build();
        User blockedUser = User.builder().id(99L).build();

        ThreadClass thread1 = new ThreadClass(); thread1.setId(100L); thread1.setUser(normalUser);
        ThreadClass thread2 = new ThreadClass(); thread2.setId(200L); thread2.setUser(blockedUser);

        SavedThreads s1 = SavedThreads.builder().thread(thread1).build();
        SavedThreads s2 = SavedThreads.builder().thread(thread2).build();

        Page<SavedThreads> page = new PageImpl<>(List.of(s1, s2));
        given(savedThreadRepository.findByUserOrderByCreatedAtDesc(eq(currentUser), any(Pageable.class)))
                .willReturn(page);

        // Simulamos bloqueo
        given(blockRepository.findBlockedUserIdsByBlocker(1L)).willReturn(Set.of(99L));
        given(blockRepository.findUserIdsWhoBlockedUser(1L)).willReturn(Collections.emptySet());

        // Mock enriquecedor
        given(threadEnricher.enrichList(anyList(), eq(currentUser))).willReturn(List.of(new FeedThreadDto(100L, null, null, null, null, false, true, "Cat", null)));

        // When
        Page<FeedThreadDto> result = saveService.getSavedThreadsForCurrentUser(Pageable.unpaged());

        // Then
        // Verificamos que al enriquecedor solo llegó el hilo 1 (el no bloqueado)
        verify(threadEnricher).enrichList(
                argThat(list -> list.size() == 1 && list.get(0).getId().equals(100L)),
                eq(currentUser)
        );

        assertThat(result.getContent()).hasSize(1);
    }
}
