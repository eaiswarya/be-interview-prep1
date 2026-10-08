package com.interviewprep.auth;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final AuthService service;

    public UserController(AuthService service) {
        this.service = service;
    }

    /** The user id comes from the verified token, never from the request, so users can only see themselves. */
    @GetMapping("/api/users/me")
    public UserResponse me(@AuthenticationPrincipal Jwt token) {
        return service.profile(Long.valueOf(token.getSubject()));
    }

    /** ADMIN only: enforced in SecurityConfig for every /api/admin/** path. */
    @GetMapping("/api/admin/users")
    public List<UserResponse> listUsers() {
        return service.listAll();
    }
}
