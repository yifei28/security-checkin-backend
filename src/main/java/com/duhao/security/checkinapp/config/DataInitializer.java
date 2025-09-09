package com.duhao.security.checkinapp.config;

import com.duhao.security.checkinapp.entity.Admin;
import com.duhao.security.checkinapp.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@Profile("docker") // Only run in docker profile
public class DataInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Bean
    CommandLineRunner initDatabase(AdminRepository adminRepository) {
        return args -> {
            // Check if admin account exists
            if (adminRepository.findByUsername("admin").isEmpty()) {
                // Create password encoder instance
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                
                // Create default admin account
                Admin admin = new Admin();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("11235813"));
                admin.setSuperAdmin(true);
                
                adminRepository.save(admin);
                logger.info("Default admin account created with username: admin");
                logger.info("Password: 11235813");
                logger.info("Please change the default password after first login!");
            } else {
                logger.info("Admin account already exists, skipping creation");
            }
        };
    }
}