package com.focalizze.Focalizze.utils;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.dto.mappers.FeedMapper;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.FollowRepository;
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
    private final FeedMapper feedMapper;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;

    /**
     * Toma una entidad ThreadClass y la enriquece con el estado de interacción del usuario actual.
     *
     * @param thread      La entidad del hilo a enriquecer.
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
     *
     * @param threads     La lista de hilos a enriquecer.
     * @param currentUser El usuario que está realizando la petición.
     * @return Una lista de FeedThreadDto completos y personalizados.
     */
    public List<FeedThreadDto> enrichList(List<ThreadClass> threads, User currentUser) {
        if (threads == null || threads.isEmpty()) {
            return List.of();
        }


        // 1. Obtenemos los IDs de los hilos y de los autores
        List<Long> threadIds = threads.stream().map(ThreadClass::getId).toList();
        Set<Long> authorIds = threads.stream().map(t -> t.getUser().getId()).collect(Collectors.toSet());

        // 2. Hacemos UNA consulta para saber qué hilos ha guardado el usuario.
        Set<Long> savedThreadIds = savedThreadRepository.findSavedThreadIdsByUserInThreadIds(currentUser, threadIds);

        // 3. Hacemos UNA consulta para saber a qué autores de esta lista sigue el usuario.
        Set<Long> followedUserIds = followRepository.findFollowedUserIdsByFollower(currentUser, authorIds);

        Set<Long> blockedUserIds = blockRepository.findBlockedIdsByBlockerAndBlockedIdsIn(currentUser, authorIds);


        // --- MAPEO Y ENRIQUECIMIENTO FINAL ---
        return threads.stream().map(thread -> {

            // a. Lógica para 'isLiked' (en memoria, gracias al JOIN FETCH)
            boolean isLiked = thread.getLikes().stream()
                    .anyMatch(like -> like.getUser().getId().equals(currentUser.getId()));

            // b. Lógica para 'isSaved' (en memoria)
            boolean isSaved = savedThreadIds.contains(thread.getId());

            // c. Lógica para 'isFollowing' (en memoria)
            boolean isFollowing = followedUserIds.contains(thread.getUser().getId());


            // d. Conversión base usando el mapper.
            //    Esto nos da un DTO con 'isLiked=false', 'isSaved=false' y 'user.isFollowing=false'.
            FeedThreadDto baseDto = feedMapper.toFeedThreadDto(thread);

            boolean isBlocked = blockedUserIds.contains(thread.getUser().getId());

            // e. Creamos el UserDto final con el 'isFollowing' correcto.
            UserDto finalUserDto = new UserDto(
                    baseDto.user().id(),
                    baseDto.user().username(),
                    baseDto.user().displayName(),
                    baseDto.user().avatarUrl(),
                    baseDto.user().calculatedThreadCount(), isFollowing,
                    baseDto.user().followingCount(),
                    baseDto.user().followersCount(),
                    isBlocked,
                    baseDto.user().role(),
                    baseDto.user().isTwoFactorEnabled(),
                    baseDto.user().backgroundType(),
                    baseDto.user().backgroundValue()
            );

            // f. Creamos el FeedThreadDto final con todos los datos enriquecidos.
            return new FeedThreadDto(
                    baseDto.id(),
                    finalUserDto,
                    baseDto.publicationDate(),
                    baseDto.posts(),
                    baseDto.stats(),
                    isLiked,
                    isSaved,
                    baseDto.categoryName(),
                    baseDto.images()
            );

        }).collect(Collectors.toList());
    }

}
