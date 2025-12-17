package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.MentionRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.NotificationService;
import com.focalizze.Focalizze.services.servicesImpl.MentionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MentionServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private MentionRepository mentionRepository;
    @Mock private NotificationService notificationService;
    @Mock private BlockRepository blockRepository;

    @InjectMocks
    private MentionServiceImpl mentionService;

    private User author;
    private User mentionedUser;
    private Post post;

    @BeforeEach
    void setUp() {
        author = User.builder().id(1L).username("author").build();
        mentionedUser = User.builder().id(2L).username("mentioned").build();

        ThreadClass thread = new ThreadClass();
        thread.setId(10L);

        post = Post.builder()
                .id(100L)
                .content("Hola @mentioned, como estas?")
                .thread(thread)
                .build();
    }

    @Test
    @DisplayName("processMentions: Should create mention and notify valid user")
    void processMentions_Valid_ShouldProcess() {
        // Given
        // El repositorio encuentra al usuario por el username extraído del regex
        given(userRepository.findAllByUsernameIn(any())).willReturn(List.of(mentionedUser));
        // No hay bloqueo
        given(blockRepository.existsByBlockerAndBlocked(any(), any())).willReturn(false);

        // When
        mentionService.processMentions(post, author);

        // Then
        verify(mentionRepository).save(any(Mention.class));
        verify(notificationService).createAndSendNotification(
                eq(mentionedUser), eq(NotificationType.MENTION), eq(author), eq(post.getThread())
        );
    }

    @Test
    @DisplayName("processMentions: Should ignore self-mentions")
    void processMentions_SelfMention_ShouldIgnore() {
        // Given
        post.setContent("Nota para mi: @author");
        // El repo devuelve al mismo autor
        given(userRepository.findAllByUsernameIn(any())).willReturn(List.of(author));

        // When
        mentionService.processMentions(post, author);

        // Then
        verify(mentionRepository, never()).save(any());
        verify(notificationService, never()).createAndSendNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("processMentions: Should ignore blocked users")
    void processMentions_Blocked_ShouldIgnore() {
        // Given
        given(userRepository.findAllByUsernameIn(any())).willReturn(List.of(mentionedUser));

        // Simulamos bloqueo (el autor bloqueó al mencionado, o viceversa)
        given(blockRepository.existsByBlockerAndBlocked(author, mentionedUser)).willReturn(true);

        // When
        mentionService.processMentions(post, author);

        // Then
        verify(mentionRepository, never()).save(any());
        verify(notificationService, never()).createAndSendNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("processMentions: Should do nothing if no mentions in text")
    void processMentions_NoText_ShouldDoNothing() {
        // Given
        post.setContent("Texto sin menciones");

        // When
        mentionService.processMentions(post, author);

        // Then
        // Ni siquiera debería llamar a la base de datos para buscar usuarios
        verify(userRepository, never()).findAllByUsernameIn(any());
    }

    @Test
    @DisplayName("processMentions: Should do nothing if mentioned user does not exist")
    void processMentions_UserNotFound_ShouldDoNothing() {
        // Given
        post.setContent("Hola @fantasma");
        // El repo devuelve lista vacía
        given(userRepository.findAllByUsernameIn(any())).willReturn(Collections.emptyList());

        // When
        mentionService.processMentions(post, author);

        // Then
        verify(mentionRepository, never()).save(any());
    }
}
