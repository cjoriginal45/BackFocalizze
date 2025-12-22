package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.*;
import com.focalizze.Focalizze.services.FileStorageService;
import com.focalizze.Focalizze.services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/**
 * Controller for managing user profiles.
 * Handles viewing, updating, and asset management (avatars) for profiles.
 * <p>
 * Controlador para gestionar perfiles de usuario.
 * Maneja la visualización, actualización y gestión de activos (avatars) para los perfiles.
 */
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final FileStorageService fileStorageService;


    /**
     * Retrieves public profile information for a user.
     * <p>
     * Recupera información de perfil público para un usuario.
     *
     * @param username The username of the profile. / El nombre de usuario del perfil.
     * @return Profile DTO. / DTO del perfil.
     */
    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponseDto> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(profileService.getProfile(username));
    }

    /**
     * Retrieves a paginated list of threads created by a specific user.
     * <p>
     * Recupera una lista paginada de hilos creados por un usuario específico.
     *
     * @param username The username. / El nombre de usuario.
     * @param pageable Pagination info (default size 10). / Información de paginación (tamaño por defecto 10).
     * @return Page of threads. / Página de hilos.
     */
    @GetMapping("/{username}/threads")
    public ResponseEntity<Page<FeedThreadDto>> getProfileThreads(
            @PathVariable String username,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(profileService.getThreadsForUser(username, pageable));
    }

    /**
     * Serves the avatar image file.
     * <p>
     * Sirve el archivo de imagen del avatar.
     *
     * @param filename The name of the file to retrieve. / El nombre del archivo a recuperar.
     * @return The image resource. / El recurso de imagen.
     */
    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
        Resource file = fileStorageService.loadFileAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"").body(file);
    }

    /**
     * Updates profile information (bio, display name).
     * <p>
     * Actualiza la información del perfil (biografía, nombre para mostrar).
     *
     * @param username  The username to update. / El nombre de usuario a actualizar.
     * @param updateDto The new data. / Los nuevos datos.
     * @return The updated profile DTO. / El DTO del perfil actualizado.
     */
    @PatchMapping("/{username}")
    public ResponseEntity<ProfileResponseDto> patchProfile(@PathVariable String username,
                                                           @Valid @RequestBody ProfileUpdateRequestDto updateDto){
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (currentUsername==null || !currentUsername.equals(username)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        ProfileResponseDto updatedProfile = profileService.updateProfile(username,updateDto);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Uploads and updates the user's avatar.
     * <p>
     * Sube y actualiza el avatar del usuario.
     *
     * @param username  The username. / El nombre de usuario.
     * @param file      The image file. / El archivo de imagen.
     * @return Map with the new avatar URL. / Mapa con la nueva URL del avatar.
     */
    @PostMapping("/{username}/avatar")
    public ResponseEntity<?> uploadAvatar(
            @PathVariable String username,
            @RequestParam("avatar") MultipartFile file
    ) {

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (currentUsername==null || !currentUsername.equals(username)) {
            return new ResponseEntity<>("No tienes permiso para cambiar el avatar de este perfil.", HttpStatus.FORBIDDEN);
        }

        try {
            String avatarUrl = profileService.updateAvatar(username, file);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (Exception e) {
            return new ResponseEntity<>("No se pudo subir el archivo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Downloads profile data (GDPR/Backup).
     * <p>
     * Descarga datos del perfil (GDPR/Backup).
     *
     * @param username  The username. / El nombre de usuario.
     * @return The download DTO. / El DTO de descarga.
     */
    @GetMapping("/{username}/download")
    public ResponseEntity<UserProfileDownloadDto> getProfileForDownload(@PathVariable String username) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (currentUsername==null || !currentUsername.equals(username)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        return ResponseEntity.ok(profileService.getProfileForDownload(username));
    }
}
