package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.FollowService;
import com.focalizze.Focalizze.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final FollowService followService;
    private final UserService userService;

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
}
