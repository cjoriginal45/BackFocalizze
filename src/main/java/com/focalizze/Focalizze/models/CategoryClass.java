package com.focalizze.Focalizze.models;

import jakarta.persistence.ManyToMany;

import java.util.List;
import java.util.Set;

public class CategoryClass {

    @ManyToMany(mappedBy="categories") //category depende de user
    private Set<User> users;
}
