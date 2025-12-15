package com.focalizze.Focalizze.serviceTest;
import com.focalizze.Focalizze.dto.BlockedUserDto;
import com.focalizze.Focalizze.models.Block;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.BlockServiceImpl;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BlockServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private FollowRepository followRepository;

    // Mocks para simular el contexto de seguridad
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private BlockServiceImpl blockService;

    private User currentUser;
    private User targetUser;
    private String defaultAvatar = "default.png";

    @BeforeEach
    void setUp() {
        // Inyectamos el valor de la propiedad @Value manualmente
        ReflectionTestUtils.setField(blockService, "defaultAvatarUrl", defaultAvatar);

        // Configuramos el contexto de seguridad simulado
        SecurityContextHolder.setContext(securityContext);

        // Datos de prueba
        currentUser = User.builder()
                .id(1L)
                .username("currentUser")
                .followingCount(10)
                .followersCount(5)
                .role(UserRole.USER)
                .build();

        targetUser = User.builder()
                .id(2L)
                .username("targetUser")
                .displayName("Target")
                .avatarUrl("avatar.png")
                .followingCount(2)
                .followersCount(8)
                .role(UserRole.USER)
                .build();
    }

    // --- TEST: toggleBlock (BLOQUEAR) ---

    @Test
    @DisplayName("toggleBlock: Debería bloquear, eliminar follows mutuos y actualizar contadores si no estaba bloqueado")
    void toggleBlock_WhenNotBlocked_ShouldBlockAndRemoveFollows() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("currentUser");
        given(userRepository.findByUsername("currentUser")).willReturn(Optional.of(currentUser));
        given(userRepository.findByUsername("targetUser")).willReturn(Optional.of(targetUser));

        // No existe bloqueo previo
        given(blockRepository.findByBlockerAndBlocked(currentUser, targetUser)).willReturn(Optional.empty());

        // Simulamos que SÍ existía seguimiento mutuo (devuelve > 0 filas borradas)
        given(followRepository.deleteFollowRelation(currentUser, targetUser)).willReturn(1); // currentUser deja de seguir
        given(followRepository.deleteFollowRelation(targetUser, currentUser)).willReturn(1); // targetUser deja de seguir

        // When
        boolean isBlocked = blockService.toggleBlock("targetUser");

        // Then
        assertThat(isBlocked).isTrue(); // True significa que ahora está bloqueado

        // Verificaciones
        verify(blockRepository).save(any(Block.class)); // Se guardó el bloqueo

        // Verificamos que se actualizaron los contadores de ambos usuarios
        verify(userRepository).decrementFollowingCount(currentUser.getId()); // currentUser dejó de seguir
        verify(userRepository).decrementFollowersCount(targetUser.getId());  // target perdió un seguidor

        verify(userRepository).decrementFollowingCount(targetUser.getId()); // target dejó de seguir
        verify(userRepository).decrementFollowersCount(currentUser.getId()); // currentUser perdió un seguidor
    }

    // --- TEST: toggleBlock (DESBLOQUEAR) ---

    @Test
    @DisplayName("toggleBlock: Debería desbloquear si ya existía el bloqueo")
    void toggleBlock_WhenBlocked_ShouldUnblock() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("currentUser");
        given(userRepository.findByUsername("currentUser")).willReturn(Optional.of(currentUser));
        given(userRepository.findByUsername("targetUser")).willReturn(Optional.of(targetUser));

        Block existingBlock = Block.builder().blocker(currentUser).blocked(targetUser).build();
        given(blockRepository.findByBlockerAndBlocked(currentUser, targetUser)).willReturn(Optional.of(existingBlock));

        // When
        boolean isBlocked = blockService.toggleBlock("targetUser");

        // Then
        assertThat(isBlocked).isFalse(); // False significa que se desbloqueó

        verify(blockRepository).delete(existingBlock); // Se eliminó el bloqueo
        verify(followRepository, never()).deleteFollowRelation(any(), any()); // No se tocan follows al desbloquear
    }

    // --- TEST: Validaciones de Error ---

    @Test
    @DisplayName("toggleBlock: Debería lanzar excepción si intenta bloquearse a sí mismo")
    void toggleBlock_SelfBlock_ThrowsException() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("currentUser");
        given(userRepository.findByUsername("currentUser")).willReturn(Optional.of(currentUser));
        // El repositorio devuelve el MISMO usuario cuando buscamos el target
        given(userRepository.findByUsername("currentUser")).willReturn(Optional.of(currentUser));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                blockService.toggleBlock("currentUser")
        );

        verify(blockRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleBlock: Debería lanzar excepción si el usuario objetivo no existe")
    void toggleBlock_TargetNotFound_ThrowsException() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("currentUser");
        given(userRepository.findByUsername("currentUser")).willReturn(Optional.of(currentUser));
        given(userRepository.findByUsername("unknownUser")).willReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () ->
                blockService.toggleBlock("unknownUser")
        );
    }

    // --- TEST: getBlockedUsers ---

    @Test
    @DisplayName("getBlockedUsers: Debería devolver lista de DTOs correctamente mapeada")
    void getBlockedUsers_ShouldReturnList() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("currentUser");
        given(userRepository.findByUsername("currentUser")).willReturn(Optional.of(currentUser));

        // Simulamos que el repositorio devuelve una lista de entidades User (los bloqueados)
        List<User> blockedEntities = List.of(targetUser);
        given(blockRepository.findBlockedUsersByBlocker(currentUser)).willReturn(blockedEntities);

        // When
        List<BlockedUserDto> result = blockService.getBlockedUsers();

        // Then
        assertThat(result).hasSize(1);
        BlockedUserDto dto = result.get(0);

        assertThat(dto.username()).isEqualTo("targetUser");
        assertThat(dto.displayName()).isEqualTo("Target");
        assertThat(dto.avatarUrl()).isEqualTo("avatar.png"); // Usa el avatar del usuario
    }

    @Test
    @DisplayName("getBlockedUsers: Debería usar avatar por defecto si es nulo")
    void getBlockedUsers_NullAvatar_ShouldUseDefault() {
        // Given
        targetUser.setAvatarUrl(null); // Avatar nulo

        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("currentUser");
        given(userRepository.findByUsername("currentUser")).willReturn(Optional.of(currentUser));
        given(blockRepository.findBlockedUsersByBlocker(currentUser)).willReturn(List.of(targetUser));

        // When
        List<BlockedUserDto> result = blockService.getBlockedUsers();

        // Then
        assertThat(result.get(0).avatarUrl()).isEqualTo(defaultAvatar); // Usa el default inyectado
    }
}
