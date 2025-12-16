package com.focalizze.Focalizze.models;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "thread_image_tbl")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    @ToString.Exclude
    private ThreadClass thread;
}
