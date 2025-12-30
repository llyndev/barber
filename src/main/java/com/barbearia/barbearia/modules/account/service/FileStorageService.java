package com.barbearia.barbearia.modules.account.service;

import com.barbearia.barbearia.exception.InvalidRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    public String saveImage(MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new InvalidRequestException("O arquivo não pode estar vazio.");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidRequestException("Tipo de arquivo inválido.");
        }

        String originalFileNames = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFileNames != null && originalFileNames.contains(".")) {
            fileExtension = originalFileNames.substring(originalFileNames.lastIndexOf("."));
        }

        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileExtension;

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    public void deleteImage(String fileName) {
        if (fileName == null || fileName.isBlank()) return;

        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.out.println("Error " + e.getMessage());
        }
    }

}
