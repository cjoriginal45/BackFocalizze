package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a user comment on a thread.
 * Supports nested replies (recursive structure).
 * <p>
 * Entidad que representa un comentario de usuario en un hilo.
 * Soporta respuestas anidadas (estructura recursiva).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "comment_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CommentClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The textual content of the comment.
     * El contenido textual del comentario.
     */
    @Column(nullable = false)
    @Size(max = 281)
    private String content;

    /**
     * Creation timestamp.
     * Marca de tiempo de creación.
     */
    private LocalDateTime createdAt;

    /**
     * The author of the comment.
     * El autor del comentario.
     */
    @ManyToOne
    @JoinColumn(name="user_id")
    @ToString.Exclude
    private User user;

    @ManyToOne
    @JoinColumn(name="thread_id")
    @ToString.Exclude
    private ThreadClass thread;

    /**
     * Soft delete flag.
     * Bandera de borrado lógico.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    /**
     * The parent comment if this is a reply.
     * El comentario padre si esto es una respuesta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    private CommentClass parent;

    /**
     * Child comments (Replies).
     * Comentarios hijos (Respuestas).
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<CommentClass> replies = new ArrayList<>();
}
