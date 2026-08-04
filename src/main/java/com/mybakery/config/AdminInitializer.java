package com.mybakery.config;

import com.mybakery.model.User;
import com.mybakery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates a first administrator only from explicit deployment credentials. */
@Component
public class AdminInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String initialUsername;
    private final String initialPassword;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${APP_ADMIN_USERNAME:}") String initialUsername,
                            @Value("${APP_ADMIN_PASSWORD:}") String initialPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialUsername = initialUsername;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() != 0) {
            return;
        }
        if (initialUsername.isBlank() || initialPassword.length() < 12) {
            System.err.println("No admin was created. Set APP_ADMIN_USERNAME and a 12+ character APP_ADMIN_PASSWORD.");
            return;
        }
        User admin = new User(initialUsername, passwordEncoder.encode(initialPassword));
        userRepository.save(admin);
        System.out.println("Initial admin user created.");
    }
}
