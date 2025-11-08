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

@Component
@RequiredArgsConstructor
@Slf4j
public class ThreadScheduler {

    private final ThreadRepository threadRepository;

    /**
     * Este método se ejecuta cada minuto, todos los días.
     * Busca hilos no publicados cuya fecha de programación ya ha pasado.
     */
    @Scheduled(cron = "0 * * * * ?") // Se ejecuta en el segundo 0 de cada minuto
    @Transactional
    public void publishScheduledThreads() {
        log.info("Ejecutando tarea de publicación de hilos programados...");

        // 1. Busca hilos que necesitan ser publicados.
        List<ThreadClass> threadsToPublish = threadRepository
                .findThreadsToPublish(LocalDateTime.now());

        if (threadsToPublish.isEmpty()) {
            log.info("No hay hilos para publicar en este momento.");
            return;
        }

        // 2. Actualiza cada hilo.
        for (ThreadClass thread : threadsToPublish) {
            thread.setPublished(true);
        }

        // 3. Guarda todos los cambios en la base de datos.
        threadRepository.saveAll(threadsToPublish);
        log.info("Publicados {} hilos que estaban programados.", threadsToPublish.size());
    }

}
