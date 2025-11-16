package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Builder
@Setter
@Getter
@Entity
@Table(name = "category_tbl")
public class CategoryClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    private String imageUrl;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CategoryFollow> followers;

    @OneToMany(mappedBy="category")
    private List<ThreadClass> threads;

    @Formula("(SELECT count(*) FROM thread_tbl t WHERE t.category_id = id AND t.is_published = true AND t.is_deleted = false)")
    private Integer threadsCount;

    @Builder.Default
    private Integer followersCount = 0;

    public CategoryClass(){
        this.threads = new ArrayList<>();
        this.followers = new HashSet<>();
    }

    public CategoryClass(String name) {
        this.name = name;
    }
}
