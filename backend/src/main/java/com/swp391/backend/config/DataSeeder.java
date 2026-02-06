package com.swp391.backend.config;

import com.swp391.backend.entity.Role;
import com.swp391.backend.entity.User;
import com.swp391.backend.repository.RoleRepository;
import com.swp391.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        Optional<Role> adminRoleOpt = roleRepository.findByRoleCode("ADMIN");
        if (adminRoleOpt.isEmpty()) {
            throw new IllegalStateException("Missing role ADMIN in database. Please run SQL seed script first.");
        }
        Role adminRole = adminRoleOpt.get();

        if (!userRepository.existsByUsernameIgnoreCase("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@gmail.com");
            admin.setFullName("System Admin");
            admin.setRole(adminRole);
            admin.setPasswordHash(passwordEncoder.encode("admin123"));

            userRepository.save(admin);
        }
    }
}
