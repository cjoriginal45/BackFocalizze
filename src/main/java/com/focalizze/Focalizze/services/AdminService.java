package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.*;
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

    void revokeAdminRole(RevokeAdminDto dto, User currentAdmin);
}
