package com.focalizze.Focalizze.serviceTest;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.BackupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BackupServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ThreadRepository threadRepository;

    @InjectMocks
    private BackupServiceImpl backupService;

    private User user;
    private ThreadClass thread;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).username("testuser").email("test@email.com")
                .role(UserRole.USER).createdAt(LocalDateTime.now())
                .build();

        CategoryClass category = new CategoryClass();
        category.setName("Tech");

        thread = new ThreadClass();
        thread.setId(10L);
        thread.setUser(user);
        thread.setCategory(category);
        thread.setPublishedAt(LocalDateTime.now());
        thread.setLikeCount(5);
        thread.setViewCount(100);
        thread.setDeleted(false);
        thread.setPublished(true);
    }

    @Test
    @DisplayName("generateExcelBackup: Debería generar un stream no vacío cuando hay datos")
    void generateExcelBackup_WithData_ShouldReturnStream() throws IOException {
        // Given: Simulamos paginación.
        // Página 1 con datos, Página 2 vacía (para terminar el bucle do-while).

        // Usuarios
        Page<User> userPage1 = new PageImpl<>(List.of(user));
        Page<User> userPage2 = Page.empty(); // Fin del bucle

        given(userRepository.findAll(any(Pageable.class))).willReturn(userPage1);

        // Hilos
        Page<ThreadClass> threadPage1 = new PageImpl<>(List.of(thread));
        given(threadRepository.findAll(any(Pageable.class))).willReturn(threadPage1);

        // When
        ByteArrayInputStream result = backupService.generateExcelBackup();

        // Then
        assertThat(result).isNotNull();
        // Verificar que hay bytes (el header de Excel + datos ocupan espacio)
        assertThat(result.available()).isGreaterThan(0);

        // Verificar que se llamó a los repositorios
        verify(userRepository, times(1)).findAll(any(Pageable.class));
        verify(threadRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("generateExcelBackup: Debería generar un archivo válido (headers) incluso si la BD está vacía")
    void generateExcelBackup_NoData_ShouldReturnHeadersOnly() throws IOException {
        // Given: BD vacía
        given(userRepository.findAll(any(Pageable.class))).willReturn(Page.empty());
        given(threadRepository.findAll(any(Pageable.class))).willReturn(Page.empty());

        // When
        ByteArrayInputStream result = backupService.generateExcelBackup();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.available()).isGreaterThan(0); // Excel vacío pero con estructura válida
    }
}
