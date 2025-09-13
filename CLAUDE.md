# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.0 security check-in application for managing security guards and their check-in records at various work sites. The application supports dual authentication: traditional admin login and WeChat Mini Program integration for security guards.

## Build and Development Commands

### Local Development
```bash
# Build the project (uses China mirrors for faster downloads)
./mvnw clean compile

# Run the application locally
./mvnw spring-boot:run

# Build JAR package
./mvnw clean package

# Run tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=CheckinControllerTest

# Skip tests during build
./mvnw package -DskipTests
```

### Docker Development
```bash
# Start all services (MySQL, Redis, Face Recognition, Spring Boot app)
docker compose up -d

# View application logs
docker compose logs -f app

# View all service logs
docker compose logs -f

# Stop all services
docker compose down

# Rebuild and restart main application only
docker compose up -d --build app

# Clean restart (removes volumes)
docker compose down -v && docker compose up -d
```

### Deployment Commands
```bash
# Deploy to production server (handles Git pull, Docker rebuild, health checks)
./deploy.sh

# Test deployment locally
./test-deployment.sh

# Manual Docker deployment
./docker-deploy.sh
```

### Database Setup
- Requires MySQL 8.0+ running on localhost:3306
- Database name: `security_db`
- Uses JPA with `hibernate.ddl-auto=update` for schema management
- Default credentials: root/Wodemimashi123a- (configurable via .env)
- Time zone: Asia/Shanghai

## Architecture Overview

### Core Components

**Entity Layer**: JPA entities representing the domain model
- `SecurityGuard`: Security personnel with auto-generated employee IDs (format: YYYYMMDD-7digits-6random)
- `WorkSite`: Locations where security guards are assigned (GPS coordinates with allowed radius)
- `CheckinRecord`: Individual check-in events with timestamps, status, and biometric data
- `Admin`: System administrators with role-based permissions
- `CheckinStatus`: Enum defining check-in states (SUCCESS, FAILED, PENDING)

**Service Layer**: Business logic implementation
- `WechatLoginService`: Handles WeChat Mini Program authentication flow
- `FaceRecognitionService`: Integrates with external face recognition API

**Security Architecture**:
- JWT-based stateless authentication with 1-hour expiration
- Role-based access control (Admin vs SuperAdmin)
- CORS configured for localhost:5173 (frontend development)
- Public endpoints: `/api/login`, `/api/wechat-*` endpoints
- All other endpoints require JWT authentication

### Microservice Integration

**Face Recognition Service**: External Python service (FastAPI) running on port 8000
- Endpoint: `http://localhost:8000/recognize` (local) or `http://face-recognition:8000/recognize` (Docker)
- Handles biometric verification with Redis caching
- Uses VGGFace2 model for face embedding comparison
- Embedding threshold: 0.8 (configurable)

**WeChat Mini Program Integration**: 
- Uses weixin-java-miniapp SDK v4.7.6
- AppID: `wx830cbb70e245f9ec` (configured in application.properties)
- Supports code-to-session exchange, user authentication, and token refresh

### Docker Architecture

**Multi-service Docker Compose setup**:
- `mysql`: MySQL 8.0 database with persistent volumes
- `redis`: Redis 7 for face recognition caching
- `face-recognition`: Python FastAPI service for biometric verification
- `app`: Spring Boot application (multi-stage Docker build)
- Network: `security-network` with custom subnet (172.20.0.0/16)

**Build Optimization**:
- Multi-stage Dockerfile with dependency caching
- Maven China mirrors (Aliyun) for faster downloads in Chinese deployments
- Docker build cache mounts for Maven dependencies
- Non-root user (spring:1001) for security

### Key Configuration Files

**Maven Settings** (`maven-settings.xml`): Configured with Aliyun mirrors for Chinese deployments
**Environment Variables** (`.env`): Contains database passwords, service URLs, and JVM options
**Docker Compose** (`docker-compose.yml`): Full service orchestration with health checks
**Deployment Scripts**: 
- `deploy.sh`: Production deployment with Git retry mechanisms and rollback
- `docker-deploy.sh`: Docker-only deployment
- `test-deployment.sh`: Local deployment testing

## API Endpoints

### Authentication Endpoints (`/api`)
- `POST /login` - Admin login with username/password
- `POST /wechat-login` - WeChat Mini Program code-to-session login
- `POST /wechat-launch` - WeChat Mini Program launch authentication
- `POST /wechat-refresh-token` - Refresh WeChat authentication token

### Security Guard Management (`/api/guards`)
- `POST /` - Add new security guard (requires site.id in payload)
- `GET /` - List all security guards with site information
- `PUT /{id}` - Update security guard information
- `DELETE /{id}` - Delete security guard (cascades to delete CheckinRecords)

### Work Site Management (`/api/sites`)
- `POST /` - Add new work site
- `GET /` - List all work sites with assigned guards
- `PUT /{id}` - Update work site information
- `DELETE /{id}` - Delete work site (cascades to delete CheckinRecords and unassign guards)

### Check-in Operations (`/api`)
- `POST /checkin/validate` - Validate check-in conditions (location, timing)
- `POST /checkin` - Perform actual check-in with face verification
- `GET /checkin` - Admin view of all check-in records with filtering
- `GET /checkin/my-records` - User's personal check-in history
- `GET /wechat-checkin/records` - WeChat user's check-in history

### Face Recognition (`/api`)
- `POST /face_recognition` - Upload face image for biometric verification (multipart/form-data)
  - Parameters: `faceImage` (MultipartFile), `employeeId` (String)
  - **Note**: This endpoint only accepts POST requests with multipart form data

### Test/Demo Endpoints
- `/demo/*` - UI integration testing endpoints
- `/api/test/*` - Development and debugging endpoints

### Admin Management (`/api/admin`)
- `POST /` - Create new admin account
- `GET /` - List all admin accounts
- `DELETE /{id}` - Delete admin account

## Data Relationships & Constraints

### Database Schema
- **SecurityGuard** -> **WorkSite** (Many-to-One, nullable)
- **CheckinRecord** -> **SecurityGuard** (Many-to-One, required)
- **CheckinRecord** -> **WorkSite** (Many-to-One, required)
- All entities use auto-generated Long IDs

**Critical Deletion Order**:
1. When deleting WorkSite: First delete all CheckinRecords, then set SecurityGuard.site to null
2. When deleting SecurityGuard: First delete all associated CheckinRecords

### Validation Rules
- SecurityGuard creation requires valid site.id (non-null WorkSite reference)
- CheckinRecord requires both guard and site references
- Employee ID auto-generation follows pattern: YYYYMMDD-7digits-6random
- GPS coordinates required for WorkSite with configurable radius

## Development Environment Setup

### Prerequisites
- Java 17 (Eclipse Temurin recommended)
- MySQL 8.0+ running on localhost:3306
- Docker and Docker Compose for containerized development
- Optional: Face recognition service (Python FastAPI) on port 8000

### File Upload Configuration
- Max file size: 5MB (for face recognition images)
- Max request size: 5MB
- Supported formats: Images for face recognition

## Troubleshooting

### Common Issues

**Face Recognition Service**:
- Ensure the endpoint accepts POST requests, not GET
- Verify multipart/form-data content type
- Check service availability on port 8000 (or Docker network)

**Database Connection**:
- Verify MySQL is running and accessible
- Check credentials in application.properties or .env
- Ensure `security_db` database exists

**Docker Deployment**:
- Use `docker compose logs -f app` to view Spring Boot logs
- Check service health with `docker compose ps`
- Port conflicts: Ensure ports 3306 (MySQL), 6379 (Redis), 8000 (Face Recognition), 8080 (Spring Boot) are available

**Git Issues in Deployment**:
- `deploy.sh` includes retry mechanisms for Git operations
- Network timeouts are handled with Git configuration optimizations
- Fallback reset+fetch strategy for connection failures

## Development Notes

### Package Structure
```
com.duhao.security.checkinapp/
├── controller/     # REST API endpoints
├── entity/         # JPA entities
├── repository/     # Spring Data JPA repositories  
├── service/        # Business logic interfaces
├── impl/           # Service implementations
├── dto/            # Data transfer objects
├── util/           # Configuration and utility classes
└── config/         # Security, JWT, and application configuration
```

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md
