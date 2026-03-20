package com.inventos.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, digits, underscores")
    private String username;
    @NotBlank @Size(max = 100) private String fullName;
    @NotBlank @Email @Size(max = 100) private String email;
    @NotBlank @Size(min = 6, max = 72) private String password;
}
