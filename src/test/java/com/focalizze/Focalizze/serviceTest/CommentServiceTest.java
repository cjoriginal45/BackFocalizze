package com.focalizze.Focalizze.serviceTest;
import com.focalizze.Focalizze.dto.CommentRequestDto;
import com.focalizze.Focalizze.dto.CommentResponseDto;

import com.focalizze.Focalizze.dto.mappers.CommentMapper;
import com.focalizze.Focalizze.dto.mappers.UserMapper;
import com.focalizze.Focalizze.models.CommentClass;
import com.focalizze.Focalizze.models.InteractionType;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;

import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.CommentRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.InteractionLimitService;
import com.focalizze.Focalizze.services.NotificationService;
import com.focalizze.Focalizze.services.servicesImpl.CommentServiceImpl;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
    @Mock private ThreadRepository threadRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private CommentMapper commentMapper;
    @Mock private InteractionLimitService interactionLimitService;
    @Mock private NotificationService notificationService;
    @Mock private BlockRepository blockRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    // Mocks para seguridad
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User currentUser;
    private User otherUser;
    private ThreadClass thread;
    private CommentClass comment;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        currentUser = User.builder().id(1L).username("user").build();
        otherUser = User.builder().id(2L).username("other").build();

        thread = new ThreadClass();
        thread.setId(10L);
        thread.setUser(otherUser);
        thread.setCommentCount(0);

        comment = CommentClass.builder()
                .id(100L)
                .content("Test")
                .user(currentUser)
                .thread(thread)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .replies(new ArrayList<>())
                .build();
    }


    // --- TEST: getCommentsByThread ---

    @Test
    @DisplayName("getComments: Si NO hay bloqueos, usa findActiveCommentsByThread")
    void getComments_NoBlocks_UsesSimpleQuery() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(currentUser);
        given(threadRepository.findById(10L)).willReturn(Optional.of(thread));

        // Simulamos que NO hay bloqueos
        given(userRepository.findBlockedUserIdsByBlocker(1L)).willReturn(Collections.emptySet());
        given(userRepository.findUserIdsWhoBlockedUser(1L)).willReturn(Collections.emptySet());

        Page<CommentClass> page = new PageImpl<>(List.of(comment));

        // --- CORRECCIÓN ---
        // Como la lista de bloqueos está vacía, el servicio llamará a 'findActiveCommentsByThread'.
        // Debemos mockear ESE método.
        given(commentRepository.findActiveCommentsByThread(
                eq(thread),
                any(Pageable.class)
        )).willReturn(page);

        // When
        commentService.getCommentsByThread(10L, Pageable.unpaged());

        // Then
        // Verificamos que se llamó al método simple, no al filtrado
        verify(commentRepository).findActiveCommentsByThread(
                eq(thread),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("getComments: Si HAY bloqueos, filtra los IDs correctos")
    void getComments_WithBlocks_FiltersIds() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(currentUser);
        given(threadRepository.findById(10L)).willReturn(Optional.of(thread));

        // Simulamos bloqueos
        given(userRepository.findBlockedUserIdsByBlocker(1L)).willReturn(Set.of(5L));
        given(userRepository.findUserIdsWhoBlockedUser(1L)).willReturn(Set.of(6L));

        given(commentRepository.findActiveRootCommentsByThreadAndFilterBlocked(any(), any(), any()))
                .willReturn(Page.empty());

        // When
        commentService.getCommentsByThread(10L, Pageable.unpaged());

        // Then
        // Verificamos que el Set contiene ambos IDs (5 y 6)
        verify(commentRepository).findActiveRootCommentsByThreadAndFilterBlocked(
                eq(thread),
                argThat(set -> set.contains(5L) && set.contains(6L)),
                any()
        );
    }

    // --- TEST: createComment ---

    @Test
    @DisplayName("createComment: Debería crear comentario si no hay bloqueo")
    void createComment_Success() {
        // Given
        CommentRequestDto request = new CommentRequestDto("Hola");
        given(threadRepository.findById(10L)).willReturn(Optional.of(thread));
        // No hay bloqueo entre currentUser y threadAuthor (otherUser)
        given(blockRepository.existsByBlockerAndBlocked(any(), any())).willReturn(false);
        given(commentRepository.save(any(CommentClass.class))).willReturn(comment);

        // When
        commentService.createComment(10L, request, currentUser);

        // Then
        verify(interactionLimitService).checkInteractionLimit(currentUser);
        verify(interactionLimitService).recordInteraction(currentUser, InteractionType.COMMENT);
        verify(notificationService).createAndSendNotification(eq(otherUser), any(), eq(currentUser), eq(thread));
        verify(commentRepository).save(any(CommentClass.class));
    }

    @Test
    @DisplayName("createComment: Debería lanzar excepción si hay bloqueo")
    void createComment_Blocked_ThrowsException() {
        // Given
        CommentRequestDto request = new CommentRequestDto("Hola");
        given(threadRepository.findById(10L)).willReturn(Optional.of(thread));
        // Simulamos bloqueo
        given(blockRepository.existsByBlockerAndBlocked(currentUser, otherUser)).willReturn(true);

        // When & Then
        assertThrows(AccessDeniedException.class, () ->
                commentService.createComment(10L, request, currentUser)
        );
        verify(commentRepository, never()).save(any());
    }

    // --- TEST: replyToComment (Lógica de Dueño del Hilo) ---

    @Test
    @DisplayName("replyToComment: Éxito si el currentUser es el DUEÑO del hilo")
    void replyToComment_OwnerReplying_Success() {
        // Given
        thread.setUser(currentUser); // Dueño = CurrentUser
        CommentClass parentComment = CommentClass.builder().id(50L).user(otherUser).thread(thread).build();
        CommentRequestDto request = new CommentRequestDto("Respuesta");

        given(commentRepository.findById(50L)).willReturn(Optional.of(parentComment));

        // Simulamos que el repositorio devuelve un objeto con replies = null (como hace Lombok por defecto)
        CommentClass savedReply = CommentClass.builder().id(51L).replies(null).build();
        given(commentRepository.save(any(CommentClass.class))).willReturn(savedReply);

        // --- CORRECCIÓN CLAVE ---
        // Mockeamos el mapper para que no falle después de la lógica que estamos probando.
        // Esto aísla el test al método del servicio.
        given(commentMapper.toCommentResponseDto(any())).willReturn(null);

        // When
        commentService.replyToComment(50L, request, currentUser);

        // Then
        verify(commentRepository).save(any(CommentClass.class));
        verify(interactionLimitService).recordInteraction(currentUser, InteractionType.COMMENT);

        // Verificamos que el servicio corrigió el NULL a una Lista Vacía
        assertThat(savedReply.getReplies()).isNotNull();
        assertThat(savedReply.getReplies()).isEmpty();
    }

    @Test
    @DisplayName("replyToComment: Fallo si un usuario ajeno intenta responder")
    void replyToComment_NonOwnerReplying_ThrowsAccessDenied() {
        // Given
        // El hilo pertenece a 'otherUser', pero 'currentUser' intenta responder
        thread.setUser(otherUser);
        CommentClass parentComment = CommentClass.builder().id(50L).user(currentUser).thread(thread).build();

        given(commentRepository.findById(50L)).willReturn(Optional.of(parentComment));

        // When & Then
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                commentService.replyToComment(50L, new CommentRequestDto("Intento responder"), currentUser)
        );

        assertThat(ex.getMessage()).contains("Solo el autor del hilo puede responder");
        verify(commentRepository, never()).save(any());
    }

    // --- TEST: deleteComment ---

    @Test
    @DisplayName("deleteComment: Debería marcar como borrado (soft delete) y reembolsar si es hoy")
    void deleteComment_Success() {
        // Given
        comment.setCreatedAt(LocalDateTime.now()); // Creado hoy
        given(commentRepository.findByIdAndUser(100L, currentUser)).willReturn(Optional.of(comment));

        // When
        commentService.deleteComment(100L, currentUser);

        // Then
        assertThat(comment.isDeleted()).isTrue();
        verify(commentRepository).save(comment);
        verify(interactionLimitService).refundInteraction(currentUser, InteractionType.COMMENT);
    }

    // --- TEST: editComment ---

    @Test
    @DisplayName("editComment: Debería actualizar contenido")
    void editComment_Success() {
        // Given
        CommentRequestDto request = new CommentRequestDto("Nuevo contenido");
        given(commentRepository.findByIdAndUser(100L, currentUser)).willReturn(Optional.of(comment));
        given(commentRepository.save(comment)).willReturn(comment);

        // When
        commentService.editComment(100L, request, currentUser);

        // Then
        assertThat(comment.getContent()).isEqualTo("Nuevo contenido");
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("editComment: No debería permitir editar si está borrado")
    void editComment_Deleted_ThrowsException() {
        // Given
        comment.setDeleted(true);
        given(commentRepository.findByIdAndUser(100L, currentUser)).willReturn(Optional.of(comment));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                commentService.editComment(100L, new CommentRequestDto("edit"), currentUser)
        );
    }
}
