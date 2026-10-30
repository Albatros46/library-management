package com.library.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Creates the first admin account on an empty database. */
    @Bean
    ApplicationRunner seedAdmin(StaffUserRepository users, PasswordEncoder encoder,
                                @Value("${library.admin.email}") String email,
                                @Value("${library.admin.password}") String password) {
        return args -> {
            if (users.count() == 0) {
                StaffUser admin = new StaffUser();
                admin.setFullName("Sistem Yöneticisi");
                admin.setEmail(email.toLowerCase());
                admin.setPasswordHash(encoder.encode(password));
                admin.setRole(StaffUser.Role.ADMIN);
                users.save(admin);
            }
        };
    }
}
