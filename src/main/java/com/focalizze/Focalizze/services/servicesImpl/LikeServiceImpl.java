package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.InteractionLogRepository;
import com.focalizze.Focalizze.repository.LikeRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.InteractionLimitService;
import com.focalizze.Focalizze.services.LikeService;
import com.focalizze.Focalizze.services.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementation of the {@link LikeService} interface.
 * Handles the logic for toggling likes on threads and managing associated side effects.
 * <p>
 * Implementación de la interfaz {@link LikeService}.
 * Maneja la lógica para alternar "me gusta" en hilos y gestionar efectos secundarios asociados.
 */
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final ThreadRepository threadRepository;
    private final LikeRepository likeRepository;
    private final InteractionLimitService interactionLimitService;
    private final InteractionLogRepository interactionLogRepository;
    private final NotificationService notificationService;

    /**
     * Toggles the like status of a thread for the current user.
     * Manages interaction limits (deducting or refunding quota) and notifications.
     * <p>
     * Alterna el estado de "me gusta" de un hilo para el usuario actual.
     * Gestiona los límites de interacción (deduciendo o reembolsando cupo) y notificaciones.
     *
     * @param threadId    The ID of the thread.
     *                    El ID del hilo.
     * @param currentUser The user performing the action.
     *                    El usuario que realiza la acción.
     * @throws EntityNotFoundException If the thread does not exist.
     *                                 Si el hilo no existe.
     */
    @Override
    @Transactional
    public void toggleLike(Long threadId, User currentUser) {
        // 1. Validate thread existence / Validar existencia del hilo
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found / Hilo no encontrado: " + threadId));

        // 2. Check existing like / Comprobar like existente
        Optional<Like> existingLike = likeRepository.findByUserAndThread(currentUser, thread);

        if (existingLike.isPresent()) {
            // REMOVE LIKE (Undo) / QUITAR LIKE (Deshacer)
            likeRepository.delete(existingLike.get());
            thread.setLikeCount(Math.max(0, thread.getLikeCount() - 1));

            // Refund logic: Only if the like was created today
            // Lógica de reembolso: Solo si el like fue creado hoy
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            if (existingLike.get().getCreatedAt().isAfter(startOfToday)) {
                interactionLimitService.refundInteraction(currentUser, InteractionType.LIKE);
            }

        } else {
            // Si el like no existe, lo creamos (dar like).
            // If the like does not exist, we create it (give like).

            // 1. Check limits / Verificar límites
            interactionLimitService.checkInteractionLimit(currentUser);

            // 2. Create Like entity / Crear entidad Like
            Like newLike = Like.builder()
                    .user(currentUser)
                    .createdAt(LocalDateTime.now())
                    .thread(thread)
                    .build();
            likeRepository.save(newLike);

            // 3. Update counter / Actualizar contador
            thread.setLikeCount(thread.getLikeCount() + 1);

            // 4. Record interaction / Registrar interacción
            interactionLimitService.recordInteraction(currentUser, InteractionType.LIKE);

            // 5. Notify author (if not self) / Notificar autor (si no es uno mismo)
            if (!thread.getUser().getId().equals(currentUser.getId())) {
                notificationService.createAndSendNotification(
                        thread.getUser(),
                        NotificationType.NEW_LIKE,
                        currentUser,
                        thread
                );
            }
        }

        // Persist thread changes (like count)
        // Persistir cambios del hilo (conteo de likes)
        threadRepository.save(thread);
    }
}
