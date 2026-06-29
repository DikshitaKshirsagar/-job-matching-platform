# CI/CD & Docker Fix Checklist

- [x] Analyzed all workflows, docker-compose, and configuration files
- [x] Fix 1: `docker-compose.yml` - Removed `env_file: ./backend/.env` (all vars already in `environment:` block)
- [x] Fix 2: `ai-service/Dockerfile` - Added `curl` for Docker healthcheck support
- [x] Fix 3: `zap-security-scan.yml` - Complete rewrite: port 8081, corrected health check, fixed API doc path (`/api-docs`), upload-artifact@v4, cleanup never fails with `|| true`
- [x] Fix 4: `deploy.yml` - Updated `docker/build-push-action` from v5 to v6
- [x] Fix 5: `ci.yml` - Modernized for 2026: Maven cache, npm cache, concurrency control, setup-node v4, branch scoping

**All CI/CD, Docker Compose, and GitHub Actions issues are resolved.**