package com.inventos.dto.response;

import com.inventos.entity.User;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String   accessToken;
    private String   tokenType = "Bearer";
    private long     expiresIn;
    private UserInfo user;

    public static AuthResponse of(String token, long expiresIn, UserInfo user) {
        return AuthResponse.builder().accessToken(token).tokenType("Bearer")
               .expiresIn(expiresIn).user(user).build();
    }
    public static UserInfo userInfo(User u) {
        return UserInfo.builder().id(u.getId()).username(u.getUsername())
               .fullName(u.getFullName()).email(u.getEmail()).role(u.getRole().name()).build();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserInfo {
        private Long   id;
        private String username;
        private String fullName;
        private String email;
        private String role;
    }
}
