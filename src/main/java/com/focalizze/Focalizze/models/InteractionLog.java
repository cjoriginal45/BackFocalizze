package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for auditing user interactions.
 * Used primarily for enforcing daily limits and activity tracking.
 * <p>
 * Entidad para auditar interacciones de usuarios.
 * Utilizada principalmente para hacer cumplir límites diarios y seguimiento de actividad.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "interaction_log_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InteractionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The user performing the interaction.
     * El usuario que realiza la interacción.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /**
     * The type of interaction performed (LIKE, COMMENT).
     * El tipo de interacción realizada (LIKE, COMENTARIO).
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InteractionType type;

    /**
     * Timestamp of the interaction.
     * Marca de tiempo de la interacción.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
