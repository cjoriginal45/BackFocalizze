package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name="user_tbl")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String displayName;

    private String biography;

    private String avatarUrl;

    private Integer dailyThreadsRemaining;

    private UserRole role;

    private Integer followingCount;

    private Integer followersCount;

    private Integer dailyInteractionsRemaining;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy="user")
    private List<SavedThreads> savedThreads;

    @OneToMany(mappedBy="user")
    private List<ThreadClass> threads;

    @OneToMany(mappedBy="user")
    private List<Report> reports;

    @OneToMany(mappedBy="user")
    private List<NotificationClass> notifications;

    @OneToMany(mappedBy="user")
    private List<CommentClass> comments;

    @OneToMany(mappedBy="user")
    private List<Like> likes;

    @ManyToMany
    @JoinTable(
            name = "categories_users",
            joinColumns = @JoinColumn(name = "user_id"),           // FK a User
            inverseJoinColumns = @JoinColumn(name = "category_id") // FK a Category
    )
    private Set<CategoryClass> categories;

    @OneToMany(mappedBy="userFollower")
    private List<Follow> following;

    @OneToMany(mappedBy="userFollowed")
    private List<Follow> followers;

    public User(){
        this.savedThreads = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.categories = new HashSet<>();
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
        this.likes = new ArrayList<>();
        this.reports = new ArrayList<>();
        this.threads = new ArrayList<>();
        this.notifications = new ArrayList<>();
    }
}
