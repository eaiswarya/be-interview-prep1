package com.interviewprep.seeder;

import com.interviewprep.model.AppUser;
import com.interviewprep.model.Role;
import com.interviewprep.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Registration only creates USERs, so the first ADMIN is created at startup from the ADMIN_EMAIL and
 * ADMIN_PASSWORD environment variables. Nothing happens when they are not set.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository repository, PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String email, @Value("${app.admin.password:}") String password) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || repository.existsByEmail(email)) {
            return;
        }
        repository.save(new AppUser(email, passwordEncoder.encode(password), Role.ADMIN));
        log.info("Created ADMIN user {}", email);
    }
}
