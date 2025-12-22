package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a "Follow" relationship between two users.
 * Establishes a directed link where one user follows another.
 * <p>
 * Entidad que representa una relación de "Seguimiento" entre dos usuarios.
 * Establece un vínculo dirigido donde un usuario sigue a otro.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "follow_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The date and time when the follow relationship was created.
     * La fecha y hora en que se creó la relación de seguimiento.
     */
    private LocalDateTime createdAt;

    /**
     * The user who initiates the follow (The Follower).
     * El usuario que inicia el seguimiento (El Seguidor).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_follower_id") // foreign key
    @ToString.Exclude
    private User userFollower;

    /**
     * The user who receives the follow (The Followed).
     * El usuario que recibe el seguimiento (El Seguido).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_followed_id") // foreign key
    @ToString.Exclude
    private User userFollowed;
}
