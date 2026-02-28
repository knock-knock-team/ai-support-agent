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
                        .fullName("Системный Администратор")
                        .role(User.Role.ROLE_ADMIN)
                        .firstLogin(true)
                        .active(true)
                        .superAdmin(true)
                        .build();

                userRepository.save(superAdmin);
                log.info("✅ Супер Администратор создан: admin/admin123 (СМЕНИТЕ ПАРОЛЬ ПРИ ПЕРВОМ ВХОДЕ!)");
            } else {
                log.info("ℹ️ Супер Администратор уже существует");
            }
        };
    }
}
