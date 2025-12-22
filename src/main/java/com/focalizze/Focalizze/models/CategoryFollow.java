package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Join table entity representing a User following a Category.
 * <p>
 * Entidad de tabla de unión que representa a un Usuario siguiendo una Categoría.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "category_follow_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CategoryFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The user following the category.
     * El usuario que sigue la categoría.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /**
     * The category being followed.
     * La categoría que está siendo seguida.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    private CategoryClass category;

    /**
     * Timestamp of the follow action.
     * Marca de tiempo de la acción de seguir.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime followedAt;

    /**
     * Sets the timestamp before persisting.
     * Establece la marca de tiempo antes de persistir.
     */
    @PrePersist
    protected void onFollow() {
        followedAt = LocalDateTime.now();
    }

}
