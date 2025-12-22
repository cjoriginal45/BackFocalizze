package com.focalizze.Focalizze.controllerTest;


import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.SavedThreadsController;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.User;

import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.services.SaveService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SavedThreadsController.class)
@Import(SecurityConfig.class)
class SavedThreadsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SaveService saveService;

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
    @DisplayName("getSavedThreads: Should return paginated threads (200 OK)")
    void getSavedThreads_Success() throws Exception {
        // Given
        authenticateUser();
        Page<FeedThreadDto> page = new PageImpl<>(Collections.emptyList());

        given(saveService.getSavedThreadsForCurrentUser(any(Pageable.class))).willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/saved-threads")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getSavedThreads: Should return 403/401 if not authenticated")
    void getSavedThreads_Unauthenticated_ReturnsForbidden() throws Exception {
        // No llamamos a authenticateUser()

        mockMvc.perform(get("/api/saved-threads")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // O isUnauthorized dependiendo de tu config
    }

    // Helper para autenticación manual
    private void authenticateUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}