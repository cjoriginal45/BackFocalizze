package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.*;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.ReportRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User adminUser;
    private User regularUser;
    private User reportedUser;
    private Report userReport;
    private Report threadReport;
    private ThreadClass thread;

    @BeforeEach
    void setUp() {
        // Inyectar valor para @Value
        ReflectionTestUtils.setField(adminService, "defaultAvatarUrl", "default.png");

        adminUser = User.builder().id(1L).username("admin").password("encodedPass").role(UserRole.ADMIN).build();
        regularUser = User.builder().id(2L).username("user").role(UserRole.USER).build();
        reportedUser = User.builder().id(3L).username("badguy").role(UserRole.USER).build();

        // Reporte de Usuario
        userReport = Report.builder()
                .id(100L)
                .userReporter(regularUser)
                .userReported(reportedUser)
                .reason(ReportReason.SPAM)
                .status(ReportStatus.PENDING)
                .build();

        // Hilo y Reporte de Hilo
        thread = new ThreadClass();
        thread.setId(500L);
        thread.setUser(reportedUser);
        thread.setPosts(new ArrayList<>(List.of(
                Post.builder().content("Bad content").build()
        )));

        threadReport = Report.builder()
                .id(200L)
                .userReporter(regularUser)
                .userReported(reportedUser)
                .thread(thread)
                .reason(ReportReason.INAPPROPRIATE_CONTENT)
                .status(ReportStatus.PENDING)
                .build();
    }

    // --- TESTS: REPORTES DE USUARIOS ---

    @Test
    @DisplayName("getPendingReports: Debería devolver página de reportes mapeados")
    void getPendingReports_ShouldReturnMappedDtos() {
        // Given
        Page<Report> reportPage = new PageImpl<>(List.of(userReport));
        given(reportRepository.findUserReportsByStatus(eq(ReportStatus.PENDING), any(Pageable.class)))
                .willReturn(reportPage);

        // When
        Page<ReportResponseDto> result = adminService.getPendingReports(Pageable.unpaged());

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).reportedUsername()).isEqualTo("badguy");
        // Verificar que el threadId es null para reporte de usuario
        assertThat(result.getContent().get(0).reportedThreadId()).isNull();
    }

    @Test
    @DisplayName("processReport (SUSPEND): Debería suspender al usuario y actualizar reporte")
    void processReport_Suspend_ShouldUpdateUserAndReport() {
        // Given
        SuspendRequestDto request = new SuspendRequestDto(100L, "SUSPEND", 7);
        given(reportRepository.findById(100L)).willReturn(Optional.of(userReport));

        // When
        adminService.processReport(request);

        // Then
        assertThat(reportedUser.getSuspensionEndsAt()).isAfter(LocalDateTime.now());
        assertThat(userReport.getStatus()).isEqualTo(ReportStatus.ACTION_TAKEN);

        verify(userRepository).save(reportedUser);
        verify(reportRepository).save(userReport);
    }

    @Test
    @DisplayName("processReport (DISMISS): Debería solo cambiar estado a DISMISSED")
    void processReport_Dismiss_ShouldUpdateStatusOnly() {
        // Given
        SuspendRequestDto request = new SuspendRequestDto(100L, "DISMISS", null);
        given(reportRepository.findById(100L)).willReturn(Optional.of(userReport));

        // When
        adminService.processReport(request);

        // Then
        assertThat(userReport.getStatus()).isEqualTo(ReportStatus.DISMISSED);
        verify(userRepository, never()).save(any()); // No se toca al usuario
        verify(reportRepository).save(userReport);
    }

    // --- TESTS: REPORTES DE HILOS ---

    @Test
    @DisplayName("processThreadReport (DELETE): Debería marcar hilo como borrado (soft delete)")
    void processThreadReport_Delete_ShouldSoftDeleteThread() {
        // Given
        AdminThreadActionDto request = new AdminThreadActionDto(200L, "DELETE", null, null, null);
        given(reportRepository.findById(200L)).willReturn(Optional.of(threadReport));

        // When
        adminService.processThreadReport(request);

        // Then
        assertThat(thread.isDeleted()).isTrue();
        assertThat(threadReport.getStatus()).isEqualTo(ReportStatus.ACTION_TAKEN);
        verify(reportRepository).save(threadReport);
    }

    @Test
    @DisplayName("processThreadReport (EDIT): Debería actualizar contenido de posts")
    void processThreadReport_Edit_ShouldUpdatePosts() {
        // Given
        AdminThreadActionDto request = new AdminThreadActionDto(200L, "EDIT", "Clean content", null, null);
        given(reportRepository.findById(200L)).willReturn(Optional.of(threadReport));

        // When
        adminService.processThreadReport(request);

        // Then
        assertThat(thread.getPosts().get(0).getContent()).isEqualTo("Clean content");
        assertThat(threadReport.getStatus()).isEqualTo(ReportStatus.ACTION_TAKEN);
    }

    // --- TESTS: GESTIÓN DE ADMINS ---

    @Test
    @DisplayName("promoteUserToAdmin: Debería promover si la contraseña es correcta")
    void promoteUserToAdmin_Success() {
        // Given
        PromoteAdminDto dto = new PromoteAdminDto("user", "correctPass");
        given(passwordEncoder.matches("correctPass", adminUser.getPassword())).willReturn(true);
        given(userRepository.findByUsername("user")).willReturn(Optional.of(regularUser));

        // When
        adminService.promoteUserToAdmin(dto, adminUser);

        // Then
        assertThat(regularUser.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).save(regularUser);
    }

    @Test
    @DisplayName("promoteUserToAdmin: Debería lanzar excepción si la contraseña es incorrecta")
    void promoteUserToAdmin_WrongPassword_ThrowsException() {
        // Given
        PromoteAdminDto dto = new PromoteAdminDto("user", "wrongPass");
        given(passwordEncoder.matches("wrongPass", adminUser.getPassword())).willReturn(false);

        // When & Then
        assertThrows(BadCredentialsException.class, () ->
                adminService.promoteUserToAdmin(dto, adminUser)
        );
        verify(userRepository, never()).save(any());
    }

    // --- TESTS: BANEO ---

    @Test
    @DisplayName("banUser: Debería banear y revocar tokens si contraseña correcta")
    void banUser_Success() {
        // Given
        BanUserRequestDto dto = new BanUserRequestDto("badguy", "Spamming", "WEEK", "correctPass");
        given(passwordEncoder.matches("correctPass", adminUser.getPassword())).willReturn(true);
        given(userRepository.findByUsername("badguy")).willReturn(Optional.of(reportedUser));

        // When
        adminService.banUser(dto, adminUser);

        // Then
        assertThat(reportedUser.isBanned()).isTrue();
        assertThat(reportedUser.getBanExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(reportedUser.getBanReason()).isEqualTo("Spamming");
        assertThat(reportedUser.getTokenVersion()).isEqualTo(1); // Incrementado

        verify(userRepository).save(reportedUser);
    }

    @Test
    @DisplayName("banUser: No debería permitir banear a un Admin")
    void banUser_TryToBanAdmin_ThrowsException() {
        // Given
        User targetAdmin = User.builder().id(5L).username("otherAdmin").role(UserRole.ADMIN).build();
        BanUserRequestDto dto = new BanUserRequestDto("otherAdmin", "Bad admin", "PERMANENT", "correctPass");

        given(passwordEncoder.matches("correctPass", adminUser.getPassword())).willReturn(true);
        given(userRepository.findByUsername("otherAdmin")).willReturn(Optional.of(targetAdmin));

        // When & Then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                adminService.banUser(dto, adminUser)
        );
        assertThat(ex.getMessage()).contains("No se puede banear a un Administrador");
    }
}
