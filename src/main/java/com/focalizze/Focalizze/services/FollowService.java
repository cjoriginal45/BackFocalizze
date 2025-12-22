package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.UserSummaryDto;
import com.focalizze.Focalizze.models.User;

import java.util.List;

public interface FollowService {

    void toggleFollowUser(String usernameToFollow, User currentUser);

    List<UserSummaryDto> getFollowers(String username, User currentUser);
    List<UserSummaryDto> getFollowing(String username, User currentUser);
}
