package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Report;
import com.focalizze.Focalizze.models.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus pending, Pageable pageable);
}
