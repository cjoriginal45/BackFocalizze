package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.BlockedUserDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.dto.UserSummaryDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.BlockService;
import com.focalizze.Focalizze.services.FollowService;
import com.focalizze.Focalizze.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final FollowService followService;
    private final UserService userService;
    private final BlockService blockService;

    // Helper para obtener usuario actual del contexto de seguridad de forma segura
    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return null;
    }

    // --- NUEVOS ENDPOINTS PARA EL MODAL DE SEGUIDORES/SEGUIDOS ---

    @GetMapping("/{username}/followers")
    public ResponseEntity<List<UserSummaryDto>> getUserFollowers(@PathVariable String username) {
        // Pasamos el currentUser para calcular el booleano 'isFollowing' en la lista
        return ResponseEntity.ok(followService.getFollowers(username, getCurrentUser()));
    }

    @GetMapping("/{username}/following")
    public ResponseEntity<List<UserSummaryDto>> getUserFollowing(@PathVariable String username) {
        // Pasamos el currentUser para calcular el booleano 'isFollowing' en la lista
        return ResponseEntity.ok(followService.getFollowing(username, getCurrentUser()));
    }

    @PostMapping("/{username}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleFollow(@PathVariable String username) {
        // Obtenemos al usuario autenticado que realiza la acción.
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        followService.toggleFollowUser(username, currentUser);

        // Devolvemos 200 OK sin cuerpo, ya que es una acción.
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserDto> getUserProfile(@PathVariable String username) {
        // Obtenemos al usuario que hace la petición (puede ser anónimo)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            currentUser = (User) authentication.getPrincipal();
        }

        UserDto userDto = userService.getUserProfile(username, currentUser);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // Asegura que solo usuarios logueados puedan acceder
    public ResponseEntity<UserDto> getMyProfile() {
        // Obtenemos el usuario autenticado del contexto de seguridad.
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Mapeamos la entidad al DTO.
        UserDto userDto = userService.mapToUserDto(currentUser);

        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/{username}/block")
    public ResponseEntity<Map<String, Boolean>> toggleBlock(@PathVariable String username) {
        boolean isBlocked = blockService.toggleBlock(username);
        // Devolvemos el estado final del bloqueo
        return ResponseEntity.ok(Map.of("isBlocked", isBlocked));
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<BlockedUserDto>> getBlockedUsersList() {
        return ResponseEntity.ok(blockService.getBlockedUsers());
    }
}
