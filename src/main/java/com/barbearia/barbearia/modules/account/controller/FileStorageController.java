package com.barbearia.barbearia.modules.account.controller;

import com.barbearia.barbearia.exception.ResourceNotFoundException;
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

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class FileStorageController {

    private final UserRepository userRepository;
    private final BusinessService businessService;
    private final FileStorageService fileStorageService;

    @PostMapping("/{id}")
    public ResponseEntity<String> uploadProfilePhoto(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                     @RequestParam("file")MultipartFile file) {
        try {
            Long userId = userDetails.user().getId();
            AppUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (user.getProfileImage() != null) {
                fileStorageService.deleteImage(user.getProfileImage());
            }

            String fileName = fileStorageService.saveImage(file);

            user.setProfileImage(fileName);
            userRepository.save(user);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();

            return ResponseEntity.ok(fileDownloadUri);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error" + e.getMessage());
        }
    }

    @PostMapping("/business/{businessId}/{type}")
    public ResponseEntity<String> uploadBusinessImage(
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

            return ResponseEntity.ok(fileDownloadUri);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
