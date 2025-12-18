package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.PasswordResetController;
import com.focalizze.Focalizze.dto.ForgotPasswordRequest;
import com.focalizze.Focalizze.dto.ResetPasswordRequest;
import com.focalizze.Focalizze.services.PasswordResetService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@Import(SecurityConfig.class)
class PasswordResetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PasswordResetService passwordResetService;

    // Security Mocks
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private JwtRequestFilter jwtRequestFilter;

    @BeforeEach
    void setUp() throws Exception {
        // Bypass del filtro JWT
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtRequestFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @Test
    @DisplayName("forgotPassword: Should return 200 OK")
    void forgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@test.com");
        doNothing().when(passwordResetService).processForgotPassword(anyString());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(passwordResetService).processForgotPassword("user@test.com");
    }

    @Test
    @DisplayName("validateToken: Should return 200 OK if token valid")
    void validateToken_Valid_Returns200() throws Exception {
        given(passwordResetService.validateResetToken("valid-token")).willReturn(true);

        mockMvc.perform(get("/api/auth/validate-reset-token")
                        .param("token", "valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("validateToken: Should return 400 Bad Request if token invalid")
    void validateToken_Invalid_Returns400() throws Exception {
        given(passwordResetService.validateResetToken("invalid")).willReturn(false);

        mockMvc.perform(get("/api/auth/validate-reset-token")
                        .param("token", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("resetPassword: Should return 200 OK if success")
    void resetPassword_Success() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "newPass");
        doNothing().when(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("resetPassword: Should return 400 Bad Request if service throws exception")
    void resetPassword_Error_Returns400() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "newPass");
        doThrow(new RuntimeException("Expired")).when(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}