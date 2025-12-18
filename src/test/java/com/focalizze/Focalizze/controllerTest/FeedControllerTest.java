package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.FeedController;
import com.focalizze.Focalizze.dto.DiscoverItemDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.services.DiscoverFeedService;
import com.focalizze.Focalizze.services.FeedService;
import com.focalizze.Focalizze.services.FeedbackService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedController.class)
@Import(SecurityConfig.class)
public class FeedControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Servicios del controlador
    @MockitoBean private FeedService feedService;
    @MockitoBean private DiscoverFeedService discoverFeedService;
    @MockitoBean private FeedbackService feedbackService;

    // Mocks de Seguridad
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private JwtRequestFilter jwtRequestFilter;

    private User currentUser;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Bypass del filtro JWT
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtRequestFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        // 2. Crear usuario personalizado para evitar ClassCastException
        currentUser = User.builder()
                .id(1L)
                .username("user")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("getFeed: Should return page of threads (200 OK)")
    void getFeed_Success() throws Exception {
        // Given
        Page<FeedThreadDto> page = new PageImpl<>(List.of());
        given(feedService.getFeed(any(Pageable.class))).willReturn(page);

        // Simulamos autenticación (aunque este endpoint no tiene @AuthenticationPrincipal explícito,
        // el servicio subyacente lo usa, así que es mejor estar autenticado)
        authenticateUser();

        // When & Then
        mockMvc.perform(get("/api/feed")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getDiscoverFeed: Should return page of recommendations (200 OK)")
    void getDiscoverFeed_Success() throws Exception {
        // Given
        Page<DiscoverItemDto> page = new PageImpl<>(List.of());

        // Configuramos el mock para que acepte NUESTRO usuario personalizado
        given(discoverFeedService.getDiscoverFeed(any(User.class), any(Pageable.class)))
                .willReturn(page);

        // Simulamos autenticación manual para inyectar 'com.focalizze.models.User'
        authenticateUser();

        // When & Then
        mockMvc.perform(get("/api/feed/discover")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("hideThread: Should call service and return 200 OK")
    void hideThread_Success() throws Exception {
        // Given
        authenticateUser();

        // When & Then
        // Nota: Es un POST pero usa @RequestParam, no @RequestBody
        mockMvc.perform(post("/api/feed/feedback/hide")
                        .param("threadId", "100")
                        .param("reasonType", "NOT_INTERESTED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verificamos que el servicio se llamó con el usuario correcto
        verify(feedbackService).hideThread(eq(100L), eq("NOT_INTERESTED"), any(User.class));
    }

    // --- Helper para autenticación manual ---
    private void authenticateUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
