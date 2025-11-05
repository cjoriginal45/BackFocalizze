package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final FollowService followService;

    @PostMapping("/{username}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleFollow(@PathVariable String username) {
        // Obtenemos al usuario autenticado que realiza la acción.
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        followService.toggleFollowUser(username, currentUser);

        // Devolvemos 200 OK sin cuerpo, ya que es una acción.
        return ResponseEntity.ok().build();
    }
}
