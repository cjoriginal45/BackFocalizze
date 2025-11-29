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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final ThreadRepository threadRepository;
    private final ThreadEnricher threadEnricher;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getFeed(Pageable pageable) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        // 2. Extraemos las listas de IDs
        List<Long> followedUserIds = new ArrayList<>(
                currentUser.getFollowing().stream()
                        .map(follow -> follow.getUserFollowed().getId())
                        .toList()
        );

        List<Long> followedCategoryIds = new ArrayList<>(
                currentUser.getFollowedCategories().stream()
                        .map(categoryFollow -> categoryFollow.getCategory().getId())
                        .toList()
        );

        // --- CORRECCIÓN: EVITAR SQL ERROR EN LISTAS VACÍAS ---
        // Si las listas están vacías, JPA puede lanzar error con "IN ()".
        // Agregamos -1L (un ID que no existe) para que la query sea válida: "IN (-1)"
        if (followedUserIds.isEmpty()) {
            followedUserIds.add(-1L);
        }
        if (followedCategoryIds.isEmpty()) {
            followedCategoryIds.add(-1L);
        }

        // --- BLOQUEADOS ---
        Set<Long> blockedUserIds = userRepository.findBlockedUserIdsByBlocker(currentUser.getId());
        Set<Long> userIdsWhoBlockedCurrentUser = userRepository.findUserIdsWhoBlockedUser(currentUser.getId());

        Set<Long> allBlockedIds = new HashSet<>();
        allBlockedIds.addAll(blockedUserIds);
        allBlockedIds.addAll(userIdsWhoBlockedCurrentUser);

        if (allBlockedIds.isEmpty()) {
            allBlockedIds.add(-1L);
        }

        // --- ELIMINADO EL IF QUE RETORNABA PAGE.EMPTY ---
        // Ahora siempre ejecutamos la consulta, porque currentUser.getId()
        // siempre traerá al menos mis propios hilos.

        // 3. Obtenemos la PÁGINA
        Page<ThreadClass> threadPage = threadRepository.findFollowingFeed(
                followedUserIds,
                followedCategoryIds,
                currentUser.getId(), // Aquí se incluyen tus hilos
                allBlockedIds,
                pageable
        );

        List<ThreadClass> threadsOnPage = threadPage.getContent();
        List<FeedThreadDto> enrichedContent = threadEnricher.enrichList(threadsOnPage, currentUser);

        return new PageImpl<>(enrichedContent, pageable, threadPage.getTotalElements());
    }
}
