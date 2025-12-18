package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.ReportRequestDto;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.ReportRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.ReportServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {
    @Mock private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private ThreadRepository threadRepository;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private ReportServiceImpl reportService;

    private User reporter;
    private User targetUser;
    private ReportRequestDto requestDto;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        reporter = User.builder().id(1L).username("reporter").build();
        targetUser = User.builder().id(2L).username("target").build();
        requestDto = new ReportRequestDto(ReportReason.SPAM, "Es spam");
    }

    // --- reportUser ---

    @Test
    @DisplayName("reportUser: Should create and save report")
    void reportUser_Success() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("reporter");
        given(userRepository.findByUsername("reporter")).willReturn(Optional.of(reporter));
        given(userRepository.findByUsername("target")).willReturn(Optional.of(targetUser));

        // When
        reportService.reportUser("target", requestDto);

        // Then
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());

        Report savedReport = captor.getValue();
        assertThat(savedReport.getUserReporter()).isEqualTo(reporter);
        assertThat(savedReport.getUserReported()).isEqualTo(targetUser);
        assertThat(savedReport.getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(savedReport.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("reportUser: Should throw exception if self-reporting")
    void reportUser_SelfReport_ThrowsException() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("reporter");
        given(userRepository.findByUsername("reporter")).willReturn(Optional.of(reporter));
        // El target es el mismo usuario
        given(userRepository.findByUsername("reporter")).willReturn(Optional.of(reporter));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                reportService.reportUser("reporter", requestDto)
        );
    }

    // --- reportThread ---

    @Test
    @DisplayName("reportThread: Should create report linked to thread")
    void reportThread_Success() {
        // Given
        ThreadClass thread = new ThreadClass();
        thread.setId(10L);
        thread.setUser(targetUser);

        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("reporter");
        given(userRepository.findByUsername("reporter")).willReturn(Optional.of(reporter));
        given(threadRepository.findById(10L)).willReturn(Optional.of(thread));

        // When
        reportService.reportThread(10L, requestDto);

        // Then
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());

        Report savedReport = captor.getValue();
        assertThat(savedReport.getUserReporter()).isEqualTo(reporter);
        assertThat(savedReport.getThread()).isEqualTo(thread);
        assertThat(savedReport.getUserReported()).isEqualTo(targetUser); // El dueño del hilo
    }

    @Test
    @DisplayName("reportThread: Should throw exception if reporting own thread")
    void reportThread_OwnThread_ThrowsException() {
        // Given
        ThreadClass myThread = new ThreadClass();
        myThread.setId(10L);
        myThread.setUser(reporter); // El dueño es el reporter

        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("reporter");
        given(userRepository.findByUsername("reporter")).willReturn(Optional.of(reporter));
        given(threadRepository.findById(10L)).willReturn(Optional.of(myThread));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                reportService.reportThread(10L, requestDto)
        );
    }
}
