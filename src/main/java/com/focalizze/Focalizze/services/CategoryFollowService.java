package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.models.User;

public interface CategoryFollowService {
    void toggleFollowCategory(Long categoryId, User currentUser);


}
