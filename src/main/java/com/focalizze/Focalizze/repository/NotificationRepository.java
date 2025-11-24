package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.NotificationClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationClass,Long> {
    Page<NotificationClass> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @Query(value = "SELECT n FROM NotificationClass n " +
            "LEFT JOIN FETCH n.triggerUser " +
            "LEFT JOIN FETCH n.thread " +
            "WHERE n.user = :user",
            countQuery = "SELECT count(n) FROM NotificationClass n WHERE n.user = :user")
    Page<NotificationClass> findByUserWithDetails(@Param("user") User user, Pageable pageable);

    boolean existsByUserAndIsReadIsFalse(User user);

    @Modifying
    @Query("UPDATE NotificationClass n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadForUser(@Param("userId") Long userId);
}
