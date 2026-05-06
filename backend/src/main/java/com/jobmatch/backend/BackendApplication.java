package com.jobmatch.backend;

import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.repository.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "JobMatch Backend API",
                version = "1.0",
                description = "Backend API for job matching, recommendations, and application workflows.",
                contact = @Contact(name = "JobMatch Team")
        )
)
public class BackendApplication {

	public static void main(String[] args) {
		loadDotenv();
		SpringApplication.run(BackendApplication.class, args);
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