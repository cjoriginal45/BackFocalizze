// src/main/java/com/focalizze/Focalizze/models/ThreadClass.java
package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault; // Importante!

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "thread_tbl")
public class ThreadClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    @Builder.Default
    private Integer saveCount = 0;

    private boolean isSavedByCurrentUser;

    private LocalDateTime createdAt;

    private LocalDateTime scheduledTime;

    private boolean isPublished;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @OneToMany(
            mappedBy = "thread",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    @ToString.Exclude
    private Set<ThreadImage> images = new HashSet<>(); // <-- Set y HashSet

    @ManyToOne
    @JoinColumn(name="user_id") //foreign key
    private User user;


    @OneToMany(
            mappedBy = "thread",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @BatchSize(size = 10)
    private List<Post> posts;

    @OneToMany(mappedBy="thread")
    private List<Report> reports;

    @OneToMany(mappedBy="thread")
    private List<Like> likes;

    @OneToMany(mappedBy="thread")
    private List<CommentClass> comments;

    @OneToMany(mappedBy="thread")
    private List<NotificationClass> notifications;

    @ManyToOne
    @JoinColumn(name="category_id")
    private CategoryClass category;

    public ThreadClass(){
        this.posts = new ArrayList<>();
        this.images = new HashSet<>();
        this.reports = new ArrayList<>();
        this.likes = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.notifications = new ArrayList<>();

        this.saveCount = 0;
        this.likeCount = 0;
        this.commentCount = 0;
        this.viewCount = 0;
    }
}