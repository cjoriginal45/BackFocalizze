package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.exceptions.DailyLimitExceededException;
import com.focalizze.Focalizze.models.InteractionLog;
import com.focalizze.Focalizze.models.InteractionType;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.InteractionLogRepository;
import com.focalizze.Focalizze.services.servicesImpl.InteractionLimitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InteractionLimitServiceTest {
    @Mock
    private InteractionLogRepository interactionLogRepository;

    @InjectMocks
    private InteractionLimitServiceImpl interactionLimitService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("user").build();
    }

    @Test
    @DisplayName("checkInteractionLimit: Should allow if limit not reached")
    void checkLimit_WithinLimit_ShouldPass() {
        // Given: Usuario no suspendido, 10 interacciones hoy (limite es 20)
        user.setSuspensionEndsAt(null);
        given(interactionLogRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class)))
                .willReturn(10L);

        // When
        interactionLimitService.checkInteractionLimit(user);

        // Then: No exception thrown
    }

    @Test
    @DisplayName("checkInteractionLimit: Deberia lanzar exception si se excedio del limite")
    void checkLimit_LimitExceeded_ThrowsException() {
        // Given: 20 interacciones hoy
        user.setSuspensionEndsAt(null);
        given(interactionLogRepository.countByUserAndCreatedAtAfter(eq(user), any(LocalDateTime.class)))
                .willReturn(20L);

        // When & Then
        assertThrows(DailyLimitExceededException.class, () ->
                interactionLimitService.checkInteractionLimit(user)
        );
    }

    @Test
    @DisplayName("checkInteractionLimit: Deberia lanzar exception si usuario esta suspendido")
    void checkLimit_Suspended_ThrowsException() {
        // Given: Usuario suspendido hasta mañana
        user.setSuspensionEndsAt(LocalDateTime.now().plusDays(1));

        // When & Then
        assertThrows(AccessDeniedException.class, () ->
                interactionLimitService.checkInteractionLimit(user)
        );
        // No debería ni consultar al repo si está suspendido
        verify(interactionLogRepository, never()).countByUserAndCreatedAtAfter(any(), any());
    }

    @Test
    @DisplayName("refundInteraction: Should delete most recent log")
    void refundInteraction_ShouldDeleteLog() {
        // Given
        InteractionLog logToDelete = new InteractionLog();
        logToDelete.setId(100L);

        // El repo devuelve una lista con 1 log
        given(interactionLogRepository.findLogsToRefund(eq(user), eq(InteractionType.LIKE), any(LocalDateTime.class)))
                .willReturn(List.of(logToDelete));

        // When
        interactionLimitService.refundInteraction(user, InteractionType.LIKE);

        // Then
        verify(interactionLogRepository).delete(logToDelete);
    }

    @Test
    @DisplayName("refundInteraction: Should do nothing if no logs found")
    void refundInteraction_NoLogs_ShouldDoNothing() {
        // Given: Lista vacía
        given(interactionLogRepository.findLogsToRefund(any(), any(), any()))
                .willReturn(Collections.emptyList());

        // When
        interactionLimitService.refundInteraction(user, InteractionType.LIKE);

        // Then
        verify(interactionLogRepository, never()).delete(any());
    }
}
