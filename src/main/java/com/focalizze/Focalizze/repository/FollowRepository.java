package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Follow;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow,Long> {

    long countByUserFollowed(User user);

    long countByUserFollower(User user);

    Optional<Follow> findByUserFollowerAndUserFollowed(User follower, User followed);
}
