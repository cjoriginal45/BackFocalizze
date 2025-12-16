package com.focalizze.Focalizze.utils;

import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task component for managing thread publication.
 * Runs periodically to publish threads that were scheduled for a specific time.
 * <p>
 * Componente de tarea programada para gestionar la publicación de hilos.
 * Se ejecuta periódicamente para publicar hilos que fueron programados para una hora específica.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ThreadScheduler {

    private final ThreadRepository threadRepository;

    /**
     * Checks every minute for threads that are ready to be published.
     * Updates their status to 'published' and persists changes.
     * <p>
     * Comprueba cada minuto si hay hilos listos para ser publicados.
     * Actualiza su estado a 'publicado' y persiste los cambios.
     */
    @Scheduled(cron = "0 * * * * ?") // Se ejecuta en el segundo 0 de cada minuto
    @Transactional
    public void publishScheduledThreads() {
        log.info("Running scheduled thread publication task... / Ejecutando tarea de publicación de hilos programados...");

        // 1. Busca hilos que necesitan ser publicados.
        List<ThreadClass> threadsToPublish = threadRepository
                .findThreadsToPublish(LocalDateTime.now());

        if (threadsToPublish.isEmpty()) {
            log.debug("No threads to publish at this time. / No hay hilos para publicar en este momento.");
            return;
        }

        // 2. Update status / Actualizar estado
        threadsToPublish.forEach(thread -> thread.setPublished(true));

        // 3. Persist changes / Persistir cambios
        // saveAll is efficient for batch updates / saveAll es eficiente para actualizaciones por lotes
        threadRepository.saveAll(threadsToPublish);

        log.info("Publicados {} hilos que estaban programados.", threadsToPublish.size());
    }

}
