package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.ProfileController;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ProfileResponseDto;
import com.focalizze.Focalizze.dto.ProfileUpdateRequestDto;
import com.focalizze.Focalizze.dto.UserProfileDownloadDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.services.FileStorageService;
import com.focalizze.Focalizze.services.ProfileService;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ProfileService profileService;
    @MockitoBean private FileStorageService fileStorageService;

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
    @DisplayName("getProfile: Should return profile DTO (Public)")
    void getProfile_Success() throws Exception {
        ProfileResponseDto dto = new ProfileResponseDto(1L, "target", "Target", "url", "bio", 10, 5, 0, null, null, false, 0, 0, false);
        given(profileService.getProfile("target")).willReturn(dto);

        mockMvc.perform(get("/api/profiles/target")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("target"));
    }

    @Test
    @DisplayName("getProfileThreads: Should return page of threads (Public)")
    void getProfileThreads_Success() throws Exception {
        Page<FeedThreadDto> page = new PageImpl<>(Collections.emptyList());
        given(profileService.getThreadsForUser(eq("target"), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/profiles/target/threads")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("patchProfile: Should return 200 OK if user owns the profile")
    void patchProfile_Owner_Success() throws Exception {
        ProfileUpdateRequestDto update = new ProfileUpdateRequestDto("New Name", "New Bio");
        ProfileResponseDto response = new ProfileResponseDto(1L, "user", "New Name", "url", "New Bio", 0, 0, 0, null, null, false, 0, 0, false);

        given(profileService.updateProfile(eq("user"), any())).willReturn(response);

        // Simulamos autenticación como 'user' (dueño)
        authenticateUser(currentUser);

        mockMvc.perform(patch("/api/profiles/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"));
    }

    @Test
    @DisplayName("patchProfile: Should return 403 Forbidden if not owner")
    void patchProfile_NotOwner_ReturnsForbidden() throws Exception {
        ProfileUpdateRequestDto update = new ProfileUpdateRequestDto("Hack", "Hack");

        // Simulamos autenticación como 'user', pero intentamos editar 'other'
        authenticateUser(currentUser);

        mockMvc.perform(patch("/api/profiles/other")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("uploadAvatar: Should return 200 OK if owner")
    void uploadAvatar_Owner_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatar", "test.jpg", "image/jpeg", "bytes".getBytes());
        given(profileService.updateAvatar(eq("user"), any())).willReturn("new-url");

        authenticateUser(currentUser);

        mockMvc.perform(multipart("/api/profiles/user/avatar")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("new-url"));
    }

    @Test
    @DisplayName("serveAvatar: Should return image resource")
    void serveAvatar_Success() throws Exception {
        Resource resource = new ByteArrayResource("image".getBytes());
        // Ajustamos el mock para devolver un recurso con nombre, ya que el controller usa getFilename()
        // Una forma fácil es usar un recurso de archivo real o un wrapper, pero ByteArrayResource no tiene filename por defecto.
        // Aquí simplificamos asumiendo que el controller maneja null filename o usando un mock de Resource.

        Resource mockResource = mock(Resource.class);
        given(mockResource.getFilename()).willReturn("avatar.png");
        given(fileStorageService.loadFileAsResource("avatar.png")).willReturn(mockResource);

        mockMvc.perform(get("/api/profiles/avatars/avatar.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"avatar.png\""));
    }

    @Test
    @DisplayName("getProfileForDownload: Should return DTO if owner")
    void getProfileForDownload_Owner_Success() throws Exception {
        UserProfileDownloadDto dto = new UserProfileDownloadDto("user", "url", "bio");
        given(profileService.getProfileForDownload("user")).willReturn(dto);

        authenticateUser(currentUser);

        mockMvc.perform(get("/api/profiles/user/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"));
    }

    // Helper para autenticar
    private void authenticateUser(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}