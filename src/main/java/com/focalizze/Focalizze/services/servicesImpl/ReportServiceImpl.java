package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.ReportRequestDto;
import com.focalizze.Focalizze.models.Report;
import com.focalizze.Focalizze.models.ReportStatus;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ReportRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.ReportService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of the {@link ReportService} interface.
 * Handles reporting of users and threads for moderation.
 * <p>
 * Implementación de la interfaz {@link ReportService}.
 * Maneja el reporte de usuarios e hilos para moderación.
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;


    /**
     * Creates a report against a user profile.
     * <p>
     * Crea un reporte contra un perfil de usuario.
     *
     * @param usernameToReport The username of the target user.
     *                         El nombre de usuario del usuario objetivo.
     * @param request          The report details.
     *                         Los detalles del reporte.
     */
    @Override
    @Transactional
    public void reportUser(String usernameToReport, ReportRequestDto request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User reporter = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Reporter user not found / Usuario reportante no encontrado"));

        User reportedUser = userRepository.findByUsername(usernameToReport)
                .orElseThrow(() -> new EntityNotFoundException("Target user not found / Usuario objetivo no encontrado"));

        if (reporter.getId().equals(reportedUser.getId())) {
            throw new IllegalArgumentException("No puedes reportarte a ti mismo.");
        }

        Report report = Report.builder()
                .userReporter(reporter)
                .userReported(reportedUser)
                .reason(request.reason())
                .description(request.description())
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        reportRepository.save(report);
    }

    /**
     * Creates a report against a thread.
     * <p>
     * Crea un reporte contra un hilo.
     *
     * @param threadId The ID of the thread.
     *                 El ID del hilo.
     * @param request  The report details.
     *                 Los detalles del reporte.
     */
    @Override
    @Transactional
    public void reportThread(Long threadId, ReportRequestDto request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User reporter = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Reporter user not found / Usuario reportante no encontrado"));

        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found / Hilo no encontrado"));

        if (thread.getUser().getId().equals(reporter.getId())) {
            throw new IllegalArgumentException("No puedes reportar tu propio hilo.");
        }

        Report report = Report.builder()
                .userReporter(reporter)
                .userReported(thread.getUser())
                .thread(thread)
                .reason(request.reason())
                .description(request.description())
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        reportRepository.save(report);
    }
}
