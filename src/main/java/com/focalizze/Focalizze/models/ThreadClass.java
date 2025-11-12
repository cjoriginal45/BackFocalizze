// src/main/java/com/focalizze/Focalizze/models/ThreadClass.java
package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault; // Importante!

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "thread_tbl")
public class ThreadClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer saveCount = 0;

    private boolean isSavedByCurrentUser;

    private LocalDateTime createdAt;

    private LocalDateTime scheduledTime;

    private boolean isPublished;

    private Integer likeCount = 0;

    private Integer commentCount = 0;

    private Integer viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

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