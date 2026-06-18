# Job Match Platform - Developer Guide

## Project Structure

```
job-match-platform/
├── backend/                    # Spring Boot backend
│   ├── src/main/java/         # Java source
│   │   ├── api/               # Controllers, DTOs
│   │   ├── domain/            # Entities, repositories
│   │   ├── service/           # Business logic
│   │   └── infrastructure/    # Security, config, external
│   ├── src/main/resources/    # Configs, migrations
│   └── src/test/              # Tests
├── ai-service/                 # Python AI service
│   ├── app.py                 # Flask application
│   ├── fine_tune.py           # Model training
│   └── tests/                 # Test suite
├── frontend/                   # React frontend
├── kubernetes/                 # K8s deployment configs
├── terraform/                  # Infrastructure as code
├── monitoring/                 # Prometheus/Grafana
├── docs/                       # Documentation
└── scripts/                    # Utility scripts
```

## Development Setup

### Prerequisites
- Java 17+
- Python 3.11+
- Node.js 18+
- Docker & Docker Compose
- MySQL 8.0
- IDE: VS Code / IntelliJ

### Backend Development

#### Environment Setup
```bash
# Copy environment template
cp backend/.env.example backend/.env

# Edit with your values
# SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/jobmatch
# SPRING_DATASOURCE_USERNAME=root
# SPRING_DATASOURCE_PASSWORD=password
# JWT_SECRET=your-256-bit-secret
```

#### Running Locally
```bash
cd backend

# Start dependencies
docker compose up -d mysql

# Build and run
./mvnw clean install -DskipTests
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
./mvnw test

# Run with coverage
./mvnw verify
```

### AI Service Development

```bash
cd ai-service

# Create virtual environment
python -m venv venv
source venv/bin/activate  # or venv\Scripts\activate on Windows

# Install dependencies
pip install -r requirements.txt

# Run locally
python app.py

# Fine-tune model
python fine_tune.py

# Run tests
pytest tests/ -v

# Run with coverage
pytest tests/ --cov=app tests/
```

## Coding Standards

### Java (Backend)
- Follow Spring Boot conventions
- Use Lombok for boilerplate
- Write Javadoc for public APIs
- Use DTOs for request/response
- Repository pattern for data access
- Service layer for business logic

### Python (AI Service)
- PEP 8 style guide
- Type hints for functions
- Docstrings for classes/methods
- Encapsulate logic in classes
- Use try/except for error handling

## API Development

### Adding a New Endpoint

1. Create DTO (if needed)
```java
public record CreateJobRequest(
    @NotBlank String title,
    @NotBlank String description,
    String location,
    JobType jobType
) {}
```

2. Create/Update Service
```java
public interface JobService {
    JobResponse createJob(CreateJobRequest request, Long userId);
}
```

3. Create Controller
```java
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        return ResponseEntity.ok(jobService.createJob(request, getUserId()));
    }
}
```

4. Add Tests
```java
@WebMvcTest(JobController.class)
class JobControllerTest {
    @Test
    void createJob_ShouldReturnCreated() {
        // Test implementation
    }
}
```

## Database Migrations

### Creating a Migration
```bash
# Create migration file in backend/src/main/resources/db/migration/
# Format: V{version}__{description}.sql
# Example: V10__add_notifications_table.sql
```

### Migration Best Practices
- Always add IF NOT EXISTS / IF EXISTS
- Use InnoDB engine
- Include rollback instructions
- Add appropriate indexes
- Use utf8mb4 charset

## Testing Strategy

### Unit Tests
- Service layer: Mock repositories
- Controllers: Mock services
- Coverage target: >80%

### Integration Tests
- Test full request flow
- Use H2 in-memory database
- Test with test profile

### Security Tests
- Test authentication
- Test authorization
- Test input validation
- Test rate limiting

## Build & Release

### Versioning
- Follow semantic versioning (MAJOR.MINOR.PATCH)
- Use Git tags for releases

### Release Process
1. Create release branch from develop
2. Run full test suite
3. Update version numbers
4. Create PR to main branch
5. Tag release in Git
6. Deploy to staging
7. Run E2E tests
8. Deploy to production (blue-green)

## Troubleshooting

### Common Issues

#### Database Connection Failed
```bash
# Check MySQL is running
docker compose ps mysql

# Check logs
docker compose logs mysql

# Verify credentials in .env
```

#### Build Fails
```bash
# Clean build
./mvnw clean install

# Skip tests for quick build
./mvnw clean install -DskipTests

# Update dependencies
./mvnw dependency:resolve
```

#### Tests Failing
```bash
# Run specific test
./mvnw test -Dtest=JobServiceImplTest

# See test output
./mvnw test -Dtest=JobServiceImplTest -e

# Run integration tests
./mvnw verify -Pintegration