package com.inventos.controller;

import com.inventos.dto.request.*;
import com.inventos.dto.response.*;
import com.inventos.exception.BusinessException;
import com.inventos.exception.ResourceNotFoundException;
import com.inventos.repository.UserRepository;
import com.inventos.security.JwtUtil;
import com.inventos.security.UserDetailsServiceImpl;
import com.inventos.service.AuthService;
import com.inventos.service.OtpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@RestController @RequestMapping("/auth") @RequiredArgsConstructor
public class AuthController {

    private final AuthService            authService;
    private final OtpService             otpService;
    private final UserRepository         userRepository;
    private final PasswordEncoder        passwordEncoder;
    private final AuthenticationManager  authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil                jwtUtil;

    
    private final Map<String, Boolean> pendingSessions = new ConcurrentHashMap<>();

    

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Account created", authService.register(req)));
    }

    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(req)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        authService.logout(auth);
        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }

    

    
    @PostMapping("/initiate-login")
    public ResponseEntity<ApiResponse<Void>> initiateLogin(@Valid @RequestBody LoginRequest req) {
        
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        var user = userRepository.findByUsername(req.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        
        otpService.sendLoginOtp(user.getEmail(), req.getUsername());
        pendingSessions.put(req.getUsername(), true);

        return ResponseEntity.ok(ApiResponse.ok(
            "Verification code sent to " + maskEmail(user.getEmail()), null));
    }

    
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyLoginOtp(
            @Valid @RequestBody LoginOtpRequest req) {

        if (!pendingSessions.containsKey(req.getUsername()))
            throw new BusinessException("No pending login session. Please start from Step 1.");

        boolean valid = otpService.verifyLoginOtp(req.getUsername(), req.getOtp());
        if (!valid) throw new BusinessException("Invalid or expired verification code.");

        pendingSessions.remove(req.getUsername());

        var user = userRepository.findByUsername(req.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserDetails ud = userDetailsService.loadUserByUsername(user.getUsername());
        AuthResponse auth = AuthResponse.of(
            jwtUtil.generateToken(ud),
            jwtUtil.getExpirationSeconds(),
            AuthResponse.userInfo(user)
        );
        return ResponseEntity.ok(ApiResponse.ok("Login successful", auth));
    }

    

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody OtpRequest req) {
        otpService.sendOtp(req.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Verification code sent to " + req.getEmail(), null));
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<ApiResponse<Void>> verifyEmailOtp(@Valid @RequestBody VerifyOtpRequest req) {
        boolean valid = otpService.verifyOtp(req.getEmail(), req.getOtp());
        if (!valid) throw new BusinessException("Invalid or expired verification code.");
        return ResponseEntity.ok(ApiResponse.ok("Email verified successfully", null));
    }

    

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody OtpRequest req) {
        userRepository.findByEmail(req.getEmail()).ifPresent(u ->
            otpService.sendPasswordResetOtp(req.getEmail())
        );
        return ResponseEntity.ok(ApiResponse.ok(
            "If an account with that email exists, a reset code has been sent.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        boolean valid = otpService.verifyPasswordResetOtp(req.getEmail(), req.getOtp());
        if (!valid) throw new BusinessException("Invalid or expired reset code.");
        var user = userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully. You can now sign in.", null));
    }

    

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "*".repeat(Math.min(at - 1, 3)) + email.substring(at);
    }

    

    @Data static class OtpRequest {
        @NotBlank @Email private String email;
    }

    @Data static class VerifyOtpRequest {
        @NotBlank @Email private String email;
        @NotBlank        private String otp;
    }

    @Data static class LoginOtpRequest {
        @NotBlank private String username;
        @NotBlank private String otp;
    }

    @Data static class ResetPasswordRequest {
        @NotBlank @Email              private String email;
        @NotBlank                     private String otp;
        @NotBlank @Size(min = 6)      private String newPassword;
    }
}
