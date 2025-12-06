package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.AdminThreadActionDto;
import com.focalizze.Focalizze.dto.ReportResponseDto;
import com.focalizze.Focalizze.dto.SuspendRequestDto;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.ReportRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    // --- REPORTES DE USUARIOS ---

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getPendingReports(Pageable pageable) {
        // Busca reportes donde reportedThread es NULL (es decir, reportes a perfiles)
        return reportRepository.findUserReportsByStatus(ReportStatus.PENDING, pageable)
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

            // Calcular fecha de fin de suspension
            int days = request.suspensionDays() != null ? request.suspensionDays() : 1;
            reportedUser.setSuspensionEndsAt(LocalDateTime.now().plusDays(days));

            userRepository.save(reportedUser);
            report.setStatus(ReportStatus.ACTION_TAKEN);
        }

        reportRepository.save(report);
    }

    // --- REPORTES DE HILOS ---

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getPendingThreadReports(Pageable pageable) {
        // Busca reportes donde reportedThread NO es NULL
        return reportRepository.findThreadReportsByStatus(ReportStatus.PENDING, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public void processThreadReport(AdminThreadActionDto request) {
        Report report = reportRepository.findById(request.reportId())
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));

        ThreadClass thread = report.getThread();
        if (thread == null) {
            throw new RuntimeException("El reporte ID " + request.reportId() + " no contiene un hilo asociado.");
        }

        switch (request.action().toUpperCase()) {
            case "DISMISS" -> report.setStatus(ReportStatus.DISMISSED);

            case "DELETE" -> {
                thread.setDeleted(true); // Delete logico
                report.setStatus(ReportStatus.ACTION_TAKEN);
            }

            case "EDIT" -> {
                // Actualizamos el contenido de los posts
                List<Post> posts = thread.getPosts();

                // Validamos y actualizamos cada post si se envió contenido nuevo
                if (request.newContentPost1() != null && !posts.isEmpty()) {
                    posts.get(0).setContent(request.newContentPost1());
                }
                if (request.newContentPost2() != null && posts.size() > 1) {
                    posts.get(1).setContent(request.newContentPost2());
                }
                if (request.newContentPost3() != null && posts.size() > 2) {
                    posts.get(2).setContent(request.newContentPost3());
                }

                report.setStatus(ReportStatus.ACTION_TAKEN);
            }
            default -> throw new IllegalArgumentException("Acción no válida: " + request.action());
        }

        reportRepository.save(report);
    }

    @Override
    public void deleteAdmin(Long id) {
        User user = userRepository.findById(id).
                orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if(user.getRole().equals(UserRole.ADMIN)){
            user.setRole(UserRole.USER);
            userRepository.save(user);
        }
    }

    // --- MAPPER COMÚN ---

    private ReportResponseDto mapToDto(Report report) {
        // Lógica para determinar si es un reporte de hilo y extraer datos
        Long threadId = null;
        String threadPreview = null;

        if (report.getThread() != null) {
            threadId = report.getThread().getId();

            // Generar preview del primer post
            if (!report.getThread().getPosts().isEmpty()) {
                String fullContent = report.getThread().getPosts().get(0).getContent();
                threadPreview = fullContent.length() > 60
                        ? fullContent.substring(0, 60) + "..."
                        : fullContent;
            }
        }

        // Asumiendo que actualizaste ReportResponseDto para tener 10 campos
        return new ReportResponseDto(
                report.getId(),
                report.getUserReporter().getUsername(),
                report.getUserReporter().getAvatarUrl(defaultAvatarUrl),
                report.getUserReported().getUsername(),
                report.getUserReported().getAvatarUrl(defaultAvatarUrl),
                report.getReason(),
                report.getDescription(),
                report.getCreatedAt(),
                threadId,       // Nuevo campo: ID del hilo (puede ser null)
                threadPreview   // Nuevo campo: Preview del texto (puede ser null)
        );
    }
}
