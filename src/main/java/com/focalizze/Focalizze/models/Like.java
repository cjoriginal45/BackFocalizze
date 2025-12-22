package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a "Like" interaction on a thread.
 * Links a user to a thread they appreciate.
 * <p>
 * Entidad que representa una interacción "Me gusta" en un hilo.
 * Vincula a un usuario con un hilo que le agrada.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "like_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Timestamp when the like was created.
     * Marca de tiempo de cuándo se creó el "me gusta".
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically sets the timestamp before persisting.
     * Establece automáticamente la marca de tiempo antes de persistir.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="thread_id", nullable = false)
    @ToString.Exclude
    private ThreadClass thread;
}
