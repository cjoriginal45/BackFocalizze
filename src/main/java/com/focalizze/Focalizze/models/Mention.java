package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a user mention within a post.
 * Generated when a user types "@username".
 * <p>
 * Entidad que representa una mención de usuario dentro de una publicación.
 * Generada cuando un usuario escribe "@nombredeusuario".
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "mention_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Mention {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The user being mentioned.
     * El usuario que está siendo mencionado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="mentioned_user_id",nullable = false)
    @ToString.Exclude
    private User mentionedUser;

    /**
     * The post containing the mention.
     * La publicación que contiene la mención.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Post post;

    /**
     * Timestamp of the mention.
     * Marca de tiempo de la mención.
     */
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically sets the timestamp before persisting.
     * Establece automáticamente la marca de tiempo antes de persistir.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
