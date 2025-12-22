package com.focalizze.Focalizze.controllerTest;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.NotificationController;
import com.focalizze.Focalizze.dto.NotificationDto;
import com.focalizze.Focalizze.models.User;

import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.services.NotificationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private NotificationService notificationService;

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

        currentUser = User.builder()
                .id(1L)
                .username("user")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("getMyNotifications: Should return paginated notifications")
    void getMyNotifications_Success() throws Exception {
        // Given
        authenticateUser();

        NotificationDto dto = new NotificationDto(
                1L,
                "NEW_LIKE",
                "Msg",
                false,
                null,
                null,
                "1h",
                null
        );

        Page<NotificationDto> page = new PageImpl<>(List.of(dto));

        given(notificationService.getNotificationsForUser(any(User.class), any(Pageable.class)))
                .willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Ahora esto pasará porque "message" ya no es null
                .andExpect(jsonPath("$.content[0].message").value("Msg"));
    }

    @Test
    @DisplayName("hasUnreadNotifications: Should return boolean status")
    void hasUnreadNotifications_Success() throws Exception {
        // Given
        authenticateUser();
        given(notificationService.hasUnreadNotifications(any(User.class))).willReturn(true);

        // When & Then
        mockMvc.perform(get("/api/notifications/unread")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasUnread").value(true));
    }

    @Test
    @DisplayName("markAllAsRead: Should call service and return 200 OK")
    void markAllAsRead_Success() throws Exception {
        // Given
        authenticateUser();

        // When & Then
        mockMvc.perform(post("/api/notifications/mark-as-read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(notificationService).markAllAsRead(any(User.class));
    }

    // Helper para evitar ClassCastException
    private void authenticateUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
