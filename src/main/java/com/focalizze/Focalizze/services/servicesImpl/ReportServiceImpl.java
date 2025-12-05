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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;

    @Override
    @Transactional
    public void reportUser(String usernameToReport, ReportRequestDto request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User reporter = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        User reportedUser = userRepository.findByUsername(usernameToReport)
                .orElseThrow(() -> new RuntimeException("Usuario a reportar no encontrado"));

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

    @Override
    @Transactional
    public void reportThread(Long threadId, ReportRequestDto request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User reporter = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Hilo no encontrado"));

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
