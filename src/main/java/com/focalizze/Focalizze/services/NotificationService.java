package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.models.NotificationType;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;

public interface NotificationService {

    void createAndSendNotification(User userToNotify, NotificationType type, String message, ThreadClass thread);


}
