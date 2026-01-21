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
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public String saveImage(MultipartFile file, String folderPath) throws IOException {

        if (file.isEmpty()) {
            throw new InvalidRequestException("O arquivo não pode estar vazio.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidRequestException("O arquivo excede o tamanho máximo permitido de 5MB.");
        }

        String contentType = file.getContentType();
        System.out.println("Recebido arquivo: " + file.getOriginalFilename() + " Content-Type: " + contentType);

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidRequestException("Tipo de arquivo inválido: " + contentType + ". Apenas JPEG, PNG e WEBP são permitidos.");
        }

        String originalFileNames = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFileNames != null && originalFileNames.contains(".")) {
            fileExtension = originalFileNames.substring(originalFileNames.lastIndexOf("."));
        }

        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        
        // Define o diretório de destino (ex: uploads/users/1)
        Path uploadPath = Paths.get(uploadDir).resolve(folderPath);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Retorna o caminho relativo (ex: users/1/nome-arquivo.jpg)
        // Normaliza para barras normais para ser URL-friendly
        return Paths.get(folderPath).resolve(uniqueFileName).toString().replace("\\", "/");
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
