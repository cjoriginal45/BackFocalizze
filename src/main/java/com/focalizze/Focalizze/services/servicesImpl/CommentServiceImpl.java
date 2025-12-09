package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.CommentRequestDto;
import com.focalizze.Focalizze.dto.CommentResponseDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.dto.mappers.CommentMapper;
import com.focalizze.Focalizze.dto.mappers.UserMapper;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.CommentRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.CommentService;
import com.focalizze.Focalizze.services.InteractionLimitService;
import com.focalizze.Focalizze.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl  implements CommentService {
    private final ThreadRepository threadRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final InteractionLimitService interactionLimitService;
    private final NotificationService notificationService;
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getCommentsByThread(Long threadId, Pageable pageable) {
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread no encontrado"));

        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Set<Long> blockedByCurrentUser = userRepository.findBlockedUserIdsByBlocker(currentUser.getId());
        Set<Long> whoBlockedCurrentUser = userRepository.findUserIdsWhoBlockedUser(currentUser.getId());

        Set<Long> allBlockedIds = new HashSet<>();
        allBlockedIds.addAll(blockedByCurrentUser);
        allBlockedIds.addAll(whoBlockedCurrentUser);

        Page<CommentClass> comments;
        if (allBlockedIds.isEmpty()) {
            comments = commentRepository.findActiveCommentsByThread(thread, pageable);
        } else {
            comments = commentRepository.findActiveCommentsByThreadAndFilterBlocked(thread, allBlockedIds, pageable);
        }

        return comments.map(commentMapper::toCommentResponseDto);
    }

    @Override
    @Transactional
    public CommentResponseDto createComment(Long threadId, CommentRequestDto commentRequestDto, User currentUser) {
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread no encontrado"));

        User threadAuthor = thread.getUser();
        boolean isBlocked = blockRepository.existsByBlockerAndBlocked(currentUser, threadAuthor) ||
                blockRepository.existsByBlockerAndBlocked(threadAuthor, currentUser);

        if (isBlocked) {
            throw new AccessDeniedException("No puedes comentar en este hilo debido a una restricción de bloqueo.");
        }

        // Verificar si el usuario puede comentar.
        interactionLimitService.checkInteractionLimit(currentUser);

        LocalDateTime date = LocalDateTime.now();

        CommentClass newComment = CommentClass.builder()
                .content(commentRequestDto.content())
                .createdAt(date)
                .user(currentUser)
                .thread(thread)
                .build();

        CommentClass savedComment = commentRepository.save(newComment);

        thread.setCommentCount(thread.getCommentCount() + 1);

        // Registramos la interacción DESPUÉS de guardar el comentario.
        interactionLimitService.recordInteraction(currentUser, InteractionType.COMMENT);

        if (!thread.getUser().getId().equals(currentUser.getId())) {
            notificationService.createAndSendNotification(
                    thread.getUser(),
                    NotificationType.NEW_COMMENT,
                    currentUser,
                    thread
            );
        }

        threadRepository.save(thread);

        return commentMapper.toCommentResponseDto(savedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, User currentUser) {
        // 1. Verificar que el comentario existe Y que pertenece al usuario actual.
        CommentClass commentToDelete = commentRepository.findByIdAndUser(commentId, currentUser)
                .orElseThrow(() -> new RuntimeException("Comentario no encontrado o no tienes permiso para eliminarlo. ID: " + commentId));

        // 2. Obtener el hilo asociado.
        ThreadClass thread = commentToDelete.getThread();

        // 3. CAMBIO: Marcar como eliminado en lugar de borrar.
        commentToDelete.setDeleted(true);
        commentRepository.save(commentToDelete);

        // 4. Actualizar el contador de comentarios en el hilo.
        thread.setCommentCount(Math.max(0, thread.getCommentCount() - 1));
        threadRepository.save(thread);

        // 5. Lógica de "reembolso" de interacción.
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        if (commentToDelete.getCreatedAt().isAfter(startOfToday)) {
            interactionLimitService.refundInteraction(currentUser, InteractionType.COMMENT);
        }
    }

    @Override
    @Transactional
    public CommentResponseDto editComment(Long commentId, CommentRequestDto commentRequestDto, User currentUser) {
        // Buscar comentario y verificar dueño
        CommentClass commentToEdit = commentRepository.findByIdAndUser(commentId, currentUser)
                .orElseThrow(() -> new RuntimeException("Comentario no encontrado o no tienes permiso para eliminarlo. ID: " + commentId));

        // Verificar si está borrado lógicamente
        if (commentToEdit.isDeleted()) {
            throw new RuntimeException("No se puede editar un comentario que ha sido eliminado.");
        }

        // Actualizar contenido
        commentToEdit.setContent(commentRequestDto.content());
        CommentClass savedComment = commentRepository.save(commentToEdit);

        // Devolver el DTO mapeado desde la entidad real
        return commentMapper.toCommentResponseDto(savedComment);
    }
}