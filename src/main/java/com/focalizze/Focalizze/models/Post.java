package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "post_tbl")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    private Integer position;

    private Integer characterLimit;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "thread_id", nullable = false)
    private ThreadClass thread;
}
