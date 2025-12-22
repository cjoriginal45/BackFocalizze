package com.focalizze.Focalizze.controllerTest;


import com.focalizze.Focalizze.configurations.SecurityConfig;
import com.focalizze.Focalizze.controllers.CategoryController;
import com.focalizze.Focalizze.dto.CategoryDetailsDto;
import com.focalizze.Focalizze.dto.CategoryDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.CategoryFollowService;
import com.focalizze.Focalizze.services.CategoryService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
public class CategoryControllerTest {
    @Autowired private MockMvc mockMvc;

    @MockitoBean private CategoryService categoryService;
    @MockitoBean private CategoryFollowService categoryFollowService;

    // Security Mocks
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private JwtRequestFilter jwtRequestFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtRequestFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @Test
    @DisplayName("getAllCategories: Should return list of categories (Public)")
    void getAllCategories_Success() throws Exception {
        List<CategoryDto> categories = List.of(new CategoryDto(1L, "Tech", "Desc", 100, false));
        given(categoryService.getAllCategories()).willReturn(categories);

        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tech"));
    }

    @Test
    @DisplayName("toggleFollow: Should return 200 OK (Authenticated)")
    void toggleFollow_Success() throws Exception {
        doNothing().when(categoryFollowService).toggleFollowCategory(eq(1L), any(User.class));

        mockMvc.perform(post("/api/categories/1/follow")
                        .with(user("user")) // Simula usuario autenticado
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("toggleFollow: Should return 403/401 if not authenticated")
    void toggleFollow_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/categories/1/follow")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getCategoryDetails: Should return details DTO")
    void getCategoryDetails_Success() throws Exception {
        CategoryDetailsDto details = new CategoryDetailsDto(1L, "Tech", "Desc", "url", 10, 5L, true);
        given(categoryService.getCategoryDetails("Tech")).willReturn(details);

        mockMvc.perform(get("/api/categories/Tech")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tech"))
                .andExpect(jsonPath("$.isFollowing").value(true));
    }

    @Test
    @DisplayName("getThreadsByCategory: Should return paginated threads")
    void getThreadsByCategory_Success() throws Exception {
        Page<FeedThreadDto> page = new PageImpl<>(Collections.emptyList());
        given(categoryService.getThreadsByCategory(eq("Tech"), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/categories/Tech/threads")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
