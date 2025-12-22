package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.HiddenContent;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.HiddenContentRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.FeedbackService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Implementation of the {@link FeedbackService} interface.
 * Handles user feedback actions such as hiding specific threads.
 * <p>
 * Implementación de la interfaz {@link FeedbackService}.
 * Maneja acciones de retroalimentación del usuario como ocultar hilos específicos.
 */
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    private final HiddenContentRepository hiddenRepo;
    private final ThreadRepository threadRepo;

    /**
     * Hides a specific thread for the current user.
     * Creates a {@link HiddenContent} record to prevent the thread from appearing in feeds.
     * <p>
     * Oculta un hilo específico para el usuario actual.
     * Crea un registro {@link HiddenContent} para evitar que el hilo aparezca en los feeds.
     *
     * @param threadId    The ID of the thread to hide.
     *                    El ID del hilo a ocultar.
     * @param reasonType  The reason for hiding (e.g., "NOT_INTERESTED").
     *                    La razón para ocultar (ej. "NO_INTERESA").
     * @param currentUser The user performing the action.
     *                    El usuario que realiza la acción.
     * @throws EntityNotFoundException If the thread does not exist.
     *                                 Si el hilo no existe.
     */
    @Override
    @Transactional
    public void hideThread(Long threadId, String reasonType, User currentUser) {
        ThreadClass thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found / Hilo no encontrado: " + threadId));

        HiddenContent hidden = HiddenContent.builder()
                .user(currentUser)
                .thread(thread)
                .reasonType(reasonType)
                .build();

        hiddenRepo.save(hidden);
    }

    /**
     * Retrieves the set of thread IDs that the user has hidden.
     * Used for client-side filtering or efficient DB exclusion.
     * <p>
     * Recupera el conjunto de IDs de hilos que el usuario ha ocultado.
     * Usado para filtrado del lado del cliente o exclusión eficiente en BD.
     *
     * @param currentUser The user to verify.
     *                    El usuario a verificar.
     * @return A {@link Set} of hidden thread IDs.
     *         Un {@link Set} de IDs de hilos ocultos.
     */
    @Override
    @Transactional(readOnly = true)
    public Set<Long> getHiddenThreadIds(User currentUser) {
        return hiddenRepo.findHiddenThreadIdsByUser(currentUser);
    }
}