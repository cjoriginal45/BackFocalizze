package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "category_follow_tbl")
public class CategoryFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryClass category;

    @Column(nullable = false, updatable = false)
    private LocalDateTime followedAt;

    @PrePersist
    protected void onFollow() {
        followedAt = LocalDateTime.now();
    }
}
