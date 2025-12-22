package com.focalizze.Focalizze.serviceTest;
import com.focalizze.Focalizze.models.HiddenContent;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.HiddenContentRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.servicesImpl.FeedbackServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FeedbackServiceTest {

    @Mock
    private HiddenContentRepository hiddenRepo;
    @Mock
    private ThreadRepository threadRepo;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    private User currentUser;
    private ThreadClass thread;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("user").build();
        thread = new ThreadClass();
        thread.setId(100L);
    }

    @Test
    @DisplayName("hideThread: Debería guardar el contenido oculto si el hilo existe")
    void hideThread_WhenExists_ShouldSave() {
        // Given
        given(threadRepo.findById(100L)).willReturn(Optional.of(thread));

        // When
        feedbackService.hideThread(100L, "NOT_INTERESTED", currentUser);

        // Then
        // Usamos ArgumentCaptor para verificar qué se pasó al método save
        ArgumentCaptor<HiddenContent> captor = ArgumentCaptor.forClass(HiddenContent.class);
        verify(hiddenRepo).save(captor.capture());

        HiddenContent savedContent = captor.getValue();
        assertThat(savedContent.getUser()).isEqualTo(currentUser);
        assertThat(savedContent.getThread()).isEqualTo(thread);
        assertThat(savedContent.getReasonType()).isEqualTo("NOT_INTERESTED");
    }

    @Test
    @DisplayName("hideThread: Debería lanzar excepción si el hilo no existe")
    void hideThread_NotFound_ThrowsException() {
        // Given
        given(threadRepo.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () ->
                feedbackService.hideThread(999L, "SPAM", currentUser)
        );
    }

    @Test
    @DisplayName("getHiddenThreadIds: Debería devolver el set de IDs del repositorio")
    void getHiddenThreadIds_ShouldReturnSet() {
        // Given
        Set<Long> hiddenIds = Set.of(100L, 200L);
        given(hiddenRepo.findHiddenThreadIdsByUser(currentUser)).willReturn(hiddenIds);

        // When
        Set<Long> result = feedbackService.getHiddenThreadIds(currentUser);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(100L, 200L);
    }
}
