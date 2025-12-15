package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.services.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Implementation of the {@link FileStorageService} interface.
 * Handles physical file storage on the server's local file system.
 * <p>
 * Implementación de la interfaz {@link FileStorageService}.
 * Maneja el almacenamiento físico de archivos en el sistema de archivos local del servidor.
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    @Autowired
    public FileStorageServiceImpl(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo crear el directorio para almacenar los archivos.", ex);
        }
    }

    /**
     * Stores a file in the local file system with a unique name.
     * Sanitizes the filename and prevents path traversal attacks.
     * <p>
     * Almacena un archivo en el sistema de archivos local con un nombre único.
     * Sanea el nombre del archivo y previene ataques de salto de directorio (path traversal).
     *
     * @param file     The multipart file to store.
     *                 El archivo multipart a almacenar.
     * @param username The username to append to the filename for traceability.
     *                 El nombre de usuario para agregar al nombre de archivo para trazabilidad.
     * @return The generated unique filename.
     *         El nombre de archivo único generado.
     * @throws RuntimeException If the file contains invalid characters or storage fails.
     *                          Si el archivo contiene caracteres inválidos o el almacenamiento falla.
     */
    @Override
    public String storeFile(MultipartFile file, String username) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (originalFilename.contains("..")) {
                throw new RuntimeException("El nombre del archivo contiene una secuencia de ruta inválida.");
            }
            // Genera un nombre de archivo único para evitar colisiones
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = username + "_" + System.currentTimeMillis() + fileExtension;

            Path targetLocation = this.fileStorageLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return newFilename;
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo almacenar el archivo " + originalFilename, ex);
        }
    }

    /**
     * Loads a file as a Resource from the file system.
     * <p>
     * Carga un archivo como un Recurso desde el sistema de archivos.
     *
     * @param filename The name of the file to load.
     *                 El nombre del archivo a cargar.
     * @return The loaded {@link Resource}.
     *         El {@link Resource} cargado.
     * @throws RuntimeException If the file is not found or cannot be read.
     *                          Si el archivo no se encuentra o no se puede leer.
     */
    @Override
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found / Archivo no encontrado: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found / Archivo no encontrado: " + filename, ex);
        }
    }

}

