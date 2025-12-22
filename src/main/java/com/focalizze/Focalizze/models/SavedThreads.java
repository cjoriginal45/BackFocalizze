package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a "Saved" or "Bookmarked" thread interaction.
 * Allows users to save content to view later.
 * <p>
 * Entidad que representa una interacción de "Guardado" o "Marcador" en un hilo.
 * Permite a los usuarios guardar contenido para verlo más tarde.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "saved_threads_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SavedThreads {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who saved the thread.
     * El usuario que guardó el hilo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /**
     * The thread that was saved.
     * El hilo que fue guardado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    @ToString.Exclude
    private ThreadClass thread;

    /**
     * Timestamp when the user saved the thread.
     * Marca de tiempo de cuándo el usuario guardó el hilo.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically sets the creation timestamp.
     * Establece automáticamente la marca de tiempo de creación.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
