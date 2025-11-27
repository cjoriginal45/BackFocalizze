package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.UserSearchDto;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ThreadRepository threadRepository;
    private final ThreadMapper threadMapper;

    @Override
    public List<UserSearchDto> searchUsersByPrefix(String prefix) {
        // La búsqueda no debería incluir el '@'
        String cleanPrefix = prefix.startsWith("@") ? prefix.substring(1) : prefix;

        // Devuelve una lista vacía si la búsqueda está vacía
        if (cleanPrefix.isBlank()) {
            return List.of();
        }

        List<User> users = userRepository.findTop5ByUsernameStartingWithIgnoreCase(cleanPrefix);

        // Mapea a DTO
        return users.stream()
                .map(user -> new UserSearchDto(user.getUsername(), user.getDisplayName(), user.getAvatarUrl()))
                .collect(Collectors.toList());
    }



    @Override
    @Transactional(readOnly = true)
    public List<ThreadResponseDto> searchContent(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Set<Long> blockedByCurrentUser = userRepository.findBlockedUserIdsByBlocker(currentUser.getId());
        Set<Long> whoBlockedCurrentUser = userRepository.findUserIdsWhoBlockedUser(currentUser.getId());

        Set<Long> allBlockedIds = new HashSet<>();
        allBlockedIds.addAll(blockedByCurrentUser);
        allBlockedIds.addAll(whoBlockedCurrentUser);

        // Estrategia de búsqueda:
        // 1. Intentar buscar por nombre de categoría exacto.
        Optional<CategoryClass> category = categoryRepository.findByName(query);

        List<ThreadClass> foundThreads;

        if (category.isPresent()) {
            foundThreads = threadRepository.findByCategory(category.get());
        } else {
            foundThreads = threadRepository.findByPostContentContainingIgnoreCase(query);
        }


        List<ThreadClass> filteredThreads = foundThreads.stream()
                .filter(thread -> !allBlockedIds.contains(thread.getUser().getId()))
                .toList();

        // Mapeamos los resultados al DTO de respuesta.
        return threadMapper.toDtoList(filteredThreads);
    }

}
