# Docker Deployment Guide

## Prerequisites

- Docker 20.10+ installed
- Docker Compose 2.0+ installed
- 4GB+ RAM available for containers
- Ports 8080, 3306 available (configurable)

## Quick Start

### 1. Local Development Deployment

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your values
vim .env

# Deploy locally
./docker-deploy.sh
```

### 2. Production Deployment

```bash
# Create production environment file
cp .env.example .env.production

# Edit with production values
vim .env.production

# Deploy to production
./docker-deploy.sh production
```

## Deployment Modes

### Local Mode (Default)
```bash
./docker-deploy.sh                    # Deploy locally
./docker-deploy.sh local restart      # Restart containers
./docker-deploy.sh local stop         # Stop containers
./docker-deploy.sh local logs         # View logs
```

### Production Mode
```bash
./docker-deploy.sh production          # Deploy with production settings
./docker-deploy.sh production restart  # Restart production
./docker-deploy.sh production down     # Remove containers
```

### Remote Server Deployment
```bash
# Add to .env.production:
# REMOTE_HOST=your-server.com
# REMOTE_USER=root
# REMOTE_PATH=/opt/security-checkin

./docker-deploy.sh remote
```

## Configuration

### Environment Variables (.env)

```bash
# Database
MYSQL_ROOT_PASSWORD=secure-password
MYSQL_PORT=3306
DB_USERNAME=root
DB_PASSWORD=secure-password

# Application
APP_PORT=8080
SPRING_PROFILES=docker
JWT_SECRET=your-256-bit-secret-key

# Performance Tuning
JAVA_OPTS=-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC
TOMCAT_MAX_THREADS=200
```

### Docker Architecture

1. **Multi-stage Build**: Optimized image size (~150MB runtime)
2. **Layer Caching**: Dependencies cached separately
3. **Health Checks**: Automatic container health monitoring
4. **Resource Limits**: Memory constraints for stability
5. **Security**: Non-root user execution

## Cloud Server Deployment Steps

### 1. Prepare Server

```bash
# Install Docker (Ubuntu/Debian)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. Transfer Files

```bash
# Clone repository
git clone https://github.com/your-repo/security-checkin.git
cd security-checkin

# Or use deployment script
./docker-deploy.sh remote
```

### 3. Configure Production

```bash
# Create production environment
cp .env.example .env.production

# Edit with production values
nano .env.production

# Set secure passwords
openssl rand -base64 32  # Generate JWT secret
```

### 4. Deploy

```bash
# Build and start
./docker-deploy.sh production

# Verify deployment
curl http://your-server:8080/actuator/health
```

## Database Management

### Backup Database
```bash
docker compose exec -T mysql mysqldump -u root -p$MYSQL_ROOT_PASSWORD security_db > backup.sql
```

### Restore Database
```bash
docker compose exec -T mysql mysql -u root -p$MYSQL_ROOT_PASSWORD security_db < backup.sql
```

### Connect to MySQL
```bash
docker compose exec mysql mysql -u root -p
```

## Monitoring & Logs

### View Application Logs
```bash
docker compose logs -f app
```

### View All Logs
```bash
docker compose logs -f
```

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Container Status
```bash
docker compose ps
docker stats
```

## Troubleshooting

### Container Won't Start
```bash
# Check logs
docker compose logs app

# Verify environment
docker compose config

# Reset everything
docker compose down -v
./docker-deploy.sh
```

### Memory Issues
```bash
# Adjust in docker-compose.yml
deploy:
  resources:
    limits:
      memory: 2G  # Increase if needed
```

### Port Conflicts
```bash
# Change in .env
APP_PORT=8081
MYSQL_PORT=3307
```

### Permission Issues
```bash
# Fix script permissions
chmod +x docker-deploy.sh
chmod +x mvnw
```

## Security Recommendations

1. **Change Default Passwords**: Update all passwords in production
2. **Use HTTPS**: Configure nginx reverse proxy with SSL
3. **Firewall Rules**: Only expose necessary ports
4. **Regular Updates**: Keep Docker images updated
5. **Secrets Management**: Use Docker secrets or external vault

## Scaling

### Horizontal Scaling
```bash
# Scale application instances
docker compose up -d --scale app=3
```

### Load Balancing
Enable nginx configuration in docker-compose.yml for load balancing across multiple app instances.

## Maintenance

### Update Application
```bash
git pull
./docker-deploy.sh production
```

### Clean Up
```bash
# Remove unused images
docker image prune -a

# Remove all data (WARNING: Deletes database)
docker compose down -v
```

## Support

For issues or questions:
1. Check application logs: `docker compose logs app`
2. Verify health: `curl http://localhost:8080/actuator/health`
3. Review this guide's troubleshooting section