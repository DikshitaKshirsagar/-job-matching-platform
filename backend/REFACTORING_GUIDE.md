# Complete Backend Refactoring Guide

## Overview
This document outlines the refactoring of the Spring Boot backend to production-grade quality following senior engineer patterns. The refactoring has been partially completed with full infrastructure in place.

## ✅ COMPLETED WORK

### 1. Directory Structure Reorganization
```
src/main/java/com/jobmatch/
├── api/
│   ├── controller/
│   │   ├── AuthController.java ✅
│   │   ├── JobController.java ✅
│   │   └── ApplicationController.java ✅
│   ├── dto/
│   │   ├── request/ ✅ (LoginRequest, RegisterRequest, CreateJobRequest, UpdateJobRequest, ApplyJobRequest)
│   │   └── response/ ✅ (ApiResponse, AuthResponse, UserResponse, JobResponse, ApplicationResponse, SavedJobResponse)
│   └── mapper/ (for future MapStruct usage)
├── domain/
│   ├── entity/ ✅ (User, Job, Application, SavedJob)
│   ├── repository/ ✅ (UserRepository, JobRepository, ApplicationRepository, SavedJobRepository)
│   └── enums/ ✅ (UserRole, ApplicationStatus, JobStatus, JobType)
├── service/
│   ├── *.java ✅ (Service interfaces)
│   └── impl/ ✅ (AuthServiceImpl, JobServiceImpl, ApplicationServiceImpl, SavedJobServiceImpl, UserServiceImpl)
├── infrastructure/
│   ├── security/ ✅ (JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService, SecurityConfig)
│   ├── config/ ✅ (AppConfig, OpenApiConfig)
│   └── external/ (AiServiceClient - placeholder)
├── exception/ ✅ (GlobalExceptionHandler, ResourceNotFoundException, UnauthorizedException, BadRequestException, FileUploadException)
└── util/ (FileValidator, SecurityUtils - for future use)
```

### 2. Entities Refactored (with comprehensive improvements)
✅ **User.java**
- Added JPA Auditing (@CreatedDate, @LastModifiedDate)
- Changed to use UserRole enum instead of String
- Added soft delete support (is_deleted field)
- Proper indexes on frequently queried columns
- Lazy loading for associations
- Fixed UserDetails implementation

✅ **Job.java**
- Added JPA Auditing
- Enum fields (JobStatus, JobType)
- Lazy loading on recruiter association
- Proper indexes (status, recruiter_id, created_at, location)
- BigDecimal for salary fields
- Soft delete support

✅ **Application.java**
- Added JPA Auditing
- Enum for ApplicationStatus
- Lazy loading on associations (applicant, job)
- Unique constraint on (applicant_id, job_id)
- Proper indexes
- Soft delete support

✅ **SavedJob.java**
- Added JPA Auditing
- Lazy loading on associations
- Unique constraint on (user_id, job_id)
- Proper indexes

### 3. Repositories Upgraded
- Added proper query methods with `@Query` annotations
- Soft delete queries (findByIdAndDeletedFalse)
- Complex search queries using JPQL
- Pagination support

### 4. Exception Handling
✅ **GlobalExceptionHandler.java** - Centralized exception handling for:
- ResourceNotFoundException
- UnauthorizedException
- BadRequestException
- BadCredentialsException
- AccessDeniedException
- Validation errors (MethodArgumentNotValidException)
- File upload errors
- Generic exceptions

✅ **Custom Exception Classes**:
- ResourceNotFoundException
- UnauthorizedException
- BadRequestException
- FileUploadException

### 5. API Response Wrapper
✅ **ApiResponse<T>** - Universal response envelope with:
- Generic type support
- Builder pattern
- Timestamp inclusion
- Null field exclusion (@JsonInclude)
- Helper methods (success, error, successMessage)

### 6. DTOs with Validation
✅ **Request DTOs** with Jakarta validation:
- LoginRequest
- RegisterRequest
- CreateJobRequest
- UpdateJobRequest
- ApplyJobRequest

✅ **Response DTOs**:
- AuthResponse
- UserResponse
- JobResponse
- ApplicationResponse
- SavedJobResponse

### 7. Service Layer
✅ **Service Interfaces**:
- AuthService
- JobService
- ApplicationService
- SavedJobService
- UserService

✅ **Service Implementations** (All with @Slf4j logging and @Transactional):
- AuthServiceImpl
- JobServiceImpl
- ApplicationServiceImpl
- SavedJobServiceImpl
- UserServiceImpl

### 8. Security Infrastructure
✅ **JWT Implementation** (Production-grade):
- JwtTokenProvider with HMAC-SHA512
- Secure token generation and validation
- Proper error handling and logging

✅ **Authentication Filter**:
- JwtAuthenticationFilter
- Public endpoint whitelist
- Automatic token extraction and validation

✅ **User Details Service**:
- CustomUserDetailsService
- Proper exception handling
- Logging

✅ **Security Configuration**:
- CORS configuration (localhost:3000)
- Stateless session management
- Endpoint-based authorization
- Proper HTTP method restrictions

### 9. Configuration
✅ **application.yml** - Complete Spring Boot configuration with:
- Database connection pooling (HikariCP)
- JPA Hibernate settings
- Flyway migration configuration
- JWT configuration
- File upload settings
- Logging levels
- Swagger/OpenAPI documentation paths

✅ **application-dev.yml** - Development profile with enhanced debugging

✅ **AppConfig.java** - RestTemplate bean

✅ **OpenApiConfig.java** - Swagger 3.0 configuration with JWT security scheme

✅ **Main Application Class**:
- @EnableJpaAuditing annotation
- .env file loading
- Test data initialization

### 10. Build Configuration
✅ **pom.xml** Updates:
- Added flyway-mysql dependency
- Added build section with Lombok annotation processor
- All critical dependencies already present

---

## 📋 REMAINING WORK

### 1. Controllers (Follow established patterns from AuthController, JobController, ApplicationController)
- [ ] **UserController**
  - GET /api/v1/users/profile
  - PATCH /api/v1/users/profile
  - POST /api/v1/users/upload-resume
  - GET /api/v1/users/dashboard

- [ ] **SavedJobController**
  - POST /api/v1/saved-jobs/{jobId}
  - DELETE /api/v1/saved-jobs/{jobId}
  - GET /api/v1/saved-jobs

### 2. Missing Service Implementations
- [ ] Implement AiMatchingService (integrate with AI service at http://localhost:5000)
- [ ] Create AuthRateLimiter service (rate limiting for auth endpoints)

### 3. Flyway Database Migrations
Create migration files in `src/main/resources/db/migration/`:

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ROLE_JOB_SEEKER','ROLE_RECRUITER','ROLE_ADMIN') NOT NULL,
    resume_text LONGTEXT,
    resume_file_name VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token VARCHAR(255),
    verification_token_expiry TIMESTAMP,
    password_reset_token VARCHAR(255),
    password_reset_token_expiry TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role)
);

-- V2__create_jobs_table.sql
CREATE TABLE jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description LONGTEXT NOT NULL,
    company VARCHAR(150) NOT NULL,
    location VARCHAR(150) NOT NULL,
    job_type ENUM('FULL_TIME','PART_TIME','CONTRACT','INTERNSHIP','REMOTE','HYBRID'),
    status ENUM('ACTIVE','CLOSED','DRAFT','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    salary_min DECIMAL(12,2),
    salary_max DECIMAL(12,2),
    required_skills LONGTEXT,
    recruiter_id BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_recruiter FOREIGN KEY (recruiter_id) REFERENCES users(id),
    INDEX idx_job_status (status),
    INDEX idx_job_recruiter (recruiter_id),
    INDEX idx_job_location (location),
    INDEX idx_job_created_at (created_at)
);

-- V3__create_applications_table.sql
CREATE TABLE applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    status ENUM('PENDING','UNDER_REVIEW','SHORTLISTED','REJECTED','HIRED') NOT NULL DEFAULT 'PENDING',
    cover_letter LONGTEXT,
    match_score DOUBLE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_applicant FOREIGN KEY (applicant_id) REFERENCES users(id),
    CONSTRAINT fk_app_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    UNIQUE KEY uq_applicant_job (applicant_id, job_id),
    INDEX idx_app_applicant (applicant_id),
    INDEX idx_app_job (job_id),
    INDEX idx_app_status (status)
);

-- V4__create_saved_jobs_table.sql
CREATE TABLE saved_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    job_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saved_job_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_saved_job_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    UNIQUE KEY uq_saved_job (user_id, job_id),
    INDEX idx_saved_job_user (user_id),
    INDEX idx_saved_job_job (job_id)
);
```

### 4. Utility Classes (Optional but recommended)
- [ ] **SecurityUtils.java** - Extract current user from JWT
- [ ] **FileValidator.java** - Centralized file validation logic

### 5. Additional Configuration
- [ ] Update SecurityConfig if endpoints need adjustment
- [ ] Add CORS headers for cross-origin preflight requests
- [ ] Configure multipart upload size limits

---

## 🔧 PATTERN EXAMPLES

### Create a New Controller
```java
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Resources", description = "Resource management endpoints")
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    @Operation(summary = "Create resource")
    public ResponseEntity<ApiResponse<ResourceResponse>> create(@Valid @RequestBody CreateResourceRequest request) {
        log.info("Creating resource");
        ResourceResponse response = resourceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource created", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource")
    public ResponseEntity<ApiResponse<ResourceResponse>> getById(@PathVariable Long id) {
        ResourceResponse response = resourceService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### Create a New Service Implementation
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;

    @Override
    @Transactional
    public ResourceResponse create(CreateResourceRequest request) {
        log.info("Creating resource");
        // Implementation
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResponse getById(Long id) {
        log.debug("Fetching resource id: {}", id);
        Resource resource = resourceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
        return toResponse(resource);
    }
}
```

---

## ✅ TESTING CHECKLIST

Before considering the refactoring complete:

- [ ] `mvn clean install` compiles without errors
- [ ] All controllers return `ResponseEntity<ApiResponse<T>>`
- [ ] All `@RequestBody` parameters have `@Valid`
- [ ] No hardcoded passwords or secrets in code
- [ ] All status fields use proper enums (not Strings)
- [ ] All `@ManyToOne` relationships use `FetchType.LAZY`
- [ ] Pagination used on all list endpoints (`Page<T>` instead of `List<T>`)
- [ ] `@Transactional` annotations present on all write methods
- [ ] `@Transactional(readOnly = true)` on all read-only methods
- [ ] All services have `@Slf4j` and proper logging
- [ ] Swagger UI accessible at `/swagger-ui.html`
- [ ] JPA Auditing working (`createdAt` and `updatedAt` fields auto-populated)
- [ ] GlobalExceptionHandler catching all exceptions
- [ ] No try-catch blocks in controllers (delegated to exception handler)
- [ ] API endpoints follow `/api/v1/` versioning scheme

---

## 📦 NEXT STEPS

1. Create the remaining controllers (UserController, SavedJobController)
2. Create Flyway migration files
3. Run `mvn clean install` to verify compilation
4. Test database migrations with `spring.flyway.enabled=true`
5. Test all endpoints with Swagger UI
6. Run integration tests
7. Delete old `backend/com/jobmatch/backend/**` files once migration is complete

---

## 📝 NOTES

- All service methods should use constructor injection with `@RequiredArgsConstructor`
- All exceptions should inherit from RuntimeException or custom exception classes
- All DTOs should use Lombok (`@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`)
- All repositories should extend `JpaRepository` and implement custom query methods as needed
- All timestamps should use `LocalDateTime` (not `Date` or `Timestamp`)
- All boolean flags should use `private boolean deleted` for soft deletes
- All enums should use `@Enumerated(EnumType.STRING)` in entities

This refactoring follows Spring Boot best practices and provides a solid foundation for a production-grade application.
