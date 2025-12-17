package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.NotificationDto;
import com.focalizze.Focalizze.dto.mappers.NotificationMapper;
import com.focalizze.Focalizze.models.NotificationClass;
import com.focalizze.Focalizze.models.NotificationType;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.NotificationRepository;
import com.focalizze.Focalizze.services.servicesImpl.NotificationServiceImpl;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {
    @Mock private NotificationRepository notificationRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private NotificationMapper notificationMapper;
    @Mock private BlockRepository blockRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User recipient;
    private User sender;
    private ThreadClass thread;

    @BeforeEach
    void setUp() {
        recipient = User.builder().id(1L).username("recipient").build();
        sender = User.builder().id(2L).username("sender").build();
        thread = new ThreadClass();
        thread.setId(10L);
    }

    @Test
    @DisplayName("createAndSend: Should save and send WS message if NOT blocked")
    void createAndSend_Success() {
        // Given
        // No hay bloqueo
        given(blockRepository.existsByBlockerAndBlocked(any(), any())).willReturn(false);

        // Mock del mapper
        NotificationDto mockDto = new NotificationDto(1L, "Msg", null, false, null, null, null,null);
        given(notificationMapper.toDto(any(NotificationClass.class))).willReturn(mockDto);

        // When
        notificationService.createAndSendNotification(recipient, NotificationType.NEW_LIKE, sender, thread);

        // Then
        // 1. Se guarda en BD
        verify(notificationRepository).save(any(NotificationClass.class));

        // 2. Se envía por WebSocket al usuario correcto
        verify(messagingTemplate).convertAndSendToUser(
                eq("recipient"),
                eq("/queue/notifications"),
                eq(mockDto)
        );
    }

    @Test
    @DisplayName("createAndSend: Should DO NOTHING if blocked")
    void createAndSend_Blocked_ShouldAbort() {
        // Given
        // Simulamos bloqueo
        given(blockRepository.existsByBlockerAndBlocked(recipient, sender)).willReturn(true);

        // When
        notificationService.createAndSendNotification(recipient, NotificationType.NEW_LIKE, sender, thread);

        // Then
        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("getNotificationsForUser: Should return mapped page")
    void getNotifications_ShouldReturnPage() {
        // Given
        Page<NotificationClass> page = new PageImpl<>(Collections.emptyList());
        given(notificationRepository.findByUserWithDetails(eq(recipient), any(Pageable.class)))
                .willReturn(page);

        // When
        notificationService.getNotificationsForUser(recipient, Pageable.unpaged());

        // Then
        verify(notificationRepository).findByUserWithDetails(eq(recipient), any(Pageable.class));
    }

    @Test
    @DisplayName("markAllAsRead: Should call repository update")
    void markAllAsRead_ShouldCallRepo() {
        // When
        notificationService.markAllAsRead(recipient);

        // Then
        verify(notificationRepository).markAllAsReadForUser(recipient.getId());
    }
}
