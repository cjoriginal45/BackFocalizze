package com.focalizze.Focalizze.utils;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.mappers.FeedMapper;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.SavedThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ThreadEnricher {

    private final SavedThreadRepository savedThreadRepository;
    private final FeedMapper feedMapper; // El mapper que convierte ThreadClass -> FeedThreadDto base

    /**
     * Toma una entidad ThreadClass y la enriquece con el estado de interacción del usuario actual.
     * @param thread La entidad del hilo a enriquecer.
     * @param currentUser El usuario que está realizando la petición.
     * @return Un FeedThreadDto completo y personalizado para el usuario.
     */
    public FeedThreadDto enrich(ThreadClass thread, User currentUser) {
        // 1. Conversión base usando el mapper.
        //    Esto nos da un DTO con los datos objetivos del hilo.
        FeedThreadDto baseDto = feedMapper.toFeedThreadDto(thread);

        // 2. Cálculo de 'isLiked'
        //    Esta operación es rápida porque asumimos que la colección 'likes'
        //    ya fue cargada con un JOIN FETCH en la consulta del repositorio.
        boolean isLikedByCurrentUser = thread.getLikes().stream()
                .anyMatch(like -> like.getUser().getId().equals(currentUser.getId()));

        // 3. Cálculo de 'isSaved'
        //    Esta operación requiere una consulta a la BD.
        //    Es ineficiente si se hace en un bucle (problema N+1).
        boolean isSavedByCurrentUser = savedThreadRepository.existsByUserAndThread(currentUser, thread);

        // 4. Devolvemos una nueva instancia del DTO con los booleanos correctos.
        return baseDto.withInteractionStatus(isLikedByCurrentUser, isSavedByCurrentUser);
    }

    /**
     * Método optimizado para enriquecer una LISTA de hilos, evitando el problema N+1 para 'isSaved'.
     * @param threads La lista de hilos a enriquecer.
     * @param currentUser El usuario que está realizando la petición.
     * @return Una lista de FeedThreadDto completos y personalizados.
     */
    public List<FeedThreadDto> enrichList(List<ThreadClass> threads, User currentUser) {
        if (threads.isEmpty()) {
            return List.of();
        }

        // --- Optimización para 'isSaved' ---
        // 1. Obtenemos los IDs de todos los hilos que vamos a procesar.
        List<Long> threadIds = threads.stream().map(ThreadClass::getId).toList();

        // 2. Hacemos UNA SOLA consulta a la BD para saber cuáles de estos hilos ha guardado el usuario.
        Set<Long> savedThreadIds = savedThreadRepository.findSavedThreadIdsByUserInThreadIds(currentUser, threadIds);

        // --- Mapeo y Enriquecimiento ---
        return threads.stream().map(thread -> {
            // Reutilizamos la lógica del 'enrich' individual, pero le pasamos la información ya calculada.
            boolean isLiked = thread.getLikes().stream()
                    .anyMatch(like -> like.getUser().getId().equals(currentUser.getId()));

            // La comprobación de 'isSaved' ahora es una operación en memoria súper rápida.
            boolean isSaved = savedThreadIds.contains(thread.getId());

            FeedThreadDto baseDto = feedMapper.toFeedThreadDto(thread);

            return baseDto.withInteractionStatus(isLiked, isSaved);
        }).collect(Collectors.toList());
    }
}
