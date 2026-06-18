# Job Match Platform - Deployment Guide

## Prerequisites

### Required Tools
- Docker 24+
- Kubernetes 1.28+ (for production)
- Terraform 1.5+ (for infrastructure)
- AWS CLI 2.0+ (for cloud deployment)
- kubectl 1.28+
- Helm 3.0+

### Environment Variables
```bash
# Backend
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/jobmatch
SPRING_DATASOURCE_USERNAME=jobmatch
SPRING_DATASOURCE_PASSWORD=<secure-password>
JWT_SECRET=<256-bit-secret>
AI_SERVICE_URL=http://localhost:5000
AI_SERVICE_API_KEY=<api-key>

# AI Service
DEFAULT_MODEL=all-MiniLM-L6-v2
FINETUNED_MODEL_PATH=/app/models/fine-tuned
PORT=5000
AI_SERVICE_API_KEY=<api-key>
```

## Local Development

### 1. Start Dependencies
```bash
# Start MySQL and Redis
docker compose up -d mysql redis

# Wait for database to be ready
docker compose logs -f mysql
```

### 2. Start Backend
```bash
cd backend
./mvnw clean install -DskipTests
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Start AI Service
```bash
cd ai-service
pip install -r requirements.txt
python app.py
```

## Docker Deployment

### Build Images
```bash
# Backend
docker build -t jobmatch/backend:latest -f backend/Dockerfile backend/

# Frontend (if needed)
docker build -t jobmatch/frontend:latest -f frontend/Dockerfile frontend/

# AI Service
docker build -t jobmatch/ai-service:latest -f ai-service/Dockerfile ai-service/
```

### Run with Docker Compose
```bash
docker compose -f docker-compose.yml up -d
docker compose -f docker-compose.prod.yml up -d  # Production
```

## Kubernetes Deployment

### 1. Create Namespace
```bash
kubectl create namespace job-match
```

### 2. Configure Secrets
```bash
# Database credentials
kubectl create secret generic db-secret \
  --from-literal=url=jdbc:mysql://jobmatch-db:3306/jobmatch \
  --from-literal=username=jobmatch \
  --from-literal=password=<secure-password> \
  -n job-match

# JWT secret
kubectl create secret generic jwt-secret \
  --from-literal=secret=<256-bit-hex-key> \
  -n job-match

# AI Service key
kubectl create secret generic ai-service-secret \
  --from-literal=api-key=<api-key> \
  -n job-match
```

### 3. Deploy Infrastructure
```bash
# Deploy database, cache, and storage
kubectl apply -f kubernetes/infrastructure.yaml -n job-match

# Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=mysql -n job-match --timeout=300s
```

### 4. Deploy Applications
```bash
# Deploy all services
kubectl apply -f kubernetes/deployment.yaml -n job-match
kubectl apply -f kubernetes/service.yaml -n job-match
kubectl apply -f kubernetes/ingress.yaml -n job-match

# Enable auto-scaling
kubectl apply -f kubernetes/hpa.yaml -n job-match
```

### 5. Verify Deployment
```bash
# Check pod status
kubectl get pods -n job-match

# Check services
kubectl get svc -n job-match

# Check ingress
kubectl get ingress -n job-match

# Check HPA
kubectl get hpa -n job-match
```

## Infrastructure as Code (Terraform)

### 1. Initialize
```bash
cd terraform
terraform init
```

### 2. Plan
```bash
terraform plan -var="environment=prod" \
  -var="db_username=admin" \
  -var="db_password=<secure>"
```

### 3. Apply
```bash
terraform apply -var="environment=prod" \
  -var="db_username=admin" \
  -var="db_password=<secure>" \
  -auto-approve
```

## Blue-Green Deployment

### Step 1: Deploy Green
```bash
kubectl scale deployment/job-match-backend-green --replicas=3 -n job-match

# Wait for readiness
kubectl wait --for=condition=ready pod \
  -l app=job-match,tier=backend,release=green \
  -n job-match --timeout=300s
```

### Step 2: Smoke Test
```bash
curl -H "Host: internal.jobmatch.com" http://<green-pod-ip>:8080/actuator/health
```

### Step 3: Switch Traffic
```bash
kubectl patch service job-match-backend-active -n job-match \
  -p '{"spec":{"selector":{"release":"green"}}}'
```

### Step 4: Scale Down Blue
```bash
kubectl scale deployment/job-match-backend --replicas=0 -n job-match
```

### Rollback (if needed)
```bash
kubectl patch service job-match-backend-active -n job-match \
  -p '{"spec":{"selector":{"release":"blue"}}}'
kubectl scale deployment/job-match-backend --replicas=3 -n job-match
```

## Canary Releases

### 5% Canary
```bash
kubectl scale deployment/job-match-backend-canary --replicas=1 -n job-match
# Monitor for 15 minutes
```

### Gradual Rollout
```bash
# 25% traffic
kubectl scale deployment/job-match-backend-canary --replicas=2 -n job-match

# 50% traffic
kubectl scale deployment/job-match-backend-canary --replicas=4 -n job-match

# 100% rollout
kubectl set image deployment/job-match-backend \
  backend=jobmatch/backend:new-version -n job-match
kubectl scale deployment/job-match-backend-canary --replicas=0 -n job-match
```

## Monitoring Setup

### Prometheus
```bash
kubectl apply -f monitoring/prometheus.yml -n monitoring
```

### Grafana
```bash
kubectl apply -f monitoring/grafana/ -n monitoring
```

### ELK Stack
```bash
# Install Elasticsearch
helm repo add elastic https://helm.elastic.co
helm install elasticsearch elastic/elasticsearch -n logging

# Install Logstash
helm install logstash elastic/logstash -n logging \
  -f monitoring/logstash/logstash.yml

# Install Kibana
helm install kibana elastic/kibana -n logging
```

## Backup & Recovery

### Database Backup
```bash
# Manual backup
mysqldump -h <host> -u <user> -p jobmatch > backup-$(date +%Y%m%d).sql

# Automated via cron
0 2 * * * /scripts/backup-database.sh
```

### Recovery
```bash
# Restore from backup
mysql -h <host> -u <user> -p jobmatch < backup-20240101.sql

# Point-in-time recovery
# Use AWS RDS console to restore to specific timestamp