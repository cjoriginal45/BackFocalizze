package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Report;
import com.focalizze.Focalizze.models.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {
    @Query("SELECT r FROM Report r WHERE r.status = :status AND r.thread IS NULL ORDER BY r.createdAt DESC")
    Page<Report> findUserReportsByStatus(@Param("status") ReportStatus status, Pageable pageable);


    // ====================================================================
    // MÉTODO 2: Reportes de HILOS (donde reportedThread NO es NULL)
    // ====================================================================
    @Query("SELECT r FROM Report r WHERE r.status = :status AND r.thread IS NOT NULL ORDER BY r.createdAt DESC")
    Page<Report> findThreadReportsByStatus(@Param("status") ReportStatus status, Pageable pageable);
}
