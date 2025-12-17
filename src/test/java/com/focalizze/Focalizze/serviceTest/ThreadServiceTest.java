package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.ThreadUpdateRequestDto;
import com.focalizze.Focalizze.dto.mappers.FeedMapper;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.exceptions.DailyLimitExceededException;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.Post;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.SavedThreadRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.FileStorageService;
import com.focalizze.Focalizze.services.MentionService;
import com.focalizze.Focalizze.services.servicesImpl.ThreadServiceImpl;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ThreadServiceTest {
    @Mock private ThreadRepository threadRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ThreadMapper threadMapper;
    @Mock private FeedMapper feedMapper;
    @Mock private SavedThreadRepository savedThreadRepository;
    @Mock private MentionService mentionService;
    @Mock private FileStorageService fileStorageService;
    @Mock private ThreadEnricher threadEnricher;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private ThreadServiceImpl threadService;

    private User currentUser;
    private ThreadClass thread;
    private CategoryClass category;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        currentUser = User.builder().id(1L).username("creator").build();
        category = new CategoryClass(); category.setName("Tech");

        thread = new ThreadClass();
        thread.setId(100L);
        thread.setUser(currentUser);
        thread.setCategory(category);
        thread.setPosts(new ArrayList<>(List.of(
                Post.builder().content("Old content 1").position(1).build(),
                Post.builder().content("Old content 2").position(2).build(),
                Post.builder().content("Old content 3").position(3).build()
        )));
        thread.setViewCount(10);
    }

    // --- getThreadsAvailableToday ---

    @Test
    @DisplayName("getThreadsAvailableToday: Should return correct remaining count")
    void getThreadsAvailable_ShouldCalculate() {
        // Given: User created 1 thread today. Limit is 3.
        given(threadRepository.countActiveThreadsSince(eq(currentUser), any(LocalDateTime.class)))
                .willReturn(1L);

        // When
        int remaining = threadService.getThreadsAvailableToday(currentUser);

        // Then
        assertThat(remaining).isEqualTo(2); // 3 - 1 = 2
    }

    // --- createThread ---

    @Test
    @DisplayName("createThread: Should create thread if limit not exceeded")
    void createThread_Success() {
        // Given
        setupAuth();
        ThreadRequestDto request = new ThreadRequestDto("Post 1", "Post 2", "Post 3", "Tech", null);

        // Mock Limits
        given(threadRepository.countActiveThreadsSince(any(), any())).willReturn(0L); // 3 remaining

        // Mock Category
        given(categoryRepository.findByName("Tech")).willReturn(Optional.of(category));

        // Mock Mapper
        given(threadMapper.mapToResponseDto(any(ThreadClass.class))).willReturn(new ThreadResponseDto(1L, null, null, null,null,null,null));

        // Mock Save
        given(threadRepository.save(any(ThreadClass.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        threadService.createThread(request, null);

        // Then
        ArgumentCaptor<ThreadClass> captor = ArgumentCaptor.forClass(ThreadClass.class);
        verify(threadRepository).save(captor.capture());

        ThreadClass saved = captor.getValue();
        assertThat(saved.getPosts()).hasSize(3);
        assertThat(saved.getCategory().getName()).isEqualTo("Tech");
        assertThat(saved.getUser().getUsername()).isEqualTo("creator");
    }

    @Test
    @DisplayName("createThread: Should throw exception if limit exceeded")
    void createThread_LimitExceeded_ThrowsException() {
        // Given
        setupAuth();
        given(threadRepository.countActiveThreadsSince(any(), any())).willReturn(3L); // Limit reached

        ThreadRequestDto request = new ThreadRequestDto("P1", "P2", "P3", "Cat", null);

        // When & Then
        assertThrows(DailyLimitExceededException.class, () ->
                threadService.createThread(request, null)
        );
    }

    @Test
    @DisplayName("createThread: Should process images if present")
    void createThread_WithImages_ShouldProcess() {
        // Mock Servlet Context for URL generation
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Given
        setupAuth();
        given(threadRepository.countActiveThreadsSince(any(), any())).willReturn(0L);
        given(fileStorageService.storeFile(any(), anyString())).willReturn("image.jpg");
        given(threadRepository.save(any())).willAnswer(i -> i.getArgument(0));

        MockMultipartFile image = new MockMultipartFile("file", "test.jpg", "image/jpeg", "bytes".getBytes());
        ThreadRequestDto dto = new ThreadRequestDto("P1", "P2", "P3", null, null);

        // When
        threadService.createThread(dto, List.of(image));

        // Then
        verify(fileStorageService).storeFile(any(), eq("creator"));
    }

    // --- getThreadByIdAndIncrementView ---

    @Test
    @DisplayName("getThreadById: Should increment view count and enrich")
    void getThreadById_ShouldIncrementViews() {
        // Given
        given(threadRepository.findByIdWithDetails(100L)).willReturn(Optional.of(thread));
        given(threadRepository.save(thread)).willReturn(thread);

        // When
        threadService.getThreadByIdAndIncrementView(100L);

        // Then
        assertThat(thread.getViewCount()).isEqualTo(11); // 10 + 1
        verify(threadRepository).save(thread);
        verify(threadEnricher).enrich(eq(thread), any()); // Current user might be null or authenticated
    }

    @Test
    @DisplayName("getThreadById: Should throw exception if deleted")
    void getThreadById_Deleted_ThrowsException() {
        thread.setDeleted(true);
        given(threadRepository.findByIdWithDetails(100L)).willReturn(Optional.of(thread));

        assertThrows(EntityNotFoundException.class, () ->
                threadService.getThreadByIdAndIncrementView(100L)
        );
    }

    // --- deleteThread ---

    @Test
    @DisplayName("deleteThread: Should perform soft delete if owner")
    void deleteThread_Owner_Success() {
        // Given
        given(threadRepository.findById(100L)).willReturn(Optional.of(thread));

        // When
        threadService.deleteThread(100L, currentUser);

        // Then
        assertThat(thread.isDeleted()).isTrue();
        verify(threadRepository).save(thread);
    }

    @Test
    @DisplayName("deleteThread: Should throw exception if not owner")
    void deleteThread_NotOwner_ThrowsAccessDenied() {
        // Given
        User otherUser = User.builder().id(99L).build();
        given(threadRepository.findById(100L)).willReturn(Optional.of(thread));

        // When & Then
        assertThrows(AccessDeniedException.class, () ->
                threadService.deleteThread(100L, otherUser)
        );
    }

    // --- updateThread ---

    @Test
    @DisplayName("updateThread: Should update posts and category")
    void updateThread_Success() {
        // Given
        given(threadRepository.findByIdWithDetails(100L)).willReturn(Optional.of(thread));
        given(threadRepository.save(any())).willReturn(thread);

        CategoryClass newCat = new CategoryClass(); newCat.setName("Art");
        given(categoryRepository.findByName("Art")).willReturn(Optional.of(newCat));

        ThreadUpdateRequestDto update = new ThreadUpdateRequestDto("New 1", null, "New 3", "Art");

        // When
        threadService.updateThread(100L, update, currentUser);

        // Then
        assertThat(thread.getPosts().get(0).getContent()).isEqualTo("New 1");
        assertThat(thread.getPosts().get(1).getContent()).isEqualTo("Old content 2"); // No change
        assertThat(thread.getPosts().get(2).getContent()).isEqualTo("New 3");
        assertThat(thread.getCategory().getName()).isEqualTo("Art");
    }

    // Helper
    private void setupAuth() {
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("creator");
        given(userRepository.findByUsername("creator")).willReturn(Optional.of(currentUser));
    }
}
