package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.CategoryClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryClass,Long> {
    @Query("SELECT c FROM CategoryClass WHERE c.name = :name")
    Optional<CategoryClass> findByName(@Param("name") String name);
}
