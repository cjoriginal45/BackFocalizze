package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.exceptions.DailyLimitExceededException;
import com.focalizze.Focalizze.models.InteractionLog;
import com.focalizze.Focalizze.models.InteractionType;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.InteractionLogRepository;
import com.focalizze.Focalizze.services.InteractionLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the {@link InteractionLimitService} interface.
 * Controls the daily limits of user interactions to prevent spam and encourage quality content.
 * <p>
 * Implementación de la interfaz {@link InteractionLimitService}.
 * Controla los límites diarios de interacciones de los usuarios para prevenir spam y fomentar contenido de calidad.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionLimitServiceImpl implements InteractionLimitService {

    private static final int DAILY_INTERACTION_LIMIT = 20;

    private final InteractionLogRepository interactionLogRepository;



    /**
     * Checks if the user has reached their daily interaction limit.
     * Throws an exception if the limit is reached or the user is suspended.
     * <p>
     * Verifica si el usuario ha alcanzado su límite diario de interacciones.
     * Lanza una excepción si se alcanza el límite o si el usuario está suspendido.
     *
     * @param user The user performing the action.
     *             El usuario que realiza la acción.
     * @throws AccessDeniedException       If the user account is suspended.
     *                                     Si la cuenta del usuario está suspendida.
     * @throws DailyLimitExceededException If the daily limit (20) is exceeded.
     *                                     Si se excede el límite diario (20).
     */
    @Override
    @Transactional(readOnly = true)
    public void checkInteractionLimit(User user) {
        // Optimization: Fail-fast check for suspension before hitting the DB.
        // Optimización: Verificación rápida de suspensión antes de consultar la BD.
        if (user.isSuspended()) {
            throw new AccessDeniedException("Your account is temporarily suspended. / Tu cuenta está suspendida temporalmente.");
        }

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long interactionsToday = interactionLogRepository.countByUserAndCreatedAtAfter(user, startOfToday);

        if (interactionsToday >= DAILY_INTERACTION_LIMIT) {
            throw new DailyLimitExceededException("Daily limit of 20 interactions reached. / Límite diario de 20 interacciones alcanzado.");
        }
    }


    /**
     * Records a new interaction log for the user.
     * <p>
     * Registra un nuevo log de interacción para el usuario.
     *
     * @param user The user who interacted.
     *             El usuario que interactuó.
     * @param type The type of interaction (LIKE or COMMENT).
     *             El tipo de interacción (LIKE o COMENTARIO).
     */
    @Override
    @Transactional
    public void recordInteraction(User user, InteractionType type) {
        InteractionLog log = InteractionLog.builder()
                .user(user)
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();
        interactionLogRepository.save(log);
    }

    /**
     * Calculates the number of remaining interactions for the user today.
     * <p>
     * Calcula el número de interacciones restantes para el usuario hoy.
     *
     * @param user The user to query.
     *             El usuario a consultar.
     * @return The number of available interactions (>= 0).
     *         El número de interacciones disponibles (>= 0).
     */
    @Override
    @Transactional(readOnly = true)
    public int getRemainingInteractions(User user) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long interactionsToday = interactionLogRepository.countByUserAndCreatedAtAfter(user, startOfToday);
        return (int) Math.max(0, DAILY_INTERACTION_LIMIT - interactionsToday);
    }

    /**
     * Attempts to delete the most recent interaction log of a specific type for today.
     * Effectively "refunds" the interaction quota to the user (e.g., unlike, delete comment).
     * <p>
     * Intenta eliminar el log de interacción más reciente de un tipo específico para hoy.
     * Efectivamente "reembolsa" el cupo de interacción al usuario (ej. quitar like, borrar comentario).
     *
     * @param user The user undoing the action.
     *             El usuario que deshace la acción.
     * @param type The type of interaction to refund.
     *             El tipo de interacción a reembolsar.
     */
    @Transactional
    @Override
    public void refundInteraction(User user, InteractionType type) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        // Usamos la nueva consulta que devuelve una lista
        List<InteractionLog> logs = interactionLogRepository.findLogsToRefund(user, type, startOfToday);

        if (!logs.isEmpty()) {
            // Take the first one (most recent due to ORDER BY DESC in repo)
            // Tomamos el primero (el más reciente debido al ORDER BY DESC en el repo)
            InteractionLog logToDelete = logs.get(0);

            interactionLogRepository.delete(logToDelete);
            log.info("Interaction log deleted. ID: {}, User: {}", logToDelete.getId(), user.getUsername());
        } else {
            log.debug("No logs found to refund for user {} today.", user.getUsername());
        }
    }
}
