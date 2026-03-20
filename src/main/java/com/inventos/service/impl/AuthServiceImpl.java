package com.inventos.service.impl;

import com.inventos.dto.request.*;
import com.inventos.dto.response.AuthResponse;
import com.inventos.entity.User;
import com.inventos.exception.ConflictException;
import com.inventos.repository.UserRepository;
import com.inventos.security.*;
import com.inventos.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service @RequiredArgsConstructor @Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil               jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService    userDetailsService;

    @Override @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new ConflictException("Username '" + req.getUsername() + "' already taken");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new ConflictException("Email '" + req.getEmail() + "' already registered");
        User user = User.builder()
            .username(req.getUsername()).fullName(req.getFullName()).email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword())).role(User.Role.USER).isActive(true).build();
        userRepository.save(user);
        log.info("Registered: {}", user.getUsername());
        UserDetails ud = userDetailsService.loadUserByUsername(user.getUsername());
        return AuthResponse.of(jwtUtil.generateToken(ud), jwtUtil.getExpirationSeconds(), AuthResponse.userInfo(user));
    }

    @Override @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
        UserDetails ud = userDetailsService.loadUserByUsername(user.getUsername());
        log.info("Login: {}", user.getUsername());
        return AuthResponse.of(jwtUtil.generateToken(ud), jwtUtil.getExpirationSeconds(), AuthResponse.userInfo(user));
    }

    @Override
    public void logout(String bearerToken) {
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) return;
        try {
            String token = bearerToken.substring(7);
            blacklistService.revoke(jwtUtil.extractJti(token), jwtUtil.extractExpiration(token));
        } catch (Exception e) { log.debug("Logout token invalid, ignoring"); }
    }
}
