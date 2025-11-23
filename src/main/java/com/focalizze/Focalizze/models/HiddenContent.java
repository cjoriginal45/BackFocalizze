package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hidden_content_tbl", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "thread_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HiddenContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    private ThreadClass thread;

    private String reasonType;

    private LocalDateTime hiddenAt;

    @PrePersist
    protected void onCreate() { hiddenAt = LocalDateTime.now(); }
}