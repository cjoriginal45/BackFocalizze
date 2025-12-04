package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.exceptions.DailyLimitExceededException;
import com.focalizze.Focalizze.models.InteractionLog;
import com.focalizze.Focalizze.models.InteractionType;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.InteractionLogRepository;
import com.focalizze.Focalizze.services.InteractionLimitService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InteractionLimitServiceImpl implements InteractionLimitService {

    private static final int DAILY_INTERACTION_LIMIT = 20;

    private final InteractionLogRepository interactionLogRepository;

    public InteractionLimitServiceImpl(InteractionLogRepository interactionLogRepository) {
        this.interactionLogRepository = interactionLogRepository;
    }

    /**
     * Verifica si el usuario ha alcanzado su límite diario de interacciones.
     * Si lo ha alcanzado, lanza una excepción.
     * @param user El usuario que realiza la acción.
     */
    @Override
    public void checkInteractionLimit(User user) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long interactionsToday = interactionLogRepository.countByUserAndCreatedAtAfter(user, startOfToday);

        if (user.isSuspended()) throw new AccessDeniedException("Tu cuenta está suspendida temporalmente.");

        if (interactionsToday >= DAILY_INTERACTION_LIMIT) {
            throw new DailyLimitExceededException("Límite diario de 20 interacciones alcanzado.");
        }

    }


    /**
     * Registra una nueva interacción para el usuario.
     * @param user El usuario que interactuó.
     * @param type El tipo de interacción (LIKE o COMMENT).
     */
    @Override
    public void recordInteraction(User user, InteractionType type) {
        InteractionLog log = InteractionLog.builder()
                .user(user)
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();
        interactionLogRepository.save(log);
    }

    /**
     * Obtiene el número de interacciones restantes para un usuario.
     * @param user El usuario a consultar.
     * @return El número de interacciones disponibles.
     */
    @Override
    public int getRemainingInteractions(User user) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long interactionsToday = interactionLogRepository.countByUserAndCreatedAtAfter(user, startOfToday);
        return (int) Math.max(0, DAILY_INTERACTION_LIMIT - interactionsToday);
    }

    /**
     * Intenta eliminar un log de interacción para 'reembolsar' el cupo al usuario.
     * @param user El usuario que deshace la acción.
     * @param type El tipo de interacción a reembolsar.
     */
    @Transactional
    @Override
    public void refundInteraction(User user, InteractionType type) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        System.out.println("--- INTENTO DE REEMBOLSO ---");
        System.out.println("Usuario: " + user.getUsername());
        System.out.println("Tipo: " + type);
        System.out.println("Buscando logs creados después de: " + startOfToday);

        // Usamos la nueva consulta que devuelve una lista
        List<InteractionLog> logs = interactionLogRepository.findLogsToRefund(user, type, startOfToday);

        if (!logs.isEmpty()) {
            // Tomamos el primero (el más reciente)
            InteractionLog logToDelete = logs.get(0);
            System.out.println("LOG ENCONTRADO. ID: " + logToDelete.getId() + " Fecha: " + logToDelete.getCreatedAt());

            interactionLogRepository.delete(logToDelete);
            System.out.println("LOG ELIMINADO CORRECTAMENTE.");
        } else {
            System.out.println("NO SE ENCONTRARON LOGS PARA REEMBOLSAR HOY.");
            // Esto nos dirá si el problema es que no encuentra el registro
        }
        System.out.println("----------------------------");
    }
}
