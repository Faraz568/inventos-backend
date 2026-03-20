package com.inventos.controller;

import com.inventos.dto.response.ApiResponse;
import com.inventos.entity.User;
import com.inventos.exception.BusinessException;
import com.inventos.exception.ResourceNotFoundException;
import com.inventos.repository.UserRepository;
import com.inventos.service.OtpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/users") @RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(
            @AuthenticationPrincipal UserDetails principal) {
        User user = find(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "id",         user.getId(),
            "username",   user.getUsername(),
            "fullName",   user.getFullName(),
            "email",      user.getEmail(),
            "phone",      user.getPhone() != null ? user.getPhone() : "",
            "role",       user.getRole().name(),
            "profilePic", user.getProfilePic() != null ? user.getProfilePic() : ""
        )));
    }

    @PutMapping("/me/profile-pic")
    public ResponseEntity<ApiResponse<Void>> updateProfilePic(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, String> body) {
        User user = find(principal.getUsername());
        String pic = body.get("profilePic");
        user.setProfilePic(pic != null && !pic.isBlank() ? pic : null);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Profile picture updated", null));
    }

    

    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ProfileRequest req) {
        User user = find(principal.getUsername());
        user.setFullName(req.getFullName().trim());
        user.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", null));
    }

    

    @PutMapping("/me/email")
    public ResponseEntity<ApiResponse<Void>> updateEmail(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody EmailRequest req) {
        
        boolean verified = otpService.verifyOtp(req.getEmail(), req.getOtp());
        if (!verified) throw new BusinessException("OTP is invalid or expired. Please request a new code.");

        if (userRepository.existsByEmail(req.getEmail()))
            throw new BusinessException("This email is already in use by another account.");

        User user = find(principal.getUsername());
        user.setEmail(req.getEmail().trim());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Email updated", null));
    }

    

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody PasswordRequest req) {
        User user = find(principal.getUsername());
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new BusinessException("Current password is incorrect.");
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Password changed", null));
    }

    

    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<User>>> all() {
        var users = userRepository.findAll();
        users.forEach(u -> u.setPassword("[PROTECTED]"));
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    

    @PatchMapping("/{id}/deactivate") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        User u = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        u.setIsActive(false);
        userRepository.save(u);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated", null));
    }

    

    private User find(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    

    @Data
    static class ProfileRequest {
        @NotBlank @Size(max = 100) private String fullName;
        @Size(max = 20)            private String phone;
    }

    @Data
    static class EmailRequest {
        @NotBlank @Email @Size(max = 100) private String email;
        @NotBlank                         private String otp;
    }

    @Data
    static class PasswordRequest {
        @NotBlank              private String currentPassword;
        @NotBlank @Size(min=6) private String newPassword;
    }
}
