package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.NotificationClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationClass,Long> {
    Page<NotificationClass> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
