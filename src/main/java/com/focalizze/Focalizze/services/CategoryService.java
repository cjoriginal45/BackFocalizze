package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.CategoryDetailsDto;
import com.focalizze.Focalizze.dto.CategoryDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    List<CategoryDto> getAllCategories();

    CategoryDetailsDto getCategoryDetails(String name); // <-- MÉTODO MODIFICADO

    Page<FeedThreadDto> getThreadsByCategory(String name, Pageable pageable); // <-- NUEVO MÉTODO
}
