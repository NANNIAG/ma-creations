package com.macreations.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.macreations.entity.AdminUser;
import com.macreations.repository.AdminUserRepository;

/**
 * Creates the first admin from env vars when the table is empty.
 * Passwords are hashed with BCrypt — never stored in plain text.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapEmail;
    private final String bootstrapPassword;

    public AdminBootstrapRunner(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.admin-bootstrap.email:}") String bootstrapEmail,
            @Value("${app.security.admin-bootstrap.password:}") String bootstrapPassword) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepository.count() > 0) {
            return;
        }
        if (!StringUtils.hasText(bootstrapEmail) || !StringUtils.hasText(bootstrapPassword)) {
            log.warn(
                    "No admin users found. Set ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD to create the first admin.");
            return;
        }

        AdminUser admin = new AdminUser();
        admin.setLoginIdentifier(bootstrapEmail.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setEnabled(true);
        adminUserRepository.save(admin);
        log.info("Bootstrapped initial admin user for identifier '{}'", admin.getLoginIdentifier());
    }
}
