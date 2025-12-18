package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.SecurityController;
import com.focalizze.Focalizze.dto.TwoFactorRequestDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.services.SecurityService;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityController.class)
@Import(SecurityConfig.class)
class SecurityControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SecurityService securityService;

    // Security Mocks
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private JwtRequestFilter jwtRequestFilter;

    private User currentUser;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtRequestFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        currentUser = User.builder().id(1L).username("user").role(UserRole.USER).build();
    }

    @Test
    @DisplayName("toggleTwoFactor: Should call service and return 200 OK")
    void toggleTwoFactor_Success() throws Exception {
        // Given
        authenticateUser();
        TwoFactorRequestDto request = new TwoFactorRequestDto(true);

        // When & Then
        mockMvc.perform(patch("/api/security/2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(securityService).toggleTwoFactor(eq(true), any(User.class));
    }

    @Test
    @DisplayName("logoutAllDevices: Should call service and return 200 OK")
    void logoutAllDevices_Success() throws Exception {
        // Given
        authenticateUser();

        // When & Then
        mockMvc.perform(post("/api/security/logout-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(securityService).logoutAllDevices(any(User.class));
    }

    @Test
    @DisplayName("validatePassword: Should return true (200 OK) if matches")
    void validatePassword_Correct_ReturnsTrue() throws Exception {
        // Given
        authenticateUser();
        given(securityService.validatePassword("correctPass", currentUser)).willReturn(true);
        Map<String, String> request = Map.of("password", "correctPass");

        // When & Then
        mockMvc.perform(post("/api/security/validate-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("validatePassword: Should return false (403 Forbidden) if mismatches")
    void validatePassword_Incorrect_ReturnsForbidden() throws Exception {
        // Given
        authenticateUser();
        // Nota: currentUser inyectado por @AuthenticationPrincipal es el mismo objeto que configuramos en setUp
        given(securityService.validatePassword("wrongPass", currentUser)).willReturn(false);
        Map<String, String> request = Map.of("password", "wrongPass");

        // When & Then
        mockMvc.perform(post("/api/security/validate-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("false"));
    }

    // Helper
    private void authenticateUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
