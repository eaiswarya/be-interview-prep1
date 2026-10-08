package com.interviewprep.auth;

import java.time.Instant;

/** Never includes the password hash. */
public record UserResponse(Long id, String email, Role role, Instant createdAt) {

    static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
