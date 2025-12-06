package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.AdminThreadActionDto;
import com.focalizze.Focalizze.dto.ReportResponseDto;
import com.focalizze.Focalizze.dto.SuspendRequestDto;
import com.focalizze.Focalizze.models.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    Page<ReportResponseDto> getPendingReports(Pageable pageable);

    void processReport(SuspendRequestDto request);

    Page<ReportResponseDto> getPendingThreadReports(Pageable pageable);

    void processThreadReport(AdminThreadActionDto request);

    void deleteAdmin(String username);
}
