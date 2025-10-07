package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

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

    private Integer saveCount;

    private boolean isSavedByCurrentUser;

    private LocalDateTime createdAt;

    private LocalDateTime scheduledTime;

    private boolean isPublished;

    private Integer likeCount;

    private Integer commentCount;

    @ManyToOne
    @JoinColumn(name="user_id") //foreign key
    private User user;

    @OneToMany(
            mappedBy = "thread",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Post> posts;

    @OneToMany(mappedBy="thread")
    private List<Report> reports;

    @OneToMany(mappedBy="thread")
    private List<Like> likes;

    @OneToMany(mappedBy="thread")
    private List<CommentClass> comments;

    @OneToMany(mappedBy="thread")
    private List<NotificationClass> notifications;

    public ThreadClass(){
        this.posts = new ArrayList<>();
        this.reports = new ArrayList<>();
        this.likes = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.notifications = new ArrayList<>();
    }
}
