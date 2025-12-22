package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.NotificationDto;
import com.focalizze.Focalizze.models.NotificationType;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void createAndSendNotification(User userToNotify, NotificationType type, User triggerUser, ThreadClass thread);

    Page<NotificationDto> getNotificationsForUser(User user, Pageable pageable);

    boolean hasUnreadNotifications(User user);
    void markAllAsRead(User user);
}
