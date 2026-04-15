package com.jobmatch.backend;

import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.repository.UserRepository;
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
		// ✅ FIXED: removed unused JwtUtil parameter — it was injected but never used
	CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.findByEmail("test@example.com").isEmpty()) {
				User testUser = new User();
				testUser.setName("Test User");
				testUser.setEmail("test@example.com");
				// ✅ FIXED: "test123" fails your own validation (needs uppercase + digit rule met)
				// New password satisfies: 8+ chars, uppercase, lowercase, digit
				testUser.setPassword(passwordEncoder.encode("Test1234"));
				testUser.setRole(Role.SEEKER);
				testUser.setEmailVerified(true);
				userRepository.save(testUser);
				System.out.println("Created test user: test@example.com / Test1234");
			}
		};
	}
}