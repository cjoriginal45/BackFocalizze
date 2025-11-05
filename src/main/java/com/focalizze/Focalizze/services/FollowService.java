package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.models.User;

public interface FollowService {

    void toggleFollowUser(String usernameToFollow, User currentUser);
}
