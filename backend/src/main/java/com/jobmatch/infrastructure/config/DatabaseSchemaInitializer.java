package com.jobmatch.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isMySql()) {
            return;
        }

        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(50) NOT NULL");
            jdbcTemplate.update("UPDATE users SET role = 'ROLE_JOB_SEEKER' WHERE role IN ('SEEKER', 'JOB_SEEKER', 'USER')");
            jdbcTemplate.update("UPDATE users SET role = 'ROLE_RECRUITER' WHERE role = 'RECRUITER'");
        } catch (Exception ex) {
            log.warn("Could not apply users.role schema compatibility update: {}", ex.getMessage());
        }
    }

    private boolean isMySql() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
        } catch (Exception ex) {
            log.warn("Could not detect database product: {}", ex.getMessage());
            return false;
        }
    }
}
