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
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    public FileStorageServiceImpl(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo crear el directorio para almacenar los archivos.", ex);
        }
    }

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

    @Override
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("Archivo no encontrado: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Archivo no encontrado: " + filename, ex);
        }
    }

    public String storeThreadImage(MultipartFile file) {
        try {
            // Validar nombre y extensión básica
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.contains("..")) {
                throw new RuntimeException("Nombre de archivo inválido: " + originalName);
            }

            // Generar nombre único
            String extension = originalName.substring(originalName.lastIndexOf("."));
            String storedFileName = "thread_" + UUID.randomUUID().toString() + extension;

            // Crear directorios si no existen
            Path targetLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(targetLocation);

            // Guardar
            Files.copy(file.getInputStream(), targetLocation.resolve(storedFileName), StandardCopyOption.REPLACE_EXISTING);

            return storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo guardar el archivo.", ex);
        }
    }

}

