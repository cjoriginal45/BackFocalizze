package com.focalizze.Focalizze.services;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file, String username);
    Resource loadFileAsResource(String filename);
}
