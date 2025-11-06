package com.focalizze.Focalizze.repository;

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
}
