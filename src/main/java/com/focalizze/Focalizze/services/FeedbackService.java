package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.models.User;

import java.util.List;

public interface FeedbackService {
    void hideThread(Long threadId, String reasonType, User currentUser);
    List<Long> getHiddenThreadIds(User currentUser);
}