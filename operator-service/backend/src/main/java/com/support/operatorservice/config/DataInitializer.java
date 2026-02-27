package com.support.operatorservice.config;

import com.support.operatorservice.entity.User;
import com.support.operatorservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Инициализирует данные при запуске приложения
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner initializeData() {
        return args -> {
            // Создаем Super Admin если его еще нет
            if (!userRepository.existsByUsername("admin")) {
                User superAdmin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("System Administrator")
                        .role(User.Role.ROLE_ADMIN)
                        .firstLogin(true)
                        .active(true)
                        .superAdmin(true)
                        .build();

                userRepository.save(superAdmin);
                log.info("✅ Super Admin created: admin/admin123 (CHANGE PASSWORD ON FIRST LOGIN!)");
            } else {
                log.info("ℹ️ Super Admin already exists");
            }
        };
    }
}
