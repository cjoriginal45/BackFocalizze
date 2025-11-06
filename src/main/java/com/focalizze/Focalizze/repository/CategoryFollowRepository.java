package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.CategoryFollow;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryFollowRepository extends JpaRepository<CategoryFollow,Long> {
    Optional<CategoryFollow> findByUserAndCategory(User user, CategoryClass category);
}
