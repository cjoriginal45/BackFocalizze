package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.ReportResponseDto;
import com.focalizze.Focalizze.dto.SuspendRequestDto;
import com.focalizze.Focalizze.models.Report;
import com.focalizze.Focalizze.models.ReportStatus;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ReportRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public void processReport(SuspendRequestDto request) {
        Report report = reportRepository.findById(request.reportId())
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));

        if ("DISMISS".equalsIgnoreCase(request.action())) {
            report.setStatus(ReportStatus.DISMISSED);
        } else if ("SUSPEND".equalsIgnoreCase(request.action())) {
            User reportedUser = report.getUserReported();

            // Calcular fecha de fin de suspensión
            int days = request.suspensionDays() != null ? request.suspensionDays() : 1;
            reportedUser.setSuspensionEndsAt(LocalDateTime.now().plusDays(days));

            userRepository.save(reportedUser);

            report.setStatus(ReportStatus.ACTION_TAKEN);
        }

        reportRepository.save(report);
    }

    private ReportResponseDto mapToDto(Report report) {
        return new ReportResponseDto(
                report.getId(),
                report.getUserReporter().getUsername(),
                report.getUserReporter().getAvatarUrl(defaultAvatarUrl),
                report.getUserReported().getUsername(),
                report.getUserReported().getAvatarUrl(defaultAvatarUrl),
                report.getReason(),
                report.getDescription(),
                report.getCreatedAt()
        );
    }
}
