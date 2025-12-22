package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.models.User;

import java.util.List;
import java.util.Set;

public interface FeedbackService {
    void hideThread(Long threadId, String reasonType, User currentUser);
    Set<Long> getHiddenThreadIds(User currentUser);
}