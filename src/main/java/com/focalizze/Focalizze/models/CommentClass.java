package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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
@Table(name = "comment_tbl")
public class CommentClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Size(max = 281)
    private String content;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name="user_id") //foreign key
    private User user;

    @ManyToOne
    @JoinColumn(name="thread_id")
    private ThreadClass thread;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false; // Valor por defecto es 'false'
}
