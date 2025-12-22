package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a single content block within a thread.
 * A thread is composed of a sequence of posts.
 * <p>
 * Entidad que representa un bloque de contenido único dentro de un hilo.
 * Un hilo se compone de una secuencia de posts.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "post_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The textual content of the post.
     * El contenido textual del post.
     */
    @Column(nullable = false)
    @Size(max = 1000)
    private String content;

    /**
     * The order index of this post within the thread (1, 2, 3...).
     * El índice de orden de este post dentro del hilo (1, 2, 3...).
     */
    @Column(nullable = false)
    private Integer position;

    /**
     * Maximum characters allowed for this specific post.
     * Caracteres máximos permitidos para este post específico.
     */
    private Integer characterLimit;

    /**
     * Creation timestamp.
     * Marca de tiempo de creación.
     */
    private LocalDateTime createdAt;

    /**
     * The thread to which this post belongs.
     * El hilo al que pertenece este post.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    @ToString.Exclude
    private ThreadClass thread;

    /**
     * List of user mentions contained in this post.
     * Lista de menciones de usuario contenidas en este post.
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<Mention> mentions = new ArrayList<>();

}
