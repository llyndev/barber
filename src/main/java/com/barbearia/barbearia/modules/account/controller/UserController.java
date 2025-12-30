package com.barbearia.barbearia.modules.account.controller;

import com.barbearia.barbearia.modules.business.dto.request.PromoteToOwnerRequest;
import com.barbearia.barbearia.modules.business.dto.request.UpdateRoleRequest;
import com.barbearia.barbearia.modules.account.dto.request.UserRequest;
import com.barbearia.barbearia.modules.account.dto.response.UserResponse;
import com.barbearia.barbearia.modules.account.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping("/barbers")
    public List<UserResponse> getBarbers() {
        return userService.listBarbers();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @RequestBody UserRequest userRequest) {
        return ResponseEntity.ok(userService.update(id, userRequest));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<UserResponse> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest role) {
        return ResponseEntity.ok(userService.updateRole(id, role));
    }

    @PostMapping("/{id}/promote-to-owner")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<UserResponse> promoteToOwner(
            @PathVariable Long id, 
            @RequestBody PromoteToOwnerRequest request) {
        return ResponseEntity.ok(userService.promoteToOwner(id, request));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Void> renewSubscription(@PathVariable Long id, @RequestParam(defaultValue = "30") int days) {
        userService.renewSubscription(id, days);
        return ResponseEntity.ok().build();
    }

}
