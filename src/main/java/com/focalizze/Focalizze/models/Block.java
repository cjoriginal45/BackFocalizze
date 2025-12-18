package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a block relationship between two users.
 * <p>
 * Entidad que representa una relación de bloqueo entre dos usuarios.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "block_tbl",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
)
public class Block {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The user who performed the block.
     * El usuario que realizó el bloqueo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    @ToString.Exclude
    private User blocker;

    /**
     * The user who was blocked.
     * El usuario que fue bloqueado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    @ToString.Exclude
    private User blocked;

    /**
     * Timestamp when the block occurred.
     * Marca de tiempo de cuándo ocurrió el bloqueo.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
