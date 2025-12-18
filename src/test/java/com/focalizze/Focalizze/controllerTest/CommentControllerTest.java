package com.focalizze.Focalizze.controllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.CommentController;
import com.focalizze.Focalizze.dto.CommentRequestDto;
import com.focalizze.Focalizze.dto.CommentResponseDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.services.CommentService;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
public class CommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CommentService commentService;

    // Security Mocks
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private JwtRequestFilter jwtRequestFilter;

    private CommentResponseDto sampleResponse;
    private UserDto userDto;

    @BeforeEach
    void setUp() throws Exception {
        // Bypass JWT Filter
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtRequestFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        // Datos de prueba
        userDto = new UserDto(1L, "user", "User", "url", 0, false, 0, 0, false, "USER", false, null, null);
        sampleResponse = new CommentResponseDto(1L, "Content", LocalDateTime.now(), userDto,  List.of());
    }

    @Test
    @DisplayName("getComments: Should return paginated comments")
    void getComments_Success() throws Exception {
        Page<CommentResponseDto> page = new PageImpl<>(List.of(sampleResponse));
        given(commentService.getCommentsByThread(eq(10L), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/threads/10/comments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].content").value("Content"));
    }

    @Test
    @DisplayName("createComment: Should return 201 Created (Authenticated)")
    void createComment_Success() throws Exception {
        CommentRequestDto request = new CommentRequestDto("New Comment");

        given(commentService.createComment(eq(10L), any(CommentRequestDto.class), any(User.class)))
                .willReturn(sampleResponse);

        com.focalizze.Focalizze.models.User myUser = new com.focalizze.Focalizze.models.User();
        myUser.setUsername("user");
        myUser.setPassword("pass");
        myUser.setRole(UserRole.USER);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                myUser, null, myUser.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/api/threads/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Content"));
    }

    @Test
    @DisplayName("editComment: Should return 200 OK with updated content")
    void editComment_Success() throws Exception {
        CommentRequestDto request = new CommentRequestDto("Updated Content");
        CommentResponseDto updatedResponse = new CommentResponseDto(1L, "Updated Content", LocalDateTime.now(), userDto, List.of());

        given(commentService.editComment(eq(1L), any(CommentRequestDto.class), any(User.class)))
                .willReturn(updatedResponse);

        com.focalizze.Focalizze.models.User myUser = new com.focalizze.Focalizze.models.User();
        myUser.setUsername("user");
        myUser.setPassword("pass");
        myUser.setRole(UserRole.USER);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                myUser, null, myUser.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated Content"));
    }

    @Test
    @DisplayName("deleteComment: Should return 204 No Content")
    void deleteComment_Success() throws Exception {
        doNothing().when(commentService).deleteComment(eq(1L), any(User.class));

        mockMvc.perform(delete("/api/comments/1")
                        .with(user("user"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("replyToComment: Should return 201 Created")
    void replyToComment_Success() throws Exception {
        CommentRequestDto request = new CommentRequestDto("Reply");
        given(commentService.replyToComment(eq(1L), any(CommentRequestDto.class), any(User.class)))
                .willReturn(sampleResponse);

        mockMvc.perform(post("/api/comments/1/reply")
                        .with(user("user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
