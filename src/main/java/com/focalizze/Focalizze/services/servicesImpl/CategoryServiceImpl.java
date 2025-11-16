package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.CategoryDetailsDto;
import com.focalizze.Focalizze.dto.CategoryDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
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
        CategoryClass category = categoryRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + name));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isFollowed = false;

        if (authentication != null && authentication.getPrincipal() instanceof User authenticatedUser) {
            isFollowed = authenticatedUser.getFollowedCategories().stream()
                    .anyMatch(cf -> cf.getCategory().getId().equals(category.getId()));
        }

        return new CategoryDetailsDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getImageUrl(),
                category.getFollowersCount(),
                category.getThreadsCount(),
                isFollowed
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getThreadsByCategory(String name, Pageable pageable) {
        CategoryClass category = categoryRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + name));

        Page<ThreadClass> threadPage = threadRepository.findByCategoryAndIsPublishedTrueAndIsDeletedFalse(category, pageable);

        User currentUser = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User authenticatedUser) {
            currentUser = authenticatedUser;
        }

        List<FeedThreadDto> enrichedDtoList = threadEnricher.enrichList(threadPage.getContent(), currentUser);
        return new PageImpl<>(enrichedDtoList, pageable, threadPage.getTotalElements());
    }
}
