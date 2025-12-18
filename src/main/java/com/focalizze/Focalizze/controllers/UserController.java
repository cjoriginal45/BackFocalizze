package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.BlockedUserDto;
import com.focalizze.Focalizze.dto.UpdateThemeDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.dto.UserSummaryDto;
import com.focalizze.Focalizze.dto.mappers.UserMapper;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.BlockService;
import com.focalizze.Focalizze.services.FollowService;
import com.focalizze.Focalizze.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing User profiles and relationships.
 * <p>
 * Controlador para gestionar perfiles de Usuario y relaciones.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final FollowService followService;
    private final UserService userService;
    private final BlockService blockService;
    private final UserMapper userMapper;

    /**
     * Retrieves the list of followers for a user.
     * <p>
     * Recupera la lista de seguidores para un usuario.
     *
     * @param username    The username to inspect. / El nombre de usuario a inspeccionar.
     * @param currentUser The currently authenticated user (optional). / El usuario actualmente autenticado (opcional).
     * @return List of followers. / Lista de seguidores.
     */
    @GetMapping("/{username}/followers")
    public ResponseEntity<List<UserSummaryDto>> getUserFollowers(
            @PathVariable String username,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(followService.getFollowers(username, currentUser));
    }

    /**
     * Retrieves the list of users a specific user is following.
     * <p>
     * Recupera la lista de usuarios que un usuario específico está siguiendo.
     *
     * @param username    The username to inspect. / El nombre de usuario a inspeccionar.
     * @param currentUser The currently authenticated user (optional). / El usuario actualmente autenticado (opcional).
     * @return List of followed users. / Lista de usuarios seguidos.
     */
    @GetMapping("/{username}/following")
    public ResponseEntity<List<UserSummaryDto>> getUserFollowing(
            @PathVariable String username,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(followService.getFollowing(username, currentUser));
    }

    /**
     * Toggles the follow status for a user.
     * <p>
     * Alterna el estado de seguimiento para un usuario.
     *
     * @param username    The username to follow/unfollow. / El nombre de usuario a seguir/dejar de seguir.
     * @param currentUser The authenticated user. / El usuario autenticado.
     * @return Empty response. / Respuesta vacía.
     */
    @PostMapping("/{username}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleFollow(
            @PathVariable String username,
            @AuthenticationPrincipal User currentUser
    ){
        followService.toggleFollowUser(username, currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves a user's public profile details.
     * <p>
     * Recupera los detalles del perfil público de un usuario.
     *
     * @param username    The username. / El nombre de usuario.
     * @param currentUser The authenticated user (optional). / El usuario autenticado (opcional).
     * @return User profile DTO. / DTO del perfil de usuario.
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserDto> getUserProfile(
            @PathVariable String username,
            @AuthenticationPrincipal User currentUser
    ) {
        UserDto userDto = userService.getUserProfile(username, currentUser);
        return ResponseEntity.ok(userDto);
    }

    /**
     * Retrieves the authenticated user's own profile.
     * <p>
     * Recupera el propio perfil del usuario autenticado.
     *
     * @param currentUser The authenticated user. / El usuario autenticado.
     * @return User profile DTO. / DTO del perfil de usuario.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // Asegura que solo usuarios logueados puedan acceder
    public ResponseEntity<UserDto> getMyProfile(
            @AuthenticationPrincipal User currentUser
    ) {
        // Maps entity directly to DTO as per logic
        // Mapea la entidad directamente a DTO según la lógica
        UserDto userDto = userMapper.toDto(currentUser);
        return ResponseEntity.ok(userDto);
    }

    /**
     * Toggles the block status for a user.
     * <p>
     * Alterna el estado de bloqueo para un usuario.
     *
     * @param username The username to block/unblock. / El nombre de usuario a bloquear/desbloquear.
     * @return Map with new block status. / Mapa con el nuevo estado de bloqueo.
     */
    @PostMapping("/{username}/block")
    public ResponseEntity<Map<String, Boolean>> toggleBlock(@PathVariable String username) {
        boolean isBlocked = blockService.toggleBlock(username);
        return ResponseEntity.ok(Map.of("isBlocked", isBlocked));
    }

    /**
     * Retrieves the list of blocked users.
     * <p>
     * Recupera la lista de usuarios bloqueados.
     *
     * @return List of blocked users. / Lista de usuarios bloqueados.
     */
    @GetMapping("/blocked")
    public ResponseEntity<List<BlockedUserDto>> getBlockedUsersList() {
        return ResponseEntity.ok(blockService.getBlockedUsers());
    }

    /**
     * Updates the user's theme preferences.
     * <p>
     * Actualiza las preferencias de tema del usuario.
     *
     * @param dto         The theme update data. / Los datos de actualización del tema.
     * @param currentUser The authenticated user. / El usuario autenticado.
     * @return Empty response. / Respuesta vacía.
     */
    @PatchMapping("/me/theme")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateTheme(
            @RequestBody UpdateThemeDto dto,
            @AuthenticationPrincipal User currentUser
    ) {
        userService.updateThemePreferences(currentUser.getUsername(), dto);
        return ResponseEntity.ok().build();
    }
}
