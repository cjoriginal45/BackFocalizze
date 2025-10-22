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
    private final SavedThreadRepository savedThreadRepository;
    private final FeedMapper feedMapper; // Usamos el nuevo mapper / We use the new mapper

    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getFeed(Pageable pageable) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<ThreadClass> threadPage = threadRepository.findThreadsForFeed(pageable);

        return threadPage.map(thread -> {
            // 1. Obtenemos el DTO base del mapper / We obtain the base DTO from the mapper
            FeedThreadDto dto = feedMapper.toFeedThreadDto(thread);

            // 2. Calculamos los booleanos específicos del usuario actual / Calculamos los booleanos específicos del usuario actual
            boolean isLiked = thread.getLikes().stream()
                    .anyMatch(like -> like.getUser().getId().equals(currentUser.getId()));

            boolean isSaved = savedThreadRepository.existsByUserAndThread(currentUser, thread);

            // 3. Devolvemos una NUEVA instancia del DTO con los datos correctos / We return a NEW instance of the DTO with the correct data
            return new FeedThreadDto(
                    dto.id(), dto.user(), dto.publicationDate(), dto.posts(),
                    dto.stats(), isLiked, isSaved
            );
        });
    }
}
