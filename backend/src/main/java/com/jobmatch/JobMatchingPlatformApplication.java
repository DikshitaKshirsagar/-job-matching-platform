package com.jobmatch;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;

import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.UserRole;
import com.jobmatch.domain.repository.UserRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootApplication
@EnableJpaAuditing
public class JobMatchingPlatformApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(JobMatchingPlatformApplication.class, args);
    }

    private static void loadDotenv() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .directory("./")
                .load();

        dotenv.entries().forEach(entry -> {
            if (System.getenv(entry.getKey()) == null && System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
    }

    @Bean
    CommandLineRunner validateDatabaseConnection(DataSource dataSource) {
        return args -> {
            try (Connection ignored = dataSource.getConnection()) {
                System.out.println("Database connection established successfully.");
            } catch (SQLException ex) {
                throw new IllegalStateException("Unable to connect to the database. Please check DB_URI/DB_URL, credentials, and network connectivity.", ex);
            }
        };
    }

    @Bean
    @Profile("prod")
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("test@example.com").isEmpty()) {
                User testUser = new User();
                testUser.setName("Test User");
                testUser.setEmail("test@example.com");
                testUser.setPassword(passwordEncoder.encode("Test1234"));
                testUser.setRole(UserRole.ROLE_JOB_SEEKER);
                testUser.setEmailVerified(true);
                userRepository.save(testUser);
                System.out.println("Created test user: test@example.com / Test1234");
            }
        };
    }
}
