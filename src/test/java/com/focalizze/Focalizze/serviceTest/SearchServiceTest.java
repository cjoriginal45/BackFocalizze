package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.UserSearchDto;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ThreadRepository threadRepository;
    @Mock private ThreadMapper threadMapper;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private SearchServiceImpl searchService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    // --- searchUsersByPrefix ---

    @Test
    @DisplayName("searchUsersByPrefix: Should clean prefix and return mapped DTOs")
    void searchUsers_Valid_ShouldReturnUsers() {
        // Given
        User u1 = User.builder().username("john").displayName("John Doe").avatarUrl("url").build();
        given(userRepository.findTop5ByUsernameStartingWithIgnoreCase("jo")).willReturn(List.of(u1));

        // When: Pasamos "@jo" para verificar que limpia el '@'
        List<UserSearchDto> result = searchService.searchUsersByPrefix("@jo");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("john");
        verify(userRepository).findTop5ByUsernameStartingWithIgnoreCase("jo");
    }

    @Test
    @DisplayName("searchUsersByPrefix: Should return empty if prefix is blank")
    void searchUsers_Blank_ShouldReturnEmpty() {
        assertThat(searchService.searchUsersByPrefix("")).isEmpty();
        assertThat(searchService.searchUsersByPrefix("  ")).isEmpty();
        assertThat(searchService.searchUsersByPrefix(null)).isEmpty();
        verify(userRepository, never()).findTop5ByUsernameStartingWithIgnoreCase(any());
    }

    // --- searchContent ---

    @Test
    @DisplayName("searchContent: Should search by CATEGORY if match found")
    void searchContent_CategoryMatch_ShouldSearchByCategory() {
        // Given
        String query = "Tech";
        CategoryClass cat = new CategoryClass();
        cat.setName("Tech");

        // --- CORRECCIÓN CLAVE ---
        // Simulamos un usuario anónimo o autenticado para evitar el NPE
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn("anonymousUser"); // No es instancia de User, así que no hay bloqueos
        // ------------------------

        given(categoryRepository.findByName("Tech")).willReturn(Optional.of(cat));
        given(threadRepository.findByCategory(cat)).willReturn(new ArrayList<>());

        // When
        searchService.searchContent(query);

        // Then
        verify(threadRepository).findByCategory(cat);
        verify(threadRepository, never()).findByPostContentContainingIgnoreCase(any());
    }


    @Test
    @DisplayName("searchContent: Should search by TEXT CONTENT if no category match")
    void searchContent_NoCategory_ShouldSearchByText() {
        // Given
        String query = "Hello World";

        // --- CORRECCIÓN CLAVE ---
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn("anonymousUser");
        // ------------------------

        given(categoryRepository.findByName(query)).willReturn(Optional.empty());
        given(threadRepository.findByPostContentContainingIgnoreCase(query)).willReturn(new ArrayList<>());

        // When
        searchService.searchContent(query);

        // Then
        verify(threadRepository).findByPostContentContainingIgnoreCase(query);
    }

    @Test
    @DisplayName("searchContent: Should FILTER OUT threads from blocked users")
    void searchContent_WithBlocks_ShouldFilter() {
        // Given
        User currentUser = User.builder().id(1L).build();
        User blockedUser = User.builder().id(99L).build();
        User cleanUser = User.builder().id(50L).build();

        // 1. Simular Auth
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(currentUser);

        // 2. Simular Bloqueos
        given(userRepository.findBlockedUserIdsByBlocker(1L)).willReturn(Set.of(99L)); // Bloqueado por mí
        given(userRepository.findUserIdsWhoBlockedUser(1L)).willReturn(Collections.emptySet());

        // 3. Simular Resultados de Búsqueda (1 hilo de bloqueado, 1 hilo limpio)
        ThreadClass t1 = new ThreadClass(); t1.setUser(blockedUser);
        ThreadClass t2 = new ThreadClass(); t2.setUser(cleanUser);

        // Asumimos búsqueda por texto
        given(categoryRepository.findByName(any())).willReturn(Optional.empty());
        given(threadRepository.findByPostContentContainingIgnoreCase(any())).willReturn(List.of(t1, t2));

        // 4. Mock Mapper
        given(threadMapper.toDtoList(anyList())).willAnswer(invocation -> {
            List<ThreadClass> list = invocation.getArgument(0);
            return list.stream().map(t -> new ThreadResponseDto(null, null, null, null, null, null, null)).toList();
        });

        // When
        List<ThreadResponseDto> result = searchService.searchContent("query");

        // Then
        // La lista original tenía 2 hilos. El resultado debe tener 1.
        assertThat(result).hasSize(1);

        // Verificamos que el filtro se aplicó correctamente antes de mapear
        // t1 (blockedUser) debió ser eliminado.
        verify(threadMapper).toDtoList(argThat(list ->
                list.size() == 1 && list.get(0).getUser().getId().equals(50L)
        ));
    }
}
