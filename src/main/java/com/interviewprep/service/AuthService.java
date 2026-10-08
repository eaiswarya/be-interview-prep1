package com.interviewprep.service;

import com.interviewprep.dto.LoginRequest;
import com.interviewprep.dto.RegisterRequest;
import com.interviewprep.dto.TokenResponse;
import com.interviewprep.dto.UserResponse;
import com.interviewprep.exception.ConflictException;
import com.interviewprep.exception.ResourceNotFoundException;
import com.interviewprep.model.AppUser;
import com.interviewprep.model.Role;
import com.interviewprep.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final String unknownUserHash;

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.unknownUserHash = passwordEncoder.encode("placeholder-for-unknown-users");
    }

    /** Self-registration always creates a USER; nobody can make themselves ADMIN. */
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (repository.existsByEmail(email)) {
            throw new ConflictException("Email " + email + " is already registered");
        }
        try {
            AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), Role.USER);
            return UserResponse.from(repository.saveAndFlush(user));
        } catch (DataIntegrityViolationException raceOnUniqueEmail) {
            // Two registrations for the same email at once: the unique constraint lets only one through.
            throw new ConflictException("Email " + email + " is already registered");
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Optional<AppUser> user = repository.findByEmail(normalize(request.email()));
        // Run BCrypt even for unknown emails, so response time does not reveal which emails are registered.
        String hash = user.map(AppUser::getPasswordHash).orElse(unknownUserHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            // Same message for both cases, for the same reason.
            throw new BadCredentialsException("Invalid email or password");
        }
        return tokenService.issue(user.get());
    }

    @Transactional(readOnly = true)
    public UserResponse profile(Long userId) {
        return repository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return repository.findAll().stream().map(UserResponse::from).toList();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
