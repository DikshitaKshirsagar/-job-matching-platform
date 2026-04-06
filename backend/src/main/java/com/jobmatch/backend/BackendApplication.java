package com.jobmatch.backend;

import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.repository.UserRepository;
import com.jobmatch.backend.security.JwtUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
		return args -> {
			// Create test user if not exists
			if (userRepository.findByEmail("test@example.com").isEmpty()) {
				User testUser = new User();
				testUser.setName("Test User");
				testUser.setEmail("test@example.com");
				testUser.setPassword(passwordEncoder.encode("test123"));
				testUser.setRole(Role.SEEKER);
				testUser.setEmailVerified(true);
				userRepository.save(testUser);
				System.out.println("Created test user: test@example.com / test123");
			}
		};
	}
}

