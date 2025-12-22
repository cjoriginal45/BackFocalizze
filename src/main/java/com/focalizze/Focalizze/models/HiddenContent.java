package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing content explicitly hidden by a user.
 * Used to filter feeds and improve personalization.
 * <p>
 * Entidad que representa contenido ocultado explícitamente por un usuario.
 * Utilizado para filtrar feeds y mejorar la personalización.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "hidden_content_tbl", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "thread_id"})
})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HiddenContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The user who hid the content.
     * El usuario que ocultó el contenido.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /**
     * The thread that was hidden.
     * El hilo que fue ocultado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    @ToString.Exclude
    private ThreadClass thread;

    /**
     * The reason provided for hiding the content (e.g., "Not interested").
     * La razón proporcionada para ocultar el contenido (ej. "No me interesa").
     */
    private String reasonType;

    /**
     * Timestamp when the action occurred.
     * Marca de tiempo de cuándo ocurrió la acción.
     */
    private LocalDateTime hiddenAt;

    /**
     * Automatically sets the timestamp on creation.
     * Establece automáticamente la marca de tiempo al crear.
     */
    @PrePersist
    protected void onCreate() {
        hiddenAt = LocalDateTime.now();
    }
}