# Terraform configuration for Job Match Platform
# Multi-cloud support: AWS, Azure, GCP

terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
  backend "s3" {
    bucket = "jobmatch-terraform-state"
    key    = "jobmatch/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = var.aws_region
}

# VPC
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  version = "5.1.0"

  name = "jobmatch-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["${var.aws_region}a", "${var.aws_region}b", "${var.aws_region}c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway   = true
  enable_vpn_gateway   = false
  enable_dns_hostnames = true

  tags = {
    Environment = var.environment
    Project     = "jobmatch"
  }
}

# EKS Cluster
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "19.15.3"

  cluster_name    = "jobmatch-${var.environment}"
  cluster_version = "1.28"

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  cluster_endpoint_public_access = true

  eks_managed_node_groups = {
    backend = {
      desired_size = 3
      min_size     = 2
      max_size     = 10

      instance_types = ["t3.medium", "t3.large"]
      capacity_type  = "ON_DEMAND"

      labels = {
        role = "backend"
      }
    }

    ai_service = {
      desired_size = 2
      min_size     = 1
      max_size     = 5

      instance_types = ["t3.large", "t3.xlarge"]
      capacity_type  = "ON_DEMAND"

      labels = {
        role = "ai-service"
      }
    }
  }

  tags = {
    Environment = var.environment
    Project     = "jobmatch"
  }
}

# RDS MySQL for primary database
resource "aws_db_instance" "primary" {
  identifier = "jobmatch-db-${var.environment}"

  engine         = "mysql"
  engine_version = "8.0"
  instance_class = "db.t3.medium"

  allocated_storage     = 100
  max_allocated_storage = 500
  storage_type          = "gp3"

  db_name  = "jobmatch"
  username = var.db_username
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.database.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name

  backup_retention_period = var.db_backup_retention_days
  backup_window          = "02:00-03:00"
  maintenance_window     = "sun:04:00-sun:05:00"

  multi_az               = var.environment == "prod" ? true : false
  storage_encrypted      = true
  deletion_protection    = var.environment == "prod" ? true : false

  tags = {
    Environment = var.environment
    Project     = "jobmatch"
  }
}

# Read Replica for read scaling
resource "aws_db_instance" "read_replica" {
  count = var.environment == "prod" ? 2 : 1

  identifier = "jobmatch-db-replica-${count.index}-${var.environment}"

  engine         = "mysql"
  engine_version = "8.0"
  instance_class = "db.t3.medium"

  allocated_storage     = 100
  max_allocated_storage = 500
  storage_type          = "gp3"

  replicate_source_db = aws_db_instance.primary.identifier

  vpc_security_group_ids = [aws_security_group.database.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name

  backup_retention_period = 7

  tags = {
    Environment = var.environment
    Project     = "jobmatch"
    Role        = "read-replica"
  }
}

# ElastiCache Redis for caching
resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "jobmatch-cache-${var.environment}"
  engine               = "redis"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  port                 = 6379

  subnet_group_name = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.cache.id]

  tags = {
    Environment = var.environment
    Project     = "jobmatch"
  }
}

# S3 bucket for resume storage
resource "aws_s3_bucket" "resumes" {
  bucket = "jobmatch-resumes-${var.environment}"

  tags = {
    Environment = var.environment
    Project     = "jobmatch"
  }
}

resource "aws_s3_bucket_versioning" "resumes" {
  bucket = aws_s3_bucket.resumes.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "resumes" {
  bucket = aws_s3_bucket.resumes.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Security groups
resource "aws_security_group" "database" {
  name        = "jobmatch-db-${var.environment}"
  description = "Security group for RDS database"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [module.eks.cluster_security_group_id]
  }

  tags = {
    Environment = var.environment
    Project     = "jobmatch"
  }
}

resource "aws_security_group" "cache" {
  name        = "jobmatch-cache-${var.environment}"
  description = "Security group for ElastiCache"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [module.eks.cluster_security_group_id]
  }

  tags = {
    Environment = var.environment
    Project     = "jobmatch"
  }
}

resource "aws_db_subnet_group" "main" {
  name       = "jobmatch-db-${var.environment}"
  subnet_ids = module.vpc.private_subnets

  tags = {
    Environment = var.environment
    Project     = "jobmatch"
  }
}

resource "aws_elasticache_subnet_group" "main" {
  name       = "jobmatch-cache-${var.environment}"
  subnet_ids = module.vpc.private_subnets
}

# CloudWatch for monitoring
resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "jobmatch-${var.environment}"

  dashboard_body = jsonencode({
    widgets = [
      {
        type = "metric"
        properties = {
          metrics = [
            ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", aws_db_instance.primary.identifier],
            [".", "CPUUtilization", ".", "."],
          ]
          period = 300
          stat   = "Average"
          region = var.aws_region
          title  = "RDS Metrics"
        }
      }
    ]
  })
}

# Route53 for DNS
resource "aws_route53_zone" "main" {
  name = var.domain_name
}

resource "aws_route53_record" "api" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "api.${var.domain_name}"
  type    = "A"

  alias {
    name                   = module.eks.cluster_endpoint
    zone_id               = module.eks.cluster_id
    evaluate_target_health = true
  }
}