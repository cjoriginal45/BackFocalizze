package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

/**
 * Entity representing a formal complaint against a user or thread.
 * Used for moderation purposes.
 * <p>
 * Entidad que representa una queja formal contra un usuario o hilo.
 * Utilizado para fines de moderación.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "report_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The categorized reason for the report.
     * La razón categorizada del reporte.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    /**
     * Additional details provided by the reporter.
     * Detalles adicionales proporcionados por el reportante.
     */
    @Column(length = 500)
    private String description;

    /**
     * The current status of the report workflow (e.g., PENDING, RESOLVED).
     * El estado actual del flujo de trabajo del reporte (ej. PENDIENTE, RESUELTO).
     */
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    /**
     * When the report was submitted.
     * Cuándo se envió el reporte.
     */
    private LocalDateTime createdAt;

    /**
     * The user who submitted the report.
     * El usuario que envió el reporte.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User userReporter;

    /**
     * The user being reported (Target).
     * El usuario siendo reportado (Objetivo).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_reported_id")
    @ToString.Exclude
    private User userReported;

    /**
     * The thread being reported (Target, optional if reporting a profile).
     * El hilo siendo reportado (Objetivo, opcional si se reporta un perfil).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    @ToString.Exclude
    private ThreadClass thread;

}
