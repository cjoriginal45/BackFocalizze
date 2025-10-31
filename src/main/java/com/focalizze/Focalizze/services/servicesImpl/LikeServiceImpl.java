package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.InteractionType;
import com.focalizze.Focalizze.models.Like;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.InteractionLogRepository;
import com.focalizze.Focalizze.repository.LikeRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.InteractionLimitService;
import com.focalizze.Focalizze.services.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final ThreadRepository threadRepository;
    private final LikeRepository likeRepository;
    private final InteractionLimitService interactionLimitService;
    private final InteractionLogRepository interactionLogRepository;

    @Override
    @Transactional
    public void toggleLike(Long threadId, User currentUser) {
        // Validar que el hilo exista.
        // Validate that the thread exists.
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread no encontrado con id: " + threadId));

        // Comprobar si el usuario ya le ha dado like a este hilo.
        // Check if the user has already liked this thread.
        Optional<Like> existingLike = likeRepository.findByUserAndThread(currentUser, thread);

        if (existingLike.isPresent()) {
            // Si el like ya existe, lo eliminamos (quitar like).
            // If the like already exists, we delete it (remove like).
            likeRepository.delete(existingLike.get());
            thread.setLikeCount(thread.getLikeCount() - 1);
            // Actualizamos el contador de likes en la entidad del hilo.
            // We update the like counter in the thread entity.
            interactionLimitService.refundInteraction(currentUser, InteractionType.LIKE);
        } else {
            // Si el like no existe, lo creamos (dar like).
            // If the like does not exist, we create it (give like).

            // Verificar si el usuario puede dar like.
            interactionLimitService.checkInteractionLimit(currentUser);

            // Si puede, creamos el like.
            Like newLike = Like.builder()
                    .user(currentUser)
                    .thread(thread)
                    .build();
            likeRepository.save(newLike);
            // Actualizamos el contador.
            // We update the counter.
            thread.setLikeCount(thread.getLikeCount() + 1);

            // Registramos la interacción.
            interactionLimitService.recordInteraction(currentUser, InteractionType.LIKE);
        }

        // Guardamos la entidad del hilo con el contador actualizado.
        // La transacción se encargará de persistir este cambio.
        // We save the thread entity with the updated counter.
        // The transaction will be responsible for persisting this change.
        threadRepository.save(thread);
    }
}
