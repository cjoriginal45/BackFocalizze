package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.dto.CategoryDetailsDto;
import com.focalizze.Focalizze.models.CategoryClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryClass,Long> {
    Optional<CategoryClass> findByName(String name);

    @Modifying
    @Query("UPDATE CategoryClass c SET c.followersCount = c.followersCount + 1 WHERE c.id = :categoryId")
    void incrementFollowersCount(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("UPDATE CategoryClass c SET c.followersCount = c.followersCount - 1 WHERE c.id = :categoryId")
    void decrementFollowersCount(@Param("categoryId") Long categoryId);

    @Query("SELECT new com.focalizze.Focalizze.dto.CategoryDetailsDto(" +
            "c.id, " +
            "c.name, " +
            "c.description, " +
            "c.imageUrl, " +
            "c.followersCount, " +
            // CAST explícito para asegurar que el resultado sea Long, aunque COUNT ya lo devuelve.
            "CAST((SELECT COUNT(t) FROM ThreadClass t WHERE t.category = c AND t.isPublished = true AND t.isDeleted = false) AS Long), " +
            // --- MEJORA ---
            // Manejamos el caso en que currentUserId es null.
            "CASE WHEN (:currentUserId IS NOT NULL) AND EXISTS(" +
            "   SELECT 1 FROM CategoryFollow cf WHERE cf.category = c AND cf.user.id = :currentUserId" +
            ") THEN true ELSE false END" +
            ") " +
            "FROM CategoryClass c " +
            "WHERE lower(c.name) = lower(:name)")
    Optional<CategoryDetailsDto> findCategoryDetailsByName(@Param("name") String name, @Param("currentUserId") Long currentUserId);
}
