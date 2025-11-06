package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.CategoryDto;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.services.CategoryService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryDto> getAllCategories() {

        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<CategoryClass> allCategories = categoryRepository.findAll();

        Set<Long> followedCategoryIds = currentUser.getFollowedCategories().stream()
                .map(cf -> cf.getCategory().getId())
                .collect(Collectors.toSet());

        return allCategories.stream().map(category -> new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getFollowersCount(),
                followedCategoryIds.contains(category.getId()) // <-- Cálculo de 'isFollowed'
        )).collect(Collectors.toList());
    }
}
