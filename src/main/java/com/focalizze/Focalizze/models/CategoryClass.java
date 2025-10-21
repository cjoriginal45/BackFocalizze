package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "category_tbl")
public class CategoryClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @ManyToMany(mappedBy="categories") //category depende de user
    private Set<User> users;

    @OneToMany(mappedBy="category")
    private List<ThreadClass> threads;


    public CategoryClass(){
        this.threads = new ArrayList<>();
        this.users = new HashSet<>();
    }

    public CategoryClass(String name) {
        this.name = name;
    }
}
