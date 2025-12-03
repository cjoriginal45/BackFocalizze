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
@Table(name = "report_tbl")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User userReporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_reported_id")
    private User userReported;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="thread_id")
    private ThreadClass thread;

}
