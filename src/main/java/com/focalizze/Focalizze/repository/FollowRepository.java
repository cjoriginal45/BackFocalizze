package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Follow;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow,Long> {

    long countByUserFollowed(User user);

    long countByUserFollower(User user);
}
