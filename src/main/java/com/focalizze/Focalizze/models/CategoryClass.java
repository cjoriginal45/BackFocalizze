package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a discussion category.
 * <p>
 * Entidad que representa una categoría de discusión.
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "category_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CategoryClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The unique name of the category.
     * El nombre único de la categoría.
     */
    @Column(unique = true, nullable = false)
    private String name;

    /**
     * A brief description of the category.
     * Una breve descripción de la categoría.
     */
    private String description;

    /**
     * URL to the category image/icon.
     * URL a la imagen/icono de la categoría.
     */
    private String imageUrl;

    /**
     * Users who follow this category.
     * Usuarios que siguen esta categoría.
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private Set<CategoryFollow> followers = new HashSet<>();

    /**
     * Threads associated with this category.
     * Hilos asociados con esta categoría.
     */
    @OneToMany(mappedBy = "category")
    @Builder.Default
    @ToString.Exclude
    private List<ThreadClass> threads = new ArrayList<>();

    /**
     * Cached count of followers for performance optimization.
     * Conteo de seguidores en caché para optimización de rendimiento.
     */
    @Builder.Default
    private Integer followersCount = 0;

    /**
     * Constructor for creating a category by name.
     * Constructor para crear una categoría por nombre.
     *
     * @param name The category name. / El nombre de la categoría.
     */
    public CategoryClass(String name) {
        this.name = name;
        this.threads = new ArrayList<>();
        this.followers = new HashSet<>();
    }
}
