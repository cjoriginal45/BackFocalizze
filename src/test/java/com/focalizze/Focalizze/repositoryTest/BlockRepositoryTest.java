package com.focalizze.Focalizze.repositoryTest;

import com.focalizze.Focalizze.models.Block;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ActiveProfiles("test")
@DataJpaTest
public class BlockRepositoryTest {

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private UserRepository userRepository;

    private User userBlocker;
    private User userBlocked1;
    private User userBlocked2;
    private User userOther;


    // Este método se ejecuta antes de cada test
    // This method is executed before each test
    @BeforeEach
    void setUp() {
        // Given: Creamos usuarios en la BD para poder establecer relaciones de bloqueo
        // Given: We create users in the DB to establish block relationships

        // 1. El usuario que bloquea
        userBlocker = User.builder()
                .username("blockerUser")
                .email("blocker@email.com")
                .password("pass123")
                .displayName("Blocker")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(userBlocker);

        // 2. Un usuario que será bloqueado
        userBlocked1 = User.builder()
                .username("blockedUser1")
                .email("blocked1@email.com")
                .password("pass123")
                .displayName("Blocked One")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(userBlocked1);

        // 3. Otro usuario que será bloqueado
        userBlocked2 = User.builder()
                .username("blockedUser2")
                .email("blocked2@email.com")
                .password("pass123")
                .displayName("Blocked Two")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(userBlocked2);

        // 4. Un usuario ajeno a los bloqueos
        userOther = User.builder()
                .username("otherUser")
                .email("other@email.com")
                .password("pass123")
                .displayName("Other")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(userOther);
    }

    // Este método se ejecuta después de cada test para limpiar la BD
    // This method is executed after each test to clean the DB
    @AfterEach
    void tearDown() {
        // Borramos los bloques primero por la restricción de clave foránea
        blockRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Debería encontrar un bloqueo específico entre dos usuarios")
    void findByBlockerAndBlocked_WhenBlockExists_ShouldReturnBlock() {
        // Given: Creamos un bloqueo
        Block block = Block.builder()
                .blocker(userBlocker)
                .blocked(userBlocked1)
                .createdAt(LocalDateTime.now())
                .build();
        blockRepository.save(block);

        // When: Buscamos el bloqueo
        Optional<Block> foundBlock = blockRepository.findByBlockerAndBlocked(userBlocker, userBlocked1);

        // Then: Verificamos que existe y es correcto
        assertThat(foundBlock).isPresent();
        assertThat(foundBlock.get().getBlocker().getUsername()).isEqualTo(userBlocker.getUsername());
        assertThat(foundBlock.get().getBlocked().getUsername()).isEqualTo(userBlocked1.getUsername());
    }

    @Test
    @DisplayName("Debería devolver TRUE si existe un bloqueo entre dos usuarios")
    void existsByBlockerAndBlocked_WhenExists_ShouldReturnTrue() {
        // Given
        Block block = Block.builder()
                .blocker(userBlocker)
                .blocked(userBlocked1)
                .createdAt(LocalDateTime.now())
                .build();
        blockRepository.save(block);

        // When
        boolean exists = blockRepository.existsByBlockerAndBlocked(userBlocker, userBlocked1);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Debería devolver FALSE si NO existe bloqueo entre dos usuarios")
    void existsByBlockerAndBlocked_WhenNotExists_ShouldReturnFalse() {
        // When: Buscamos un bloqueo que no hemos creado
        boolean exists = blockRepository.existsByBlockerAndBlocked(userBlocker, userOther);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Debería devolver los IDs de los usuarios bloqueados por un usuario específico")
    void findBlockedUserIdsByBlocker_ShouldReturnBlockedIds() {
        // Given: userBlocker bloquea a userBlocked1 y userBlocked2
        Block b1 = Block.builder().blocker(userBlocker).blocked(userBlocked1).createdAt(LocalDateTime.now()).build();
        Block b2 = Block.builder().blocker(userBlocker).blocked(userBlocked2).createdAt(LocalDateTime.now()).build();
        blockRepository.saveAll(List.of(b1, b2));

        // When
        Set<Long> blockedIds = blockRepository.findBlockedUserIdsByBlocker(userBlocker.getId());

        // Then
        assertThat(blockedIds).hasSize(2);
        assertThat(blockedIds).contains(userBlocked1.getId(), userBlocked2.getId());
        assertThat(blockedIds).doesNotContain(userOther.getId());
    }

    @Test
    @DisplayName("Debería devolver los IDs de quiénes han bloqueado a un usuario")
    void findUserIdsWhoBlockedUser_ShouldReturnBlockerIds() {
        // Given: userBlocker bloquea a userBlocked1, y userOther TAMBIÉN bloquea a userBlocked1
        Block b1 = Block.builder().blocker(userBlocker).blocked(userBlocked1).createdAt(LocalDateTime.now()).build();
        Block b2 = Block.builder().blocker(userOther).blocked(userBlocked1).createdAt(LocalDateTime.now()).build();
        blockRepository.saveAll(List.of(b1, b2));

        // When: Buscamos quién bloqueó a userBlocked1
        Set<Long> blockerIds = blockRepository.findUserIdsWhoBlockedUser(userBlocked1.getId());

        // Then
        assertThat(blockerIds).hasSize(2);
        assertThat(blockerIds).contains(userBlocker.getId(), userOther.getId());
    }

    @Test
    @DisplayName("Debería filtrar una lista de autores y devolver solo los que están bloqueados")
    void findBlockedIdsByBlockerAndBlockedIdsIn_ShouldFilterCorrectly() {
        // Given: userBlocker bloquea a userBlocked1. NO bloquea a userBlocked2 ni a userOther.
        Block b1 = Block.builder().blocker(userBlocker).blocked(userBlocked1).createdAt(LocalDateTime.now()).build();
        blockRepository.save(b1);

        // Creamos una lista "candidata" de IDs que queremos verificar
        Set<Long> candidateIds = Set.of(userBlocked1.getId(), userBlocked2.getId(), userOther.getId());

        // When
        Set<Long> resultIds = blockRepository.findBlockedIdsByBlockerAndBlockedIdsIn(userBlocker, candidateIds);

        // Then: Solo debería devolver el ID de userBlocked1
        assertThat(resultIds).hasSize(1);
        assertThat(resultIds).contains(userBlocked1.getId());
        assertThat(resultIds).doesNotContain(userBlocked2.getId(), userOther.getId());
    }

    @Test
    @DisplayName("Debería devolver la lista de entidades User bloqueadas ordenadas por fecha")
    void findBlockedUsersByBlocker_ShouldReturnUsersOrdered() {
        // Given
        // Bloqueo 1 (Antiguo)
        Block b1 = Block.builder().blocker(userBlocker).blocked(userBlocked1).createdAt(LocalDateTime.now().minusDays(5)).build();
        // Bloqueo 2 (Reciente)
        Block b2 = Block.builder().blocker(userBlocker).blocked(userBlocked2).createdAt(LocalDateTime.now()).build();

        blockRepository.save(b1);
        blockRepository.save(b2);

        // When
        List<User> blockedUsers = blockRepository.findBlockedUsersByBlocker(userBlocker);

        // Then
        assertThat(blockedUsers).hasSize(2);
        // Esperamos orden DESC por createdAt (el más reciente primero)
        assertThat(blockedUsers.get(0).getUsername()).isEqualTo(userBlocked2.getUsername());
        assertThat(blockedUsers.get(1).getUsername()).isEqualTo(userBlocked1.getUsername());
    }
}
