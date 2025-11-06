package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.CategoryDto;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;


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
}
