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

/**
 * Entity representing a discussion thread.
 * Contains posts, images, and interaction metrics.
 * <p>
 * Entidad que representa un hilo de discusión.
 * Contiene publicaciones, imágenes y métricas de interacción.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "thread_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ThreadClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // --- Metrics / Métricas ---
    @Column(nullable = false)
    @Builder.Default
    private Integer saveCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    // --- State / Estado ---

    private LocalDateTime createdAt;

    private LocalDateTime scheduledTime;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    private boolean isPublished;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;


    // --- Transient Field (Not in DB) ---
    @Transient
    private boolean isSavedByCurrentUser;

    // --- Relationships / Relaciones ---
    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private Set<ThreadImage> images = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    @Builder.Default
    @ToString.Exclude
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "thread")
    @Builder.Default
    @ToString.Exclude
    private List<Report> reports = new ArrayList<>();

    @OneToMany(mappedBy = "thread")
    @Builder.Default
    @ToString.Exclude
    private List<Like> likes = new ArrayList<>();

    @OneToMany(mappedBy = "thread")
    @Builder.Default
    @ToString.Exclude
    private List<CommentClass> comments = new ArrayList<>();

    @OneToMany(mappedBy = "thread")
    @Builder.Default
    @ToString.Exclude
    private List<NotificationClass> notifications = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    private CategoryClass category;

}