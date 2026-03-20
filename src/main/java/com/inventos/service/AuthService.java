package com.inventos.service;

import com.inventos.dto.request.*;
import com.inventos.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
    void logout(String bearerToken);
}
