package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.mappers.FeedMapper;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.SavedThreadRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final ThreadRepository threadRepository;
    private final FeedMapper feedMapper;
    private final SavedThreadRepository savedThreadRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getFeed(Pageable pageable) {

        // Obtenemos al usuario que está haciendo la petición desde el contexto de seguridad.
        // We get the user who is making the request from the security context.
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Obtenemos la página de hilos desde la base de datos.
        // We get the threads page from the database.
        Page<ThreadClass> threadPage = threadRepository.findThreadsForFeed(pageable);

        List<ThreadClass> threadsOnPage = threadPage.getContent();

        if (threadsOnPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Obtenemos los IDs de los hilos de la página actual.
        List<Long> threadIds = threadsOnPage.stream().map(ThreadClass::getId).collect(Collectors.toList());

        //    Hacemos una consulta a la BD para saber cuáles de estos hilos ha guardado el usuario.
        Set<Long> savedThreadIds = savedThreadRepository.findSavedThreadIdsByUserInThreadIds(currentUser, threadIds);

        // Mapeamos cada hilo de la página a su DTO correspondiente.
        // We map each thread on the page to its corresponding DTO.
        return threadPage.map(thread -> {

            boolean isLikedByCurrentUser = thread.getLikes().stream()
                    .anyMatch(like -> like.getUser().getId().equals(currentUser.getId()));

            boolean isSavedByCurrentUser = savedThreadIds.contains(thread.getId());

            FeedThreadDto baseDto = feedMapper.toFeedThreadDto(thread);

            return baseDto.withInteractionStatus(isLikedByCurrentUser, isSavedByCurrentUser);
        });
    }
}
