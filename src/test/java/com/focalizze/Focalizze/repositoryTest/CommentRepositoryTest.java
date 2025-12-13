package com.focalizze.Focalizze.repositoryTest;

import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.CommentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User threadAuthor;
    private User commenter;
    private User blockedUser;
    private ThreadClass thread;

    @BeforeEach
    void setUp() {
        // 1. Crear Usuarios
        threadAuthor = createUser("author", "author@test.com");
        commenter = createUser("commenter", "commenter@test.com");
        blockedUser = createUser("blocked", "blocked@test.com");

        // 2. Crear Categoría (necesaria para el hilo)
        CategoryClass category = new CategoryClass();
        category.setName("General");
        category.setDescription("Desc");
        entityManager.persist(category);

        // 3. Crear Hilo
        thread = new ThreadClass();
        thread.setUser(threadAuthor);
        thread.setCategory(category);
        thread.setPublishedAt(LocalDateTime.now());
        thread.setPublished(true);
        thread.setDeleted(false);
        entityManager.persist(thread);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @DisplayName("findActiveCommentsByThread: Debería devolver solo comentarios no eliminados")
    void findActiveCommentsByThread_ShouldReturnOnlyActive() {
        // Given
        createComment(thread, commenter, "Active comment", false);
        createComment(thread, commenter, "Deleted comment", true); // Eliminado
        entityManager.flush();

        // When
        Page<CommentClass> result = commentRepository.findActiveCommentsByThread(thread, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Active comment");
    }

    @Test
    @DisplayName("findByIdAndUser: Debería encontrar comentario si pertenece al usuario")
    void findByIdAndUser_WhenOwned_ShouldReturnComment() {
        // Given
        CommentClass comment = createComment(thread, commenter, "My comment", false);
        entityManager.flush();

        // When
        Optional<CommentClass> result = commentRepository.findByIdAndUser(comment.getId(), commenter);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUser()).isEqualTo(commenter);
    }

    @Test
    @DisplayName("findByIdAndUser: No debería encontrar comentario si pertenece a otro usuario")
    void findByIdAndUser_WhenNotOwned_ShouldReturnEmpty() {
        // Given
        CommentClass comment = createComment(thread, commenter, "My comment", false);
        entityManager.flush();

        // When: Buscamos con el autor del hilo (que no es el dueño del comentario)
        Optional<CommentClass> result = commentRepository.findByIdAndUser(comment.getId(), threadAuthor);

        // Then
        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("findActiveRootComments...: Debería filtrar bloqueados y cargar respuestas")
    void findRootComments_ShouldFilterBlockedAndFetchReplies() {
        // Given: Estructura de comentarios compleja

        // 1. Comentario Raíz Válido (de 'commenter')
        CommentClass rootValid = createComment(thread, commenter, "Root Valid", false);

        // 2. Respuesta al comentario válido (el autor del hilo responde)
        CommentClass replyToValid = createComment(thread, threadAuthor, "Reply to Valid", false);
        replyToValid.setParent(rootValid);
        rootValid.getReplies().add(replyToValid); // Mantener coherencia en memoria
        entityManager.persist(replyToValid);

        // 3. Comentario Raíz de Usuario Bloqueado
        createComment(thread, blockedUser, "Root Blocked", false);

        // 4. Comentario Raíz Eliminado (no debería salir)
        createComment(thread, commenter, "Root Deleted", true);

        entityManager.flush();
        entityManager.clear(); // Limpiamos para probar el FETCH real desde la BD

        // When: Buscamos filtrando al 'blockedUser'
        Set<Long> blockedIds = Set.of(blockedUser.getId());
        Page<CommentClass> result = commentRepository.findActiveRootCommentsByThreadAndFilterBlocked(
                thread, blockedIds, PageRequest.of(0, 10));

        // Then
        // Esperamos solo 1 comentario raíz (el de 'commenter').
        // El del 'blockedUser' se filtra. El 'deleted' se filtra. La respuesta no es raíz.
        assertThat(result.getContent()).hasSize(1);

        CommentClass loadedRoot = result.getContent().get(0);
        assertThat(loadedRoot.getContent()).isEqualTo("Root Valid");

        // Verificamos que las respuestas se cargaron (JOIN FETCH)
        assertThat(loadedRoot.getReplies()).hasSize(1);
        assertThat(loadedRoot.getReplies().get(0).getContent()).isEqualTo("Reply to Valid");
    }

    @Test
    @DisplayName("findActiveRootComments...: Debería funcionar si la lista de bloqueados está vacía (o tiene -1)")
    void findRootComments_NoBlockedUsers_ShouldReturnAllRoots() {
        // Given
        createComment(thread, commenter, "Comment 1", false);
        createComment(thread, blockedUser, "Comment 2", false); // Este ahora debería aparecer
        entityManager.flush();
        entityManager.clear();

        // When: Pasamos un set "vacío" o con ID ficticio
        Set<Long> blockedIds = Set.of(-1L);
        Page<CommentClass> result = commentRepository.findActiveRootCommentsByThreadAndFilterBlocked(
                thread, blockedIds, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(2);
    }

    // --- Helpers ---

    private User createUser(String username, String email) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password("pass")
                .displayName(username)
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(user);
        return user;
    }

    private CommentClass createComment(ThreadClass thread, User author, String content, boolean isDeleted) {
        CommentClass comment = CommentClass.builder()
                .thread(thread)
                .user(author)
                .content(content)
                .createdAt(LocalDateTime.now())
                .isDeleted(isDeleted)
                .build();
        entityManager.persist(comment);
        return comment;
    }
}
