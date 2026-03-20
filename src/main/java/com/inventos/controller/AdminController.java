package com.inventos.controller;

import com.inventos.dto.response.ApiResponse;
import com.inventos.entity.User;
import com.inventos.exception.*;
import com.inventos.repository.*;
import com.inventos.security.TokenBlacklistService;
import com.inventos.service.SaleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository        userRepository;
    private final ProductRepository     productRepository;
    private final SaleService           saleService;
    private final PasswordEncoder       passwordEncoder;
    private final TokenBlacklistService blacklistService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        long totalUsers  = userRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(User::getIsActive).count();
        long totalProds  = productRepository.countByIsActiveTrue();
        long outOfStock  = productRepository.countOutOfStock();
        long lowStock    = productRepository.countLowStock();
        var  saleStats   = saleService.getStats();

        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalUsers",    totalUsers);
        stats.put("activeUsers",   activeUsers);
        stats.put("totalProducts", totalProds);
        stats.put("outOfStock",    outOfStock);
        stats.put("lowStock",      lowStock);
        stats.put("totalSales",    saleStats.total());
        stats.put("totalRevenue",  saleStats.totalRevenue());
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setPassword("[PROTECTED]"));
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createUser(@Valid @RequestBody CreateUserRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new ConflictException("Username '" + req.getUsername() + "' already taken");
        User user = User.builder()
            .username(req.getUsername()).fullName(req.getFullName()).email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .role(User.Role.valueOf(req.getRole().toUpperCase())).isActive(true).build();
        User saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("User created",
            Map.of("id", saved.getId(), "username", saved.getUsername(), "role", saved.getRole())));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<Void>> changeRole(@PathVariable Long id, @RequestParam String role) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        try { user.setRole(User.Role.valueOf(role.toUpperCase())); }
        catch (IllegalArgumentException e) { throw new BusinessException("Invalid role: " + role); }
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Role updated to " + role.toUpperCase(), null));
    }

    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setIsActive(false); userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated", null));
    }

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setIsActive(true); userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("User activated", null));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        userRepository.delete(user);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }

    @Data
    static class CreateUserRequest {
        @NotBlank private String username;
        @NotBlank private String fullName;
        @NotBlank private String email;
        @NotBlank @Size(min = 6) private String password;
        @NotBlank private String role;
    }
}
