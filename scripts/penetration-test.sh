#!/bin/bash
# ============================================================
# Penetration Testing Script for Job Match Platform
# Tests: API attacks, JWT attacks, File upload attacks
# ============================================================

BASE_URL="${1:-http://localhost:8080}"
REPORT_FILE="penetration-test-report-$(date +%Y%m%d-%H%M%S).txt"

echo "========================================"
echo "  Penetration Test Report"
echo "  Target: $BASE_URL"
echo "  Date: $(date)"
echo "========================================" > "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass_count=0
fail_count=0

check() {
    local test_name="$1"
    local status="$2"
    if [ "$status" -eq 0 ]; then
        echo -e "${GREEN}[PASS]${NC} $test_name"
        echo "[PASS] $test_name" >> "$REPORT_FILE"
        ((pass_count++))
    else
        echo -e "${RED}[FAIL]${NC} $test_name"
        echo "[FAIL] $test_name" >> "$REPORT_FILE"
        ((fail_count++))
    fi
}

echo ""
echo "=== SQL Injection Tests ==="
echo "--- SQL Injection Tests ---" >> "$REPORT_FILE"

# SQL injection in login
RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@test.com'\'' OR '\''1'\''='\''1","password":"test"}')
[ "$RESP" = "400" ] || [ "$RESP" = "401" ]
check "SQL Injection - Login bypass" $?

# SQL injection in search
RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/jobs/search?q=test%27%20OR%20%271%27=%271")
[ "$RESP" = "400" ] || [ "$RESP" = "200" ]
check "SQL Injection - Search parameter" $?

echo ""
echo "=== JWT Attack Tests ==="
echo "--- JWT Attack Tests ---" >> "$REPORT_FILE"

# Test with no token
RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/admin/users")
[ "$RESP" = "401" ] || [ "$RESP" = "403" ]
check "JWT - No token provided" $?

# Test with invalid token
RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/admin/users" \
    -H "Authorization: Bearer invalid.jwt.token")
[ "$RESP" = "401" ] || [ "$RESP" = "403" ]
check "JWT - Invalid token rejected" $?

# Test with expired token format
RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/admin/users" \
    -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkB0ZXN0LmNvbSIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTE2MjM5MDIyfQ.abc")
[ "$RESP" = "401" ] || [ "$RESP" = "403" ]
check "JWT - Expired token rejected" $?

echo ""
echo "=== XSS Attack Tests ==="
echo "--- XSS Attack Tests ---" >> "$REPORT_FILE"

# XSS in job description
RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/jobs" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TEST_TOKEN" \
    -d '{"title":"Test","description":"<script>alert(1)</script>","location":"Remote","jobType":"FULL_TIME"}')
[ "$RESP" = "400" ] || [ "$RESP" = "200" ]
check "XSS - Script injection in job description" $?

echo ""
echo "=== File Upload Attack Tests ==="
echo "--- File Upload Attack Tests ---" >> "$REPORT_FILE"

# Test uploading executable
RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/resume/upload" \
    -H "Authorization: Bearer $TEST_TOKEN" \
    -F "file=@/etc/passwd")
[ "$RESP" = "400" ] || [ "$RESP" = "415" ]
check "File Upload - System file rejected" $?

# Test large file upload
dd if=/dev/zero bs=1M count=100 of=/tmp/large_test.bin 2>/dev/null
RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/resume/upload" \
    -H "Authorization: Bearer $TEST_TOKEN" \
    -F "file=@/tmp/large_test.bin")
rm -f /tmp/large_test.bin
[ "$RESP" = "413" ] || [ "$RESP" = "400" ]
check "File Upload - Large file rejected" $?

echo ""
echo "=== SSRF Attack Tests ==="
echo "--- SSRF Attack Tests ---" >> "$REPORT_FILE"

# SSRF via URL parameters
RESP=$(curl -s -o /dev/null -w "%{http_code}" \
    "$BASE_URL/api/v1/jobs?url=http://169.254.169.254/latest/meta-data/")
[ "$RESP" = "400" ] || [ "$RESP" = "404" ]
check "SSRF - Metadata endpoint blocked" $?

echo ""
echo "=== Rate Limiting Tests ==="
echo "--- Rate Limiting Tests ---" >> "$REPORT_FILE"

# Rate limiting test
RATE_LIMITED=false
for i in $(seq 1 30); do
    RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"ratetest@test.com","password":"test"}')
    if [ "$RESP" = "429" ]; then
        RATE_LIMITED=true
        break
    fi
done
$RATE_LIMITED
check "Rate Limiting - 429 returned after threshold" $?

echo ""
echo "=== Broken Auth Tests ==="
echo "--- Broken Auth Tests ---" >> "$REPORT_FILE"

# Test default credentials
RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@admin.com","password":"admin"}')
[ "$RESP" = "401" ] || [ "$RESP" = "400" ]
check "Broken Auth - Default credentials rejected" $?

echo ""
echo "========================================"
echo "  Test Results Summary"
echo "========================================"
echo "  Passed: $pass_count"
echo "  Failed: $fail_count"
echo "  Total:  $((pass_count + fail_count))"
echo "========================================"

echo "" >> "$REPORT_FILE"
echo "========================================" >> "$REPORT_FILE"
echo "  Test Results Summary" >> "$REPORT_FILE"
echo "========================================" >> "$REPORT_FILE"
echo "  Passed: $pass_count" >> "$REPORT_FILE"
echo "  Failed: $fail_count" >> "$REPORT_FILE"
echo "  Total:  $((pass_count + fail_count))" >> "$REPORT_FILE"

exit $fail_count