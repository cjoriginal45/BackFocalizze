package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.AdminThreadActionDto;
import com.focalizze.Focalizze.dto.PromoteAdminDto;
import com.focalizze.Focalizze.dto.ReportResponseDto;
import com.focalizze.Focalizze.dto.SuspendRequestDto;
import com.focalizze.Focalizze.models.Report;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    Page<ReportResponseDto> getPendingReports(Pageable pageable);

    void processReport(SuspendRequestDto request);

    Page<ReportResponseDto> getPendingThreadReports(Pageable pageable);

    void processThreadReport(AdminThreadActionDto request);

    void promoteUserToAdmin(PromoteAdminDto dto, User currentAdmin);
    void deleteAdmin(String username);
}
