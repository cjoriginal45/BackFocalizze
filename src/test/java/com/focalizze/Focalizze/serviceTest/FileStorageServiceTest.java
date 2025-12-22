package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.services.servicesImpl.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class FileStorageServiceTest {
    // JUnit 5 inyecta una ruta temporal aislada aquí.
    // Se crea antes de cada test y se borra después.
    @TempDir
    Path tempDir;

    private FileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        // Inicializamos el servicio pasando la ruta temporal como el directorio de subida.
        // Esto simula la propiedad "${file.upload-dir}"
        fileStorageService = new FileStorageServiceImpl(tempDir.toString());
    }

    @Test
    @DisplayName("storeFile: Debería guardar el archivo y devolver un nombre único")
    void storeFile_Success() {
        // Given: Un archivo simulado (MockMultipartFile es una utilidad de Spring Test)
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "test-image.png",
                "image/png",
                "contenido-de-prueba".getBytes()
        );
        String username = "testuser";

        // When
        String storedFilename = fileStorageService.storeFile(file, username);

        // Then
        // 1. Verificar que el nombre generado contiene el username y la extensión
        assertThat(storedFilename).startsWith("testuser_");
        assertThat(storedFilename).endsWith(".png");

        // 2. Verificar que el archivo FÍSICAMENTE existe en el directorio temporal
        Path filePath = tempDir.resolve(storedFilename);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @Test
    @DisplayName("storeFile: Debería lanzar excepción si el nombre contiene secuencias inválidas (..)")
    void storeFile_InvalidPath_ThrowsException() {
        // Given: Nombre de archivo malicioso
        MockMultipartFile badFile = new MockMultipartFile(
                "data",
                "../hack.exe",
                "text/plain",
                "malicious".getBytes()
        );

        // When & Then
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                fileStorageService.storeFile(badFile, "user")
        );
        assertThat(ex.getMessage()).contains("secuencia de ruta inválida");
    }

    @Test
    @DisplayName("loadFileAsResource: Debería cargar un recurso existente")
    void loadFileAsResource_Success() throws IOException {
        // Given: Creamos manualmente un archivo en el directorio temporal
        String filename = "existing_file.txt";
        Path filePath = tempDir.resolve(filename);
        Files.writeString(filePath, "Hola Mundo");

        // When
        Resource resource = fileStorageService.loadFileAsResource(filename);

        // Then
        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    @DisplayName("loadFileAsResource: Debería lanzar excepción si el archivo no existe")
    void loadFileAsResource_NotFound_ThrowsException() {
        // Given
        String nonExistentFile = "ghost.png";

        // When & Then
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                fileStorageService.loadFileAsResource(nonExistentFile)
        );
        assertThat(ex.getMessage()).contains("Archivo no encontrado");
    }

    @Test
    @DisplayName("Constructor: Debería crear el directorio si no existe")
    void constructor_ShouldCreateDirectory() {
        // Given: Una ruta dentro del tempDir que aun no existe
        Path newUploadDir = tempDir.resolve("uploads");

        // When: Instanciamos el servicio apuntando a esa ruta
        new FileStorageServiceImpl(newUploadDir.toString());

        // Then: El directorio debe haber sido creado
        assertThat(Files.exists(newUploadDir)).isTrue();
        assertThat(Files.isDirectory(newUploadDir)).isTrue();
    }
}
