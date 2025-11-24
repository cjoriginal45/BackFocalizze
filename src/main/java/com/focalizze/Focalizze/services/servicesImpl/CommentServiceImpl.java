package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.CommentRequestDto;
import com.focalizze.Focalizze.dto.CommentResponseDto;
import com.focalizze.Focalizze.dto.mappers.CommentMapper;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.CommentRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.CommentService;
import com.focalizze.Focalizze.services.InteractionLimitService;
import com.focalizze.Focalizze.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl  implements CommentService {
    private final ThreadRepository threadRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final InteractionLimitService interactionLimitService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getCommentsByThread(Long threadId, Pageable pageable) {
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread no encontrado"));

        Page<CommentClass> comments = commentRepository.findActiveCommentsByThread(thread, pageable);
        return comments.map(commentMapper::toCommentResponseDto);
    }

    @Override
    @Transactional
    public CommentResponseDto createComment(Long threadId, CommentRequestDto commentRequestDto, User currentUser) {
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread no encontrado"));

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
        CommentClass commentToDelete = commentRepository.findByIdAndUser(commentId, currentUser)
                .orElseThrow(() -> new RuntimeException("Error..."));

        // ... (lógica de borrado lógico y thread count) ...
        commentToDelete.setDeleted(true);
        commentRepository.save(commentToDelete);

        ThreadClass thread = commentToDelete.getThread();
        thread.setCommentCount(Math.max(0, thread.getCommentCount() - 1));
        threadRepository.save(thread);

        // --- DIAGNÓSTICO DE FECHAS ---
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        System.out.println("--- BORRANDO COMENTARIO ---");
        System.out.println("Fecha Comentario: " + commentToDelete.getCreatedAt());
        System.out.println("Inicio de Hoy: " + startOfToday);
        boolean isToday = commentToDelete.getCreatedAt().isAfter(startOfToday);
        System.out.println("¿Es de hoy?: " + isToday);

        if (isToday) {
            interactionLimitService.refundInteraction(currentUser, InteractionType.COMMENT);
        } else {
            System.out.println("No se pide reembolso porque el comentario es antiguo.");
        }
        // -----------------------------
    }
}