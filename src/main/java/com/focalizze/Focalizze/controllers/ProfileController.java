package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.*;
import com.focalizze.Focalizze.services.FileStorageService;
import com.focalizze.Focalizze.services.ProfileService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final FileStorageService fileStorageService;

    public ProfileController(ProfileService profileService, FileStorageService fileStorageService) {
        this.profileService = profileService;
        this.fileStorageService = fileStorageService;
    }

    // Endpoint para obtener los datos del perfil
    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponseDto> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(profileService.getProfile(username));
    }

    // Endpoint para obtener los hilos del usuario con paginación
    @GetMapping("/{username}/threads")
    public ResponseEntity<Page<FeedThreadDto>> getProfileThreads(
            @PathVariable String username,
            @PageableDefault(size = 10) Pageable pageable) { // Spring crea el Pageable a partir de ?page=0&size=10
        return ResponseEntity.ok(profileService.getThreadsForUser(username, pageable));
    }

    // Endpoint para servir la imagen del avatar
    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
        Resource file = fileStorageService.loadFileAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PatchMapping("/{username}")
    public ResponseEntity<ProfileResponseDto> patchProfile(@PathVariable String username, // Se lee de la URL
                                                           @Valid @RequestBody ProfileUpdateRequestDto updateDto){
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!currentUsername.equals(username)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // 403 Forbidden
        }

        ProfileResponseDto updatedProfile = profileService.updateProfile(username,updateDto);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/{username}/avatar")
    public ResponseEntity<?> uploadAvatar(
            @PathVariable String username,
            @RequestParam("avatar") MultipartFile file // Recibe el archivo
    ) {

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!currentUsername.equals(username)) {
            return new ResponseEntity<>("No tienes permiso para cambiar el avatar de este perfil.", HttpStatus.FORBIDDEN);
        }

        try {
            String avatarUrl = profileService.updateAvatar(username, file);
            // Devolvemos un JSON simple con la nueva URL del avatar
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (Exception e) {
            return new ResponseEntity<>("No se pudo subir el archivo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{username}/download")
    public ResponseEntity<UserProfileDownloadDto> getProfileForDownload(@PathVariable String username) {
        // Verificación de seguridad: solo el dueño del perfil puede acceder a esta info.
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!currentUsername.equals(username)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // 403 Prohibido
        }

        return ResponseEntity.ok(profileService.getProfileForDownload(username));
    }
}
