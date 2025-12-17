package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.AdminController;
import com.focalizze.Focalizze.dto.*;
import com.focalizze.Focalizze.models.ReportReason;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.services.AdminService;
import com.focalizze.Focalizze.services.BackupService;
import com.focalizze.Focalizze.utils.JwtRequestFilter;
import com.focalizze.Focalizze.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
public class AdminControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Dependencias del controlador
    @MockitoBean private AdminService adminService;
    @MockitoBean private BackupService backupService;

    // Dependencias de SecurityConfig
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private JwtRequestFilter jwtRequestFilter;

    private User adminUser;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .role(UserRole.ADMIN)
                .build();

        // 2. CONFIGURACIÓN CRÍTICA DEL FILTRO MOCKEADO
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res); // Continuar la cadena
            return null;
        }).when(jwtRequestFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("getUserReports: Should return page of reports")
    void getUserReports_Success() throws Exception {
        // Given
        ReportResponseDto report = new ReportResponseDto(1L, "rep", "url", "bad", "url", ReportReason.SPAM, "desc", LocalDateTime.now(), null, null);
        Page<ReportResponseDto> page = new PageImpl<>(List.of(report));
        given(adminService.getPendingReports(any(Pageable.class))).willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/admin/reports/users")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }


    @Test
    @DisplayName("processSuspension: Should return 200 OK")
    void processSuspension_Success() throws Exception {
        SuspendRequestDto request = new SuspendRequestDto(1L, "SUSPEND", 7);
        doNothing().when(adminService).processReport(any());

        mockMvc.perform(post("/api/admin/suspend")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("banUser: Should return 403 Forbidden if password incorrect (Exception Handling)")
    void banUser_BadCredentials_Returns403() throws Exception {
        // Given
        BanUserRequestDto request = new BanUserRequestDto("target", "wrongPass", "WEEK", "Reason");

        // Simulamos la excepción del servicio
        doThrow(new BadCredentialsException("Password incorrect")).when(adminService).banUser(any(), any());

        // When & Then
        mockMvc.perform(post("/api/admin/ban")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("downloadBackup: Should return file stream")
    void downloadBackup_Success() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream("Excel Content".getBytes());
        given(backupService.generateExcelBackup()).willReturn(stream);

        mockMvc.perform(get("/api/admin/backup/download")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
    }
}
