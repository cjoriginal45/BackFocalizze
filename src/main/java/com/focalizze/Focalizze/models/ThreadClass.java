package com.focalizze.Focalizze.models;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

public class ThreadClass {

    @ManyToOne
    @JoinColumn(name="user_id") //foreign key
    private User user;
}
