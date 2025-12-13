package com.focalizze.Focalizze.repositoryTest;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class ReportRepositoryTest {
    @Autowired
    private ReportRepository reportRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User reporter;
    private User reportedUser;
    private ThreadClass reportedThread;

    @BeforeEach
    void setUp() {
        reporter = createUser("reporter", "reporter@test.com");
        reportedUser = createUser("badGuy", "bad@test.com");

        var category = new com.focalizze.Focalizze.models.CategoryClass();
        category.setName("General");
        category.setDescription("Desc");
        entityManager.persist(category);

        reportedThread = new ThreadClass();
        reportedThread.setUser(reportedUser);
        reportedThread.setCategory(category);
        reportedThread.setPublishedAt(LocalDateTime.now());
        reportedThread.setPublished(true);
        entityManager.persist(reportedThread);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @DisplayName("findUserReportsByStatus: Debería devolver reportes donde thread es NULL y estado coincide")
    void findUserReports_ShouldReturnOnlyUserReports() {
        // Given
        // 1. Reporte a Usuario (PENDING) -> Guardamos la referencia
        Report myReport = createReport(reporter, reportedUser, null, ReportStatus.PENDING);

        // 2. Otros reportes para confundir
        createReport(reporter, reportedUser, null, ReportStatus.DISMISSED);
        createReport(reporter, reportedUser, reportedThread, ReportStatus.PENDING);

        entityManager.flush();

        // When
        Page<Report> result = reportRepository.findUserReportsByStatus(ReportStatus.PENDING, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent())
                .extracting(Report::getId)
                .contains(myReport.getId());

        // Verificamos que NINGUNO de los resultados tenga thread (filtro correcto)
        assertThat(result.getContent()).allMatch(r -> r.getThread() == null);

        // Verificamos que TODOS tengan el estado correcto
        assertThat(result.getContent()).allMatch(r -> r.getStatus() == ReportStatus.PENDING);
    }

    @Test
    @DisplayName("findThreadReportsByStatus: Debería devolver reportes donde thread NO es NULL y estado coincide")
    void findThreadReports_ShouldReturnOnlyThreadReports() {
        // Given
        // 1. Reporte a Hilo (PENDING) -> Guardamos referencia
        Report myThreadReport = createReport(reporter, reportedUser, reportedThread, ReportStatus.PENDING);

        // 2. Otros reportes
        createReport(reporter, reportedUser, null, ReportStatus.PENDING);
        createReport(reporter, reportedUser, reportedThread, ReportStatus.ACTION_TAKEN);

        entityManager.flush();

        // When
        Page<Report> result = reportRepository.findThreadReportsByStatus(ReportStatus.PENDING, PageRequest.of(0, 10));

        // Then
        // CAMBIO: Verificamos presencia por ID
        assertThat(result.getContent())
                .extracting(Report::getId)
                .contains(myThreadReport.getId());

        // Verificamos filtros
        assertThat(result.getContent()).allMatch(r -> r.getThread() != null);
        assertThat(result.getContent()).allMatch(r -> r.getStatus() == ReportStatus.PENDING);
    }

    // --- Helpers ---
    private User createUser(String username, String email) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password("pass")
                .displayName(username)
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(user);
        return user;
    }

    // CAMBIO: Ahora devuelve el Report creado
    private Report createReport(User reporter, User reported, ThreadClass thread, ReportStatus status) {
        Report report = Report.builder()
                .userReporter(reporter)
                .userReported(reported)
                .thread(thread)
                .reason(ReportReason.SPAM)
                .description("Test desc")
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(report);
        return report;
    }
}
