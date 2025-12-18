package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.ReportController;
import com.focalizze.Focalizze.dto.ReportRequestDto;
import com.focalizze.Focalizze.models.ReportReason;
import com.focalizze.Focalizze.models.User;

import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.services.ReportService;
import com.focalizze.Focalizze.utils.JwtRequestFilter;
import com.focalizze.Focalizze.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ReportService reportService;

    // Security Mocks
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private JwtRequestFilter jwtRequestFilter;

    private User currentUser;

    @BeforeEach
    void setUp() throws Exception {
        // Bypass del filtro JWT
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtRequestFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        currentUser = User.builder().id(1L).username("reporter").role(UserRole.USER).build();
    }

    @Test
    @DisplayName("reportUser: Should return 200 OK")
    void reportUser_Success() throws Exception {
        // Given
        ReportRequestDto request = new ReportRequestDto(ReportReason.SPAM, "Bot account");

        // Simulamos autenticación (normalmente reportar requiere estar logueado)
        authenticateUser();

        // When & Then
        mockMvc.perform(post("/api/reports/users/targetUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verificar que se llamó al servicio
        verify(reportService).reportUser(eq("targetUser"), any(ReportRequestDto.class));
    }

    @Test
    @DisplayName("reportThread: Should return 200 OK")
    void reportThread_Success() throws Exception {
        // Given
        ReportRequestDto request = new ReportRequestDto(ReportReason.INAPPROPRIATE_CONTENT, "Bad content");
        authenticateUser();

        // When & Then
        mockMvc.perform(post("/api/reports/threads/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(reportService).reportThread(eq(100L), any(ReportRequestDto.class));
    }

    // Helper para autenticación manual
    private void authenticateUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
