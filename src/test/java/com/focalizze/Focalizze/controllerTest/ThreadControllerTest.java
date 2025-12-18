package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.ThreadController;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.ThreadUpdateRequestDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.services.LikeService;
import com.focalizze.Focalizze.services.SaveService;
import com.focalizze.Focalizze.services.ThreadService;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ThreadController.class)
@Import(SecurityConfig.class)
class ThreadControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Servicios del controlador
    @MockitoBean private ThreadService threadService;
    @MockitoBean private LikeService likeService;
    @MockitoBean private SaveService saveService;

    // Security Mocks
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private JwtRequestFilter jwtRequestFilter;

    private User currentUser;
    private ThreadResponseDto threadResponse;
    private FeedThreadDto feedThreadDto;

    @BeforeEach
    void setUp() throws Exception {
        // Bypass del filtro JWT
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

        threadResponse = new ThreadResponseDto(1L, null, null,null,LocalDateTime.now(), null,null);
        // Constructor simplificado para el test
        feedThreadDto = new FeedThreadDto(1L, null, null, null, null, false, false, "Cat", null);
    }

    @Test
    @DisplayName("createThread: Should return 201 Created (Multipart)")
    void createThread_Success() throws Exception {
        // Given
        authenticateUser();
        ThreadRequestDto requestDto = new ThreadRequestDto("Post 1", "Post 2", "Post 3", "Cat", null);

        given(threadService.createThread(any(ThreadRequestDto.class), any())).willReturn(threadResponse);

        // Preparar Multipart: JSON como archivo y archivo de imagen opcional
        MockMultipartFile jsonPart = new MockMultipartFile(
                "threadRequest",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(requestDto)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
                "images",
                "test.jpg",
                "image/jpeg",
                "bytes".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/thread/create")
                        .file(jsonPart)
                        .file(imagePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)) // Spring boot detecta el multipart
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("toggleLike: Should return 200 OK")
    void toggleLike_Success() throws Exception {
        authenticateUser();

        mockMvc.perform(post("/api/thread/1/like")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(likeService).toggleLike(eq(1L), any(User.class));
    }

    @Test
    @DisplayName("getThreadById: Should return Thread Details (Public)")
    void getThreadById_Success() throws Exception {
        given(threadService.getThreadByIdAndIncrementView(1L)).willReturn(feedThreadDto);

        mockMvc.perform(get("/api/thread/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("toggleSave: Should return 200 OK")
    void toggleSave_Success() throws Exception {
        authenticateUser();

        mockMvc.perform(post("/api/thread/1/save")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(saveService).toggleSave(eq(1L), any(User.class));
    }

    @Test
    @DisplayName("deleteThread: Should return 204 No Content")
    void deleteThread_Success() throws Exception {
        authenticateUser();

        mockMvc.perform(delete("/api/thread/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(threadService).deleteThread(eq(1L), any(User.class));
    }

    @Test
    @DisplayName("updateThread: Should return 200 OK with updated DTO")
    void updateThread_Success() throws Exception {
        authenticateUser();
        ThreadUpdateRequestDto updateDto = new ThreadUpdateRequestDto("New 1", "New 2", "New 3", "Cat");
        given(threadService.updateThread(eq(1L), any(ThreadUpdateRequestDto.class), any(User.class)))
                .willReturn(threadResponse);

        mockMvc.perform(patch("/api/thread/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    // Helper para evitar ClassCastException
    private void authenticateUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}