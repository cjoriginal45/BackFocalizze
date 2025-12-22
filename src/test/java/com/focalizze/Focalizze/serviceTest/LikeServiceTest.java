package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.InteractionLogRepository;
import com.focalizze.Focalizze.repository.LikeRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.InteractionLimitService;
import com.focalizze.Focalizze.services.NotificationService;
import com.focalizze.Focalizze.services.servicesImpl.LikeServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LikeServiceTest {
    @Mock private ThreadRepository threadRepository;
    @Mock private LikeRepository likeRepository;
    @Mock private InteractionLimitService interactionLimitService;
    @Mock private NotificationService notificationService;
    @Mock private InteractionLogRepository interactionLogRepository;

    @InjectMocks
    private LikeServiceImpl likeService;

    private User currentUser;
    private User authorUser;
    private ThreadClass thread;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("user").build();
        authorUser = User.builder().id(2L).username("author").build();

        thread = new ThreadClass();
        thread.setId(100L);
        thread.setUser(authorUser);
        thread.setLikeCount(10);
    }

    @Test
    @DisplayName("toggleLike: Should add like, increment count, record interaction and notify")
    void toggleLike_WhenNotLiked_ShouldAddLike() {
        // Given
        given(threadRepository.findById(100L)).willReturn(Optional.of(thread));
        given(likeRepository.findByUserAndThread(currentUser, thread)).willReturn(Optional.empty());

        // When
        likeService.toggleLike(100L, currentUser);

        // Then
        verify(interactionLimitService).checkInteractionLimit(currentUser);
        verify(likeRepository).save(any(Like.class));
        verify(interactionLimitService).recordInteraction(currentUser, InteractionType.LIKE);

        // Verificar incremento
        assertThat(thread.getLikeCount()).isEqualTo(11);
        verify(threadRepository).save(thread);

        // Verificar notificación (porque no es el dueño)
        verify(notificationService).createAndSendNotification(
                eq(authorUser), eq(NotificationType.NEW_LIKE), eq(currentUser), eq(thread)
        );
    }

    @Test
    @DisplayName("toggleLike: Should NOT notify if user likes their own thread")
    void toggleLike_SelfLike_ShouldNotNotify() {
        // Given
        thread.setUser(currentUser); // Dueño = CurrentUser

        given(threadRepository.findById(100L)).willReturn(Optional.of(thread));
        given(likeRepository.findByUserAndThread(currentUser, thread)).willReturn(Optional.empty());

        // When
        likeService.toggleLike(100L, currentUser);

        // Then
        verify(likeRepository).save(any(Like.class));
        verify(notificationService, never()).createAndSendNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("toggleLike: Should remove like (Undo) and REFUND if created today")
    void toggleLike_RemoveToday_ShouldRefund() {
        // Given
        Like existingLike = Like.builder()
                .user(currentUser)
                .thread(thread)
                .createdAt(LocalDateTime.now()) // Creado HOY
                .build();

        given(threadRepository.findById(100L)).willReturn(Optional.of(thread));
        given(likeRepository.findByUserAndThread(currentUser, thread)).willReturn(Optional.of(existingLike));

        // When
        likeService.toggleLike(100L, currentUser);

        // Then
        verify(likeRepository).delete(existingLike);

        // Verificar decremento
        assertThat(thread.getLikeCount()).isEqualTo(9);

        // Verificar Reembolso
        verify(interactionLimitService).refundInteraction(currentUser, InteractionType.LIKE);
    }

    @Test
    @DisplayName("toggleLike: Should remove like but NOT REFUND if created yesterday")
    void toggleLike_RemoveYesterday_ShouldNotRefund() {
        // Given
        Like oldLike = Like.builder()
                .user(currentUser)
                .thread(thread)
                .createdAt(LocalDateTime.now().minusDays(1)) // Creado AYER
                .build();

        given(threadRepository.findById(100L)).willReturn(Optional.of(thread));
        given(likeRepository.findByUserAndThread(currentUser, thread)).willReturn(Optional.of(oldLike));

        // When
        likeService.toggleLike(100L, currentUser);

        // Then
        verify(likeRepository).delete(oldLike);
        // NO debe haber reembolso
        verify(interactionLimitService, never()).refundInteraction(any(), any());
    }

    @Test
    @DisplayName("toggleLike: Should throw exception if thread not found")
    void toggleLike_NotFound_ThrowsException() {
        given(threadRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                likeService.toggleLike(999L, currentUser)
        );
    }
}
