package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.UserController;
import com.focalizze.Focalizze.dto.*;

import com.focalizze.Focalizze.dto.mappers.UserMapper;
import com.focalizze.Focalizze.models.User;

import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.services.BlockService;
import com.focalizze.Focalizze.services.FollowService;
import com.focalizze.Focalizze.services.UserService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Dependencias del controlador
    @MockitoBean private FollowService followService;
    @MockitoBean private UserService userService;
    @MockitoBean private BlockService blockService;
    @MockitoBean private UserMapper userMapper;

    // Dependencias de Seguridad
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private JwtRequestFilter jwtRequestFilter;

    private User currentUser;
    private UserSummaryDto userSummaryDto;
    private UserDto userDto;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Bypass del filtro JWT
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtRequestFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        // 2. Datos de prueba
        currentUser = User.builder()
                .id(1L)
                .username("currentUser")
                .role(UserRole.USER)
                .build();

        userSummaryDto = new UserSummaryDto(2L, "target", "Target", "url", false);

        userDto = new UserDto(1L, "currentUser", "Display", "url", 5, false, 10, 20, false, "USER", false, null, null);
    }

    // --- FOLLOWERS & FOLLOWING ---

    @Test
    @DisplayName("getUserFollowers: Should return list of followers (200 OK)")
    void getUserFollowers_Success() throws Exception {
        // Given
        given(followService.getFollowers(eq("target"), any())).willReturn(List.of(userSummaryDto));

        authenticateUser(); // Opcional, pero prueba que funciona con usuario logueado

        // When & Then
        mockMvc.perform(get("/api/users/target/followers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("target"));
    }

    @Test
    @DisplayName("getUserFollowing: Should return list of following (200 OK)")
    void getUserFollowing_Success() throws Exception {
        // Given
        given(followService.getFollowing(eq("target"), any())).willReturn(List.of(userSummaryDto));

        authenticateUser();

        // When & Then
        mockMvc.perform(get("/api/users/target/following")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("target"));
    }

    @Test
    @DisplayName("toggleFollow: Should return 200 OK")
    void toggleFollow_Success() throws Exception {
        // Given
        authenticateUser(); // Requerido por @PreAuthorize
        doNothing().when(followService).toggleFollowUser(eq("target"), any(User.class));

        // When & Then
        mockMvc.perform(post("/api/users/target/follow")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // --- PROFILES ---

    @Test
    @DisplayName("getUserProfile: Should return UserDto (200 OK)")
    void getUserProfile_Success() throws Exception {
        // Given
        UserDto profileDto = new UserDto(2L, "target", "Target", "url", 0, true, 0, 0, false, "USER", false, null, null);
        given(userService.getUserProfile(eq("target"), any())).willReturn(profileDto);

        authenticateUser();

        // When & Then
        mockMvc.perform(get("/api/users/target")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("target"))
                .andExpect(jsonPath("$.isFollowing").value(true));
    }

    @Test
    @DisplayName("getMyProfile: Should return own UserDto (200 OK)")
    void getMyProfile_Success() throws Exception {
        // Given
        authenticateUser(); // Requerido por @PreAuthorize
        given(userMapper.toDto(any(User.class))).willReturn(userDto);

        // When & Then
        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("currentUser"));
    }

    // --- BLOCKS ---

    @Test
    @DisplayName("toggleBlock: Should return block status map (200 OK)")
    void toggleBlock_Success() throws Exception {
        // Given
        authenticateUser();
        given(blockService.toggleBlock("target")).willReturn(true);

        // When & Then
        mockMvc.perform(post("/api/users/target/block")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true));
    }

    @Test
    @DisplayName("getBlockedUsersList: Should return list of blocked users (200 OK)")
    void getBlockedUsersList_Success() throws Exception {
        // Given
        authenticateUser();
        BlockedUserDto blockedDto = new BlockedUserDto(3L, "blocked", "Blocked", "url");
        given(blockService.getBlockedUsers()).willReturn(List.of(blockedDto));

        // When & Then
        mockMvc.perform(get("/api/users/blocked")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("blocked"));
    }

    // --- THEME ---

    @Test
    @DisplayName("updateTheme: Should call service and return 200 OK")
    void updateTheme_Success() throws Exception {
        // Given
        authenticateUser();
        UpdateThemeDto themeDto = new UpdateThemeDto("DARK", "#000");
        doNothing().when(userService).updateThemePreferences(eq("currentUser"), any(UpdateThemeDto.class));

        // When & Then
        mockMvc.perform(patch("/api/users/me/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(themeDto)))
                .andExpect(status().isOk());
    }

    // --- Helper para autenticación manual ---
    private void authenticateUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
