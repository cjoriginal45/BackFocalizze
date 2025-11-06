package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.CategoryFollow;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryFollowRepository;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.services.CategoryFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryFollowServiceImpl implements CategoryFollowService {

    private final CategoryRepository categoryRepository;
    private final CategoryFollowRepository categoryFollowRepository;

    @Override
    @Transactional
    public void toggleFollowCategory(Long categoryId, User currentUser) {
        // Buscamos la categoría.
        CategoryClass category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoria no encontrada"+ categoryId));

        // Comprobamos si ya existe la relación de seguimiento.
        Optional<CategoryFollow> existingFollow = categoryFollowRepository
                .findByUserAndCategory(currentUser,category);

        if(existingFollow.isPresent()){
            // dejar de seguir
            categoryFollowRepository.delete(existingFollow.get());
            categoryRepository.decrementFollowersCount(categoryId);
        }else {
            // seguir
            CategoryFollow newFollow = CategoryFollow
                    .builder()
                    .user(currentUser)
                    .category(category)
                    .build();
            categoryFollowRepository.save(newFollow);
            categoryRepository.incrementFollowersCount(categoryId);
        }
    }
}
