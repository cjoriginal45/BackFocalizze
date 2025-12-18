package com.focalizze.Focalizze.models;


import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing an image attached to a thread.
 * <p>
 * Entidad que representa una imagen adjunta a un hilo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "thread_image_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ThreadImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The URL or path to the stored image file.
     * La URL o ruta al archivo de imagen almacenado.
     */
    @Column(nullable = false)
    private String imageUrl;

    /**
     * The thread to which this image belongs.
     * El hilo al que pertenece esta imagen.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    @ToString.Exclude
    private ThreadClass thread;
}
