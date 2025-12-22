package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.*;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.ReportRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.AdminService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;


/**
 * Implementation of the {@link AdminService} interface.
 * Handles administrative actions such as managing reports, suspending users, and promoting/demoting admins.
 * <p>
 * Implementación de la interfaz {@link AdminService}.
 * Maneja acciones administrativas como gestionar reportes, suspender usuarios y promover/degradar administradores.
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    // --- REPORTES DE USUARIOS ---

    /**
     * Retrieves a paginated list of pending user reports (reports not associated with a thread).
     * <p>
     * Recupera una lista paginada de reportes de usuarios pendientes (reportes no asociados con un hilo).
     *
     * @param pageable Pagination information. / Información de paginación.
     * @return A {@link Page} of {@link ReportResponseDto}. / Una {@link Page} de {@link ReportResponseDto}.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getPendingReports(Pageable pageable) {
        return reportRepository.findUserReportsByStatus(ReportStatus.PENDING, pageable)
                .map(this::mapToDto);
    }

    /**
     * Processes a user report, either dismissing it or suspending the user.
     * <p>
     * Procesa un reporte de usuario, ya sea descartándolo o suspendiendo al usuario.
     *
     * @param request The request containing the report ID and action to take.
     *                La solicitud que contiene el ID del reporte y la acción a tomar.
     * @throws EntityNotFoundException If the report is not found.
     *                                 Si no se encuentra el reporte.
     */
    @Override
    @Transactional
    public void processReport(SuspendRequestDto request) {
        Report report = reportRepository.findById(request.reportId())
                .orElseThrow(() -> new EntityNotFoundException("Report not found / Reporte no encontrado: " + request.reportId()));

        if ("DISMISS".equalsIgnoreCase(request.action())) {
            report.setStatus(ReportStatus.DISMISSED);
        } else if ("SUSPEND".equalsIgnoreCase(request.action())) {
            User reportedUser = report.getUserReported();

            int days = request.suspensionDays() != null ? request.suspensionDays() : 1;
            reportedUser.setSuspensionEndsAt(LocalDateTime.now().plusDays(days));

            userRepository.save(reportedUser);
            report.setStatus(ReportStatus.ACTION_TAKEN);
        } else {
            throw new IllegalArgumentException("Invalid action / Acción no válida: " + request.action());
        }

        reportRepository.save(report);
    }

    // --- REPORTES DE HILOS ---

    /**
     * Retrieves a paginated list of pending thread reports.
     * <p>
     * Recupera una lista paginada de reportes de hilos pendientes.
     *
     * @param pageable Pagination information. / Información de paginación.
     * @return A {@link Page} of {@link ReportResponseDto}. / Una {@link Page} de {@link ReportResponseDto}.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getPendingThreadReports(Pageable pageable) {
        return reportRepository.findThreadReportsByStatus(ReportStatus.PENDING, pageable)
                .map(this::mapToDto);
    }

    /**
     * Processes a thread report (Dismiss, Delete Thread, or Edit Posts).
     * <p>
     * Procesa un reporte de hilo (Descartar, Eliminar Hilo o Editar Posts).
     *
     * @param request DTO containing the action details.
     *                DTO que contiene los detalles de la acción.
     * @throws EntityNotFoundException If report or thread is missing.
     *                                 Si falta el reporte o el hilo.
     */
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
                List<Post> posts = thread.getPosts();

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
            default -> throw new IllegalArgumentException("Invalid action / Acción no válida: " + request.action());
        }

        reportRepository.save(report);
    }

    // --- MAPPER COMÚN ---

    /**
     * Maps a {@link Report} entity to a {@link ReportResponseDto}.
     * Handles logic for extracting thread details if present.
     * <p>
     * Mapea una entidad {@link Report} a un {@link ReportResponseDto}.
     * Maneja la lógica para extraer detalles del hilo si están presentes.
     *
     * @param report The entity to map. / La entidad a mapear.
     * @return The DTO. / El DTO.
     */
    private ReportResponseDto mapToDto(Report report) {
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

    /**
     * Promotes a regular user to Administrator role.
     * Requires the current admin's password for verification.
     * <p>
     * Promueve a un usuario regular al rol de Administrador.
     * Requiere la contraseña del administrador actual para verificación.
     *
     * @param dto          DTO containing target username and admin password.
     *                     DTO con el nombre de usuario objetivo y contraseña de admin.
     * @param currentAdmin The authenticated admin performing the action.
     *                     El admin autenticado que realiza la acción.
     * @throws BadCredentialsException If password does not match.
     *                                 Si la contraseña no coincide.
     */
    @Override
    @Transactional
    public void promoteUserToAdmin(PromoteAdminDto dto, User currentAdmin) {
        if (!passwordEncoder.matches(dto.adminPassword(), currentAdmin.getPassword())) {
            throw new BadCredentialsException("La contraseña del administrador es incorrecta.");
        }

        User targetUser = userRepository.findByUsername(dto.targetUsername())
                .orElseThrow(() -> new UsernameNotFoundException("El usuario @" + dto.targetUsername() + " no existe."));

        if (targetUser.getRole() == UserRole.ADMIN) {
            throw new IllegalStateException("El usuario @" + targetUser.getUsername() + " ya es Administrador.");
        }

        targetUser.setRole(UserRole.ADMIN);
        userRepository.save(targetUser);
    }

    /**
     * Revokes Administrator role from a user (Downgrade to USER).
     * <p>
     * Revoca el rol de Administrador de un usuario (Degradar a USUARIO).
     *
     * @param dto          DTO containing target username and admin password.
     *                     DTO con el nombre de usuario objetivo y contraseña de admin.
     * @param currentAdmin The authenticated admin performing the action.
     *                     El admin autenticado que realiza la acción.
     */
    @Transactional
    public void revokeAdminRole(RevokeAdminDto dto, User currentAdmin) {
        if (!passwordEncoder.matches(dto.adminPassword(), currentAdmin.getPassword())) {
            throw new BadCredentialsException("La contraseña del administrador es incorrecta.");
        }

        User targetUser = userRepository.findByUsername(dto.targetUsername())
                .orElseThrow(() -> new UsernameNotFoundException("El usuario @" + dto.targetUsername() + " no existe."));

        if (targetUser.getId().equals(currentAdmin.getId())) {
            throw new IllegalStateException("No puedes quitarte el rol de administrador a ti mismo desde esta pantalla.");
        }

        if (!targetUser.getRole().equals(UserRole.ADMIN)) {
            throw new IllegalStateException("El usuario @" + targetUser.getUsername() + " no es Administrador.");
        }

        targetUser.setRole(UserRole.USER);
        userRepository.save(targetUser);
    }

    /**
     * Bans a user from the platform for a specified duration.
     * <p>
     * Banea a un usuario de la plataforma por una duración especificada.
     *
     * @param dto          DTO containing ban details.
     *                     DTO con detalles del baneo.
     * @param currentAdmin The authenticated admin.
     *                     El admin autenticado.
     */
    @Override
    @Transactional
    public void banUser(BanUserRequestDto dto, User currentAdmin) {
        if (!passwordEncoder.matches(dto.adminPassword(), currentAdmin.getPassword())) {
            throw new BadCredentialsException("La contraseña del administrador es incorrecta.");
        }

        User targetUser = userRepository.findByUsername(dto.targetUsername())
                .orElseThrow(() -> new UsernameNotFoundException("El usuario @" + dto.targetUsername() + " no existe."));

        if (targetUser.getId().equals(currentAdmin.getId())) {
            throw new IllegalStateException("No puedes banearte a ti mismo.");
        }
        if (targetUser.getRole() == UserRole.ADMIN) {
            throw new IllegalStateException("No se puede banear a un Administrador. Primero revoca su rol.");
        }

        LocalDateTime expirationDate = null;
        switch (dto.duration().toUpperCase()) {
            case "WEEK" -> expirationDate = LocalDateTime.now().plusWeeks(1);
            case "MONTH" -> expirationDate = LocalDateTime.now().plusMonths(1);
            case "PERMANENT" -> expirationDate = null; // Null representa permanente
            default -> throw new IllegalArgumentException("Duración no válida.");
        }

        targetUser.setBanned(true);
        targetUser.setBanExpiresAt(expirationDate);
        targetUser.setBanReason(dto.reason());

        targetUser.setTokenVersion(targetUser.getTokenVersion() + 1);

        userRepository.save(targetUser);
    }
}
