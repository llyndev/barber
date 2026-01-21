package com.barbearia.barbearia.modules.account.controller;

import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.account.dto.UploadResponse;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.modules.business.service.BusinessService;
import com.barbearia.barbearia.modules.account.service.FileStorageService;
import com.barbearia.barbearia.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class FileStorageController {

    private final UserRepository userRepository;
    private final BusinessService businessService;
    private final FileStorageService fileStorageService;

    @PostMapping("/profile")
    public ResponseEntity<UploadResponse> uploadProfilePhoto(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                             @RequestParam("file") MultipartFile file) {
        
        Long userId = userDetails.user().getId();
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            String folder = "users/" + userId;
            String fileName = fileStorageService.saveImage(file, folder);

            String oldImage = user.getProfileImage();
            
            user.setProfileImage(fileName);
            userRepository.save(user);
            
            if (oldImage != null) {
                fileStorageService.deleteImage(oldImage);
            }

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();

            return ResponseEntity.ok(new UploadResponse(fileDownloadUri, fileName));

        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteProfileImage(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.user().getId();
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getProfileImage() != null) {
            fileStorageService.deleteImage(user.getProfileImage());
            user.setProfileImage(null);
            userRepository.save(user);
        }
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/business/{businessId}/{type}")
    public ResponseEntity<UploadResponse> uploadBusinessImage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long businessId,
            @PathVariable String type,
            @RequestParam("file") MultipartFile file) {
        
        try {
            String fileName = businessService.updateBusinessImage(businessId, userDetails.user().getId(), type, file);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();

            return ResponseEntity.ok(new UploadResponse(fileDownloadUri, fileName));

        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/business/{businessId}/{type}")
    public ResponseEntity<Void> deleteBusinessImage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long businessId,
            @PathVariable String type) {
        
        businessService.removeBusinessImage(businessId, userDetails.user().getId(), type);
        return ResponseEntity.noContent().build();
    }
}
