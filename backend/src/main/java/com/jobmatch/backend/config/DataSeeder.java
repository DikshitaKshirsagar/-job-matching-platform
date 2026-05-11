package com.jobmatch.backend.config;

import com.jobmatch.backend.entity.Application;
import com.jobmatch.backend.entity.ApplicationStatus;
import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.repository.ApplicationRepository;
import com.jobmatch.backend.repository.JobRepository;
import com.jobmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")  // Seed sample data in non-test environments
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        User seeker = userRepository.findByEmail("seeker@example.com")
                .orElseGet(() -> {
                    User user = new User();
                    user.setName("Seeker User");
                    user.setEmail("seeker@example.com");
                    user.setPassword(passwordEncoder.encode("password123"));
                    user.setRole(Role.SEEKER);
                    user.setEmailVerified(true);
                    user.setResumeText("Java Spring Boot React MySQL REST API");
                    user.setResumeFileName("sample-resume.pdf");
                    return userRepository.save(user);
                });

        User recruiter = userRepository.findByEmail("recruiter@example.com")
                .orElseGet(() -> {
                    User user = new User();
                    user.setName("Recruiter User");
                    user.setEmail("recruiter@example.com");
                    user.setPassword(passwordEncoder.encode("password123"));
                    user.setRole(Role.RECRUITER);
                    user.setEmailVerified(true);
                    return userRepository.save(user);
                });

        Job job = jobRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Job newJob = new Job();
                    newJob.setTitle("Java Developer");
                    newJob.setCompany("Acme Tech");
                    newJob.setDescription("Build REST APIs with Java, Spring Boot, JPA, MySQL, and React integrations.");
                    newJob.setSkills("Java, Spring Boot, JPA, MySQL, React");
                    newJob.setLocation("Remote");
                    newJob.setSalary("80000");
                    newJob.setRecruiterId(recruiter.getId());
                    return jobRepository.save(newJob);
                });

        if (!applicationRepository.existsByUserAndJob(seeker, job)) {
            Application application = new Application();
            application.setUser(seeker);
            application.setJob(job);
            application.setMatchScore(92.0);
            application.setStatus(ApplicationStatus.APPLIED);
            applicationRepository.save(application);
            log.info("Seeded sample application data for frontend testing");
        }
    }
}
