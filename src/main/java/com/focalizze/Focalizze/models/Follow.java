package com.focalizze.Focalizze.models;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

public class Follow {

    @ManyToOne
    @JoinColumn(name="user_follower_id") //foreign key
    private User userFollower;

    @ManyToOne
    @JoinColumn(name="user_followed_id") //foreign key
    private User userFollowed;
}
