package com.interviewprep.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 254, message = "email must be at most 254 characters")
        String email,

        // BCrypt only uses the first 72 bytes, so longer passwords would be silently truncated.
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
        String password) {
}
