package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.FeedService;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final ThreadRepository threadRepository;
    private final ThreadEnricher threadEnricher;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getFeed(Pageable pageable) {
        // 1. Obtenemos al usuario autenticado y lo recargamos para acceder a sus colecciones LAZY.
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        // 2. Extraemos las listas de IDs que el usuario sigue.
        List<Long> followedUserIds = currentUser.getFollowing().stream()
                .map(follow -> follow.getUserFollowed().getId())
                .toList();

        List<Long> followedCategoryIds = currentUser.getFollowedCategories().stream()
                .map(categoryFollow -> categoryFollow.getCategory().getId())
                .toList();

        if (followedUserIds.isEmpty() && followedCategoryIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // (Añadimos placeholders si alguna lista está vacía para que la consulta IN no falle)
        if (followedUserIds.isEmpty()) { followedUserIds.add(-1L); }
        if (followedCategoryIds.isEmpty()) { followedCategoryIds.add(-1L); }

        // 3. Obtenemos la PÁGINA de entidades 'ThreadClass' filtradas.
        Page<ThreadClass> threadPage = threadRepository.findFollowingFeed(
                followedUserIds,
                followedCategoryIds,
                pageable
        );

        //    Extraemos la LISTA de contenido de la página.
        List<ThreadClass> threadsOnPage = threadPage.getContent();

        //    Pasamos la LISTA al enriquecedor.
        List<FeedThreadDto> enrichedContent = threadEnricher.enrichList(threadsOnPage, currentUser);

        // 5. Reconstruimos y devolvemos un nuevo objeto 'Page'.
        return new PageImpl<>(enrichedContent, pageable, threadPage.getTotalElements());
    }
}
