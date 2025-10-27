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

        // Mapeamos cada hilo de la página a su DTO correspondiente.
        // We map each thread on the page to its corresponding DTO.
        return threadPage.map(thread -> {
            // Usamos el mapper para hacer la conversión base (stats, posts, etc.).
            // We use the mapper to do the base conversion (stats, posts, etc.).
            FeedThreadDto dto = feedMapper.toFeedThreadDto(thread);

            // Calculamos dinámicamente si al usuario actual le gusta este hilo.
            //  Esto funciona porque la consulta del repositorio ya cargó la lista de 'likes'.
            // We dynamically calculate whether the current user likes this thread.
            // This works because the repository query already loaded the list of likes.
            boolean isLiked = thread.getLikes().stream()
                    .anyMatch(like -> like.getUser().getId().equals(currentUser.getId()));

            // Calculamos dinámicamente si el usuario actual ha guardado este hilo.
            // We dynamically calculate if the current user has saved this thread.
            boolean isSaved = savedThreadRepository.existsByUserAndThread(currentUser, thread);

            // Devolvemos una nueva instancia del DTO con los booleanos correctos.
            // We return a new instance of the DTO with the correct booleans.
            return new FeedThreadDto(
                    dto.id(),
                    dto.user(),
                    dto.publicationDate(),
                    dto.posts(),
                    dto.stats(),
                    isLiked,
                    isSaved
            );
        });
    }
}
