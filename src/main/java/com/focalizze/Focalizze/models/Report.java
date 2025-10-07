package com.focalizze.Focalizze.models;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

public class Report {

    @ManyToOne
    @JoinColumn(name="user_id") //foreign key
    private User user;

}
