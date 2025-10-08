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

    private String reason;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private User userReporter;      // Usuario que hace el reporte

    @ManyToOne
    @JoinColumn(name = "user_reported_id") // Cambié el nombre para claridad
    private User userReported;      // Usuario que es reportado

    @ManyToOne
    @JoinColumn(name="thread_id")
    private ThreadClass thread;
}
