package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.CategoryDto;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.services.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryDto> getAllCategories() {
        // 1. Busca todas las categorías en la base de datos.
        List<CategoryClass> categories = categoryRepository.findAll();

        // 2. Mapea la lista de entidades a una lista de DTOs.
        return categories.stream()
                .map(category -> new CategoryDto(category.getName()))
                .collect(Collectors.toList());
    }
}
