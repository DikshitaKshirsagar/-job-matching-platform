# Backend API Testing Guide

## Backend Status ✅
- **Port:** 8090
- **Status:** Running and Operational
- **Database:** H2 In-Memory (auto-initialized)
- **Registration:** Working

---

## Testing the Registration Endpoint

### Using PowerShell (Proper Syntax)

```powershell
$body = @{
    name = "John Doe"
    email = "test@example.com"
    password = "Test1234"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8090/api/v1/auth/register" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body `
    -UseBasicParsing | Select-Object -ExpandProperty Content
```

### Using curl (PowerShell 7+)

```powershell
curl.exe -X POST `
    -H "Content-Type: application/json" `
    -d '{"name":"John Doe","email":"test@example.com","password":"Test1234"}' `
    http://localhost:8090/api/v1/auth/register
```

### Using Invoke-RestMethod (Simpler)

```powershell
$body = @{
    name = "John Doe"
    email = "test@example.com"
    password = "Test1234"
}

Invoke-RestMethod -Uri "http://localhost:8090/api/v1/auth/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body ($body | ConvertTo-Json)
```

---

## Available Endpoints

### Authentication Endpoints
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login user
- `POST /api/v1/auth/verify-email` - Verify email with token
- `POST /api/v1/auth/forgot-password` - Send password reset email
- `POST /api/v1/auth/reset-password` - Reset password with token

### Jobs Endpoints
- `GET /api/v1/jobs` - List all jobs (public)
- `GET /api/v1/jobs/{id}` - Get job details (public)

---

## Password Requirements

Your password must contain:
- ✓ Minimum 8 characters
- ✓ At least one uppercase letter (A-Z)
- ✓ At least one lowercase letter (a-z)
- ✓ At least one digit (0-9)

**Valid Examples:**
- `Test1234`
- `Secure@Pass123`
- `MyPassword99`

**Invalid Examples:**
- `test1234` (no uppercase)
- `TEST1234` (no lowercase)
- `TestTest` (no digit)
- `Tp1` (too short)

---

## Email Verification Flow

1. **Register** → User gets verification email (mocked in development)
2. **Verify Email** → Send verification token to verify endpoint
3. **Login** → Can only login after email verification

### To Verify Email During Development

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8090/api/v1/auth/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body (@{
        name = "John Doe"
        email = "john@example.com"
        password = "Test1234"
    } | ConvertTo-Json)

# Check backend logs for verification token
# Then verify email:
Invoke-RestMethod -Uri "http://localhost:8090/api/v1/auth/verify-email" `
    -Method POST `
    -ContentType "application/json" `
    -Body (@{ token = "VERIFICATION_TOKEN" } | ConvertTo-Json)
```

---

## Accessing H2 Database Console

- **URL:** http://localhost:8090/h2-console
- **JDBC URL:** `jdbc:h2:mem:jobmatchdb`
- **Username:** `sa`
- **Password:** (leave empty)

---

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| "Email already exists" | Use a different email address |
| "Name must be 2-50 characters" | Provide a valid name (2-50 chars) |
| "Password must be 8+ chars..." | Use password meeting requirements |
| "Invalid email format" | Use valid email format (e.g., user@domain.com) |
| CORS error from frontend | Check SecurityConfig.java for allowed origins |

---

## Frontend Integration

When testing from the React frontend (port 60607), ensure:
1. Backend is running on port 8090
2. CORS is configured (already done)
3. API calls use: `http://localhost:8090/api/v1/auth/register`
4. Request includes `Content-Type: application/json`

**Allowed Frontend Ports:** 3000, 3001, 3008, 49941, 5173, 5000, 60607

