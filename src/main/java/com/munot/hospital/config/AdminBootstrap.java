package com.munot.hospital.config;

import com.munot.hospital.entity.Role;
import com.munot.hospital.entity.User;
import com.munot.hospital.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the one initial ADMIN account on first start (empty users table) from ADMIN_EMAIL / ADMIN_PASSWORD.
 * Everything else (doctors, receptionists, content) is then created from the Admin panel.
 * No credentials are hard-coded; the password is only read from the environment.
 */
@Component
@Order(1)
public class AdminBootstrap implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminBootstrap(AppProperties props, UserRepository users, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (props.seedDemo() || users.count() > 0) return;
        AppProperties.Admin a = props.admin();
        String email = a == null ? null : a.email();
        String pw = a == null ? null : a.password();
        if (email == null || email.isBlank() || pw == null || pw.isBlank()) {
            log.error("No users exist and ADMIN_EMAIL / ADMIN_PASSWORD are not set. Set them and restart to create the first admin account.");
            return;
        }
        if (!pw.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$")) {
            log.error("ADMIN_PASSWORD must be 8-72 characters with at least one letter and one digit. Admin account NOT created.");
            return;
        }
        User u = new User();
        u.setEmail(email.trim().toLowerCase());
        u.setFullName(a.name() == null || a.name().isBlank() ? "Hospital Admin" : a.name().trim());
        u.setPasswordHash(encoder.encode(pw));
        u.setRole(Role.ADMIN);
        users.save(u);
        log.warn("Initial admin account created for {}. Sign in, change the password, then remove ADMIN_PASSWORD from the environment.", u.getEmail());
    }
}
