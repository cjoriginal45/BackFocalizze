package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.SavedThreads;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.SavedThreadRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.SaveService;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of the {@link SaveService} interface.
 * Handles saving threads (bookmarks) and retrieving the saved threads feed.
 * <p>
 * Implementación de la interfaz {@link SaveService}.
 * Maneja el guardado de hilos (marcadores) y la recuperación del feed de hilos guardados.
 */
@Service
@RequiredArgsConstructor
public class SaveServiceImpl implements SaveService {

    private final ThreadRepository threadRepository;
    private final SavedThreadRepository savedThreadRepository;
    private final ThreadEnricher threadEnricher;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;

    /**
     * Toggles the saved state of a thread for the current user.
     * <p>
     * Alterna el estado guardado de un hilo para el usuario actual.
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
    public void toggleSave(Long threadId, User currentUser) {
        // 1. Validar que el hilo exista.
        // 1. Validate that the thread exists.
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found / Hilo no encontrado: " + threadId));

        // 2. Comprobar si el usuario ya ha guardado este hilo.
        // 2. Check if the user has already saved this thread.
        Optional<SavedThreads> existingSave = savedThreadRepository.findByUserAndThread(currentUser, thread);

        if (existingSave.isPresent()) {
            // 3. Si ya está guardado, lo eliminamos.
            // 3. If it is already saved, we delete it.
            savedThreadRepository.delete(existingSave.get());
            thread.setSaveCount(thread.getSaveCount() - 1);
        } else {
            // 4. Si no está guardado, creamos una nueva entrada.
            // 4. If it is not saved, we create a new entry.
            SavedThreads newSave = SavedThreads.builder()
                    .user(currentUser)
                    .thread(thread)
                    .build();
            savedThreadRepository.save(newSave);
            thread.setSaveCount(thread.getSaveCount() + 1);
        }

        // 5. Guardamos la entidad del hilo con el contador actualizado.
        // 5. We save the thread entity with the updated counter.
        threadRepository.save(thread);
    }

    /**
     * Retrieves a paginated list of threads saved by the current user.
     * Filters out threads if the author is blocked.
     * <p>
     * Recupera una lista paginada de hilos guardados por el usuario actual.
     * Filtra hilos si el autor está bloqueado.
     *
     * @param pageable Pagination info.
     *                 Información de paginación.
     * @return A Page of enriched threads.
     *         Una Página de hilos enriquecidos.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getSavedThreadsForCurrentUser(Pageable pageable) {
        // 1. Get User Safely
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            throw new IllegalStateException("Authentication principal is not a User entity.");
        }

        // 2. Fetch SavedThreads Entities
        Page<SavedThreads> savedThreadsPage = savedThreadRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable);

        // 3. Get Blocked IDs
        Set<Long> allBlockedIds = new HashSet<>();
        allBlockedIds.addAll(blockRepository.findBlockedUserIdsByBlocker(currentUser.getId()));
        allBlockedIds.addAll(blockRepository.findUserIdsWhoBlockedUser(currentUser.getId()));

        // 4. Extract and Filter Threads
        List<ThreadClass> threads = savedThreadsPage.getContent().stream()
                .map(SavedThreads::getThread)
                .filter(thread -> !allBlockedIds.contains(thread.getUser().getId()))
                .toList();

        // 5. Enrich
        List<FeedThreadDto> enrichedDtoList = threadEnricher.enrichList(threads, currentUser);

        // 6. Return Page
        return new PageImpl<>(enrichedDtoList, pageable, savedThreadsPage.getTotalElements());
    }

}
