package com.focalizze.Focalizze.controllerTest;

import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.SearchController;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.UserSearchDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.services.SearchService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import(SecurityConfig.class)
class SearchControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SearchService searchService;

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

        currentUser = User.builder().id(1L).username("user").role(UserRole.USER).build();
    }

    @Test
    @DisplayName("searchUsers: Should return list of matching users (200 OK)")
    void searchUsers_Success() throws Exception {
        // Given
        UserSearchDto dto = new UserSearchDto("target", "Target User", "url");
        given(searchService.searchUsersByPrefix("tar")).willReturn(List.of(dto));

        // When & Then
        mockMvc.perform(get("/api/search/users")
                        .param("q", "tar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("target"));
    }

    @Test
    @DisplayName("searchContent: Should return list of matching threads (200 OK)")
    void searchContent_Success() throws Exception {
        // Given
        // Simulamos que el usuario está logueado para que la lógica de filtrado de bloqueos en el servicio funcione
        authenticateUser();

        // Mock simple de respuesta (no necesitamos llenar todos los campos para probar el controller)
        ThreadResponseDto threadDto = new ThreadResponseDto(1L, null, null, null, null, null, null);
        given(searchService.searchContent("query")).willReturn(List.of(threadDto));

        // When & Then
        mockMvc.perform(get("/api/search/content")
                        .param("q", "query")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    // Helper para autenticación manual
    private void authenticateUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}