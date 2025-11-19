package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.models.Post;
import com.focalizze.Focalizze.models.User;

public interface MentionService {
    void processMentions(Post post, User author);
}
