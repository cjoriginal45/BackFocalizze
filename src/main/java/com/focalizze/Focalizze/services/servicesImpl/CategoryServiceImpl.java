package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.CategoryDetailsDto;
import com.focalizze.Focalizze.dto.CategoryDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.CategoryService;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final ThreadEnricher threadEnricher;
    private final BlockRepository blockRepository;

    @Override
    public List<CategoryDto> getAllCategories() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Set<Long> followedCategoryIds = Collections.emptySet();

        // Comprobamos si el principal es una instancia de User
        if (authentication != null && authentication.getPrincipal() instanceof User authenticatedUser) {

            User currentUser = userRepository.findById(authenticatedUser.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en la base de datos"));

            if (currentUser.getFollowedCategories() != null) {
                followedCategoryIds = currentUser.getFollowedCategories().stream()
                        .map(cf -> cf.getCategory().getId())
                        .collect(Collectors.toSet());
            }
        }

        List<CategoryClass> allCategories = categoryRepository.findAll();

        final Set<Long> finalFollowedCategoryIds = followedCategoryIds;
        return allCategories.stream().map(category -> new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getFollowersCount(),
                finalFollowedCategoryIds.contains(category.getId())
        )).collect(Collectors.toList());
    }

    @Override
    public CategoryDetailsDto getCategoryDetails(String name) {
        Long currentUserId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Obtenemos el ID del usuario actual, si está autenticado
        if (authentication != null && authentication.getPrincipal() instanceof User authenticatedUser) {
            currentUserId = authenticatedUser.getId();
        }

        // Llamamos al nuevo método del repositorio, que hace todo el trabajo.
        return categoryRepository.findCategoryDetailsByName(name, currentUserId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + name));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getThreadsByCategory(String name, Pageable pageable) {
        // --- YA NO NECESITAMOS BUSCAR LA CATEGORÍA AQUÍ ---
        // CategoryClass category = categoryRepository.findByNameIgnoreCase(name)
        //         .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + name));

        // --- LLAMAMOS AL NUEVO MÉTODO DEL THREAD REPOSITORY ---
        Page<ThreadClass> threadPage = threadRepository.findPublishedThreadsByCategoryName(name, pageable);

        // El resto del método para enriquecer los hilos no cambia
        User currentUser = null;
        Set<Long> allBlockedIds = new HashSet<>();
        allBlockedIds.add(-1L);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof User authenticatedUser) {
            currentUser = authenticatedUser;

            Set<Long> blockedByCurrentUser = blockRepository.findBlockedUserIdsByBlocker(currentUser.getId());
            Set<Long> whoBlockedCurrentUser = blockRepository.findUserIdsWhoBlockedUser(currentUser.getId());

            // Re-inicializamos el Set para no tener el -1L si hay datos reales
            allBlockedIds = new HashSet<>();
            allBlockedIds.addAll(blockedByCurrentUser);
            allBlockedIds.addAll(whoBlockedCurrentUser);
        }

        final Set<Long> finalBlockedIds = allBlockedIds;
        List<ThreadClass> content = threadPage.getContent().stream()
                // Filtramos en memoria los hilos de usuarios bloqueados
                .filter(thread -> !finalBlockedIds.contains(thread.getUser().getId()))
                .toList();

        List<FeedThreadDto> enrichedDtoList = threadEnricher.enrichList(threadPage.getContent(), currentUser);
        return new PageImpl<>(enrichedDtoList, pageable, threadPage.getTotalElements());
    }
}
