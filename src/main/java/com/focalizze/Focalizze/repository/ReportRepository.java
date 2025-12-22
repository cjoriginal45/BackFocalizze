package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Report;
import com.focalizze.Focalizze.models.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Report} entities.
 * Handles operations related to content or user reporting (Moderation).
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link Report}.
 * Maneja operaciones relacionadas con reportes de contenido o usuarios (Moderación).
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Retrieves a paginated list of reports targeting Users (where thread is null).
     * Filtered by status (e.g., PENDING, RESOLVED).
     * <p>
     * Recupera una lista paginada de reportes dirigidos a Usuarios (donde el hilo es nulo).
     * Filtrado por estado (ej. PENDING, RESOLVED).
     *
     * @param status   The status of the reports to fetch.
     *                 El estado de los reportes a obtener.
     * @param pageable Pagination and sorting information.
     *                 Información de paginación y ordenamiento.
     * @return A {@link Page} of user reports.
     *         Una {@link Page} de reportes de usuarios.
     */
    @Query(value = "SELECT r FROM Report r WHERE r.status = :status AND r.thread IS NULL ORDER BY r.createdAt DESC",
            countQuery = "SELECT count(r) FROM Report r WHERE r.status = :status AND r.thread IS NULL")
    Page<Report> findUserReportsByStatus(@Param("status") ReportStatus status, Pageable pageable);


    /**
     * Retrieves a paginated list of reports targeting Threads.
     * Filtered by status.
     * <p>
     * Recupera una lista paginada de reportes dirigidos a Hilos.
     * Filtrado por estado.
     *
     * @param status   The status of the reports to fetch.
     *                 El estado de los reportes a obtener.
     * @param pageable Pagination and sorting information.
     *                 Información de paginación y ordenamiento.
     * @return A {@link Page} of thread reports.
     *         Una {@link Page} de reportes de hilos.
     */
    @Query(value = "SELECT r FROM Report r WHERE r.status = :status AND r.thread IS NOT NULL ORDER BY r.createdAt DESC",
            countQuery = "SELECT count(r) FROM Report r WHERE r.status = :status AND r.thread IS NOT NULL")
    Page<Report> findThreadReportsByStatus(@Param("status") ReportStatus status, Pageable pageable);
}
