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

# Run specific test method
./mvnw test -Dtest=CheckinControllerTest#testCheckinSuccess

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
- `Employee`: Abstract parent class for all employee types (uses SINGLE_TABLE inheritance)
  - Fields: id, name, employeeId, openId, phoneNumber, birthDate, idCardNumber, gender, employmentStatus, originalHireDate, latestHireDate, resignDate
  - Computed field: `age` (calculated from birthDate)
- `SecurityGuard`: Extends Employee, security personnel with guard-specific fields
  - Additional fields: site, role (TEAM_LEADER/TEAM_MEMBER), height
  - Certificate fields: firefightingCertLevel, securityGuardCertLevel, securityCheckCertLevel (1-5 levels, null = no cert)
  - Employee IDs auto-generated (format: YYYYMMDD-7digits-6random)
- `WorkSite`: Locations where security guards are assigned (GPS coordinates with allowed radius)
- `CheckinRecord`: Work sessions (工作片段) with start/end times, status, and spot check statistics
  - Status: ACTIVE (在岗中), COMPLETED (已下岗), TIMEOUT (超时下岗), LEGACY (旧数据)
  - Uses @Version for optimistic locking
- `SpotCheck`: Random spot check records linked to CheckinRecord (1:N relationship)
  - Status: PENDING (待处理), PASSED (已通过), MISSED (超时未响应)
  - TriggerType: AUTOMATIC (系统触发), MANUAL (管理员触发)
- `Admin`: System administrators with role-based permissions
- `WorkStatus`: Enum defining work session states (ACTIVE, COMPLETED, TIMEOUT, LEGACY)
- `SpotCheckStatus`: Enum for spot check states (PENDING, PASSED, MISSED)
- `EmploymentStatus`: Enum for employee status (ACTIVE, PROBATION, SUSPENDED, RESIGNED, RETIRED)
- `Gender`: Enum for gender (MALE, FEMALE)

**Service Layer**: Business logic implementation
- `WechatLoginService`: Handles WeChat Mini Program authentication flow
- `FaceRecognitionService`: Integrates with external face recognition API
- `WorkService`: Handles work session start/end with location validation
  - On clock-out: PENDING spot checks are auto-marked as PASSED (guard is present)
- `SpotCheckService`: Manages spot check creation, completion, and statistics
- `WechatNotificationService`: Sends WeChat subscription messages for spot checks (@Async)
- `DelayedTaskService`: Redis ZSET-based delay queue for scheduled tasks
- `DelayedTaskProcessor`: Scheduler that polls Redis queues and delegates to handler
- `DelayedTaskHandler`: Transactional business logic for delayed tasks (separate class to ensure @Transactional works via Spring AOP proxy)

**Security Architecture**:
- JWT-based stateless authentication with 1-hour expiration
- Role-based access control (Admin vs SuperAdmin)
- CORS configured for: localhost:5173, localhost:3000, duhaosecurity.com (with/without https)
- Public endpoints: `/api/login`, `/api/health`, `/api/wechat-*` endpoints, `/api/test/*`, `/demo/*`
- All other endpoints require JWT authentication
- JWT Filter enforces authentication before Spring Security configuration

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
- `redis`: Redis 7 with multiple databases:
  - DB 0: Face recognition caching
  - DB 1: Delay queue for spot check scheduling (ZSET-based)
- `face-recognition`: Python FastAPI service for biometric verification
- `app`: Spring Boot application (multi-stage Docker build)
- Network: `security-network` with custom subnet (172.20.0.0/16)

**Redis Delay Queues** (DB 1):
- `timeout:session` - Work session timeout queue
- `spotcheck:trigger` - Spot check trigger queue
- `spotcheck:timeout` - Spot check timeout queue
- Value format: `id:version` for optimistic locking

**Build Optimization**:
- Multi-stage Dockerfile with dependency caching
- Maven China mirrors (Aliyun) for faster downloads in Chinese deployments
- Docker base images use Huawei Cloud mirror (`swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/`)
- Docker build cache mounts for Maven dependencies
- Non-root user (spring:1001) for security

### Key Configuration Files

**Maven Settings** (`maven-settings.xml`): Configured with Aliyun mirrors for Chinese deployments
**Environment Variables** (`.env`): Contains database passwords, service URLs, JWT secrets, and JVM options
- **Critical**: JWT_SECRET must be at least 256 bits (32+ characters) to avoid WeakKeyException
- Face recognition service paths configurable via FACE_SERVICE_BUILD_CONTEXT and FACE_SERVICE_MODELS_PATH
- WeChat Mini Program: WECHAT_APPID and WECHAT_SECRET (required for WeChat login)
**Docker Compose** (`docker-compose.yml`): Full service orchestration with health checks and configurable paths
**Deployment Scripts**: 
- `deploy.sh`: Production deployment with 10-retry Git pull mechanism, fails if all attempts fail
- `docker-deploy.sh`: Docker-only deployment
- `test-deployment.sh`: Local deployment testing
**CI/CD Pipeline** (`.github/workflows/deploy.yml`):
- Automated testing with JUnit result publishing (`dorny/test-reporter`)
- JaCoCo code coverage reports uploaded as artifacts
- SSH-based deployment to Tencent Cloud server

## API Endpoints

### Authentication Endpoints (`/api`)
- `GET /health` - Health check endpoint (public, no authentication required)
- `POST /login` - Admin login with username/password
- `POST /wechat-login` - WeChat Mini Program code-to-session login
- `POST /wechat-launch` - WeChat Mini Program launch authentication
- `POST /wechat-refresh-token` - Refresh WeChat authentication token

### Security Guard Management (`/api/guards`)
- `POST /` - Add new security guard (requires site.id in payload)
- `GET /` - List all security guards with pagination
- `PUT /{id}` - Update security guard information
- `DELETE /{id}` - Delete security guard (cascades to delete CheckinRecords)

### Work Site Management (`/api/sites`)
- `POST /` - Add new work site
- `GET /` - List all work sites with pagination (includes locationCount, guardCount, onDutyNow stats)
- `PUT /{id}` - Update work site information
- `DELETE /{id}` - Delete work site (cascades to delete CheckinRecords and unassign guards)
- `GET /{id}/statistics` - Get site statistics (totalGuards, onDutyCount, checkinRate, onDutyGuards list)
- `GET /{id}/guards` - Get guards assigned to this site

**Note**: Site IDs in API responses are pure numbers (Long), not prefixed strings.

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
- `GET /` - List all admin accounts with pagination
- `DELETE /{id}` - Delete admin account

### Work Session Management (`/api/work`) - Guard端
工作片段管理，保安通过小程序使用：
- `POST /start` - 上岗（开始工作片段）
- `POST /end` - 下岗（结束工作片段，需工作满1小时）
- `GET /status` - 获取当前工作状态（含待处理抽查信息）

### Spot Check Management (`/api/spot-check`) - Guard端
随机抽查验证，保安通过小程序使用：
- `GET /pending` - 查询待处理抽查（小程序轮询用）
- `POST /complete` - 完成抽查验证（提交位置+人脸）
- `GET /my-history` - 查询我的抽查历史
- `GET /today-stats` - 查询今日抽查统计

### Spot Check Admin (`/api/admin/spot-check`) - Admin端
抽查管理功能：
- `POST /trigger` - 手动触发抽查（指定保安ID列表）
- `GET /records` - 筛选查询抽查记录
- `DELETE /{id}` - 取消抽查
- `GET /statistics` - 获取抽查统计
- `GET /today` - 获取今日抽查

### Work Admin (`/api/admin/work`) - Admin端
工作记录管理：
- `GET /records` - 筛选查询工作记录
- `GET /active` - 查询当前在岗保安
- `GET /{id}` - 获取工作记录详情

### Report (`/api/admin/report`) - Admin端
报表功能：
- `GET /weekly` - 周报
- `GET /monthly` - 月报
- `GET /custom` - 自定义时间范围报告
- `GET /daily-trend` - 每日趋势
- `GET /overview` - 概览统计

### Pagination (All GET List Endpoints)
All list endpoints support unified pagination with these parameters:
- `page` (default: 1) - Page number (1-based)
- `pageSize` (default: 20) - Items per page
- `sortBy` (default: "id") - Sort field
- `sortOrder` (default: "asc") - Sort direction (asc/desc)

Response format:
```json
{
  "success": true,
  "data": [...],
  "pagination": { "total": 31, "page": 1, "pageSize": 20, "totalPages": 2 }
}
```

See `docs/API_PAGINATION.md` for full documentation.

### Backend Filtering (Guards, Sites, Checkin)
All list endpoints support backend filtering using the `findWithFilters` repository pattern:
- **Guards**: name, siteId, employmentStatus, role, heightMin/Max, firefightingCertMin/Max, securityGuardCertMin/Max, securityCheckCertMin/Max
- **Sites**: name
- **Checkin**: startDate, endDate, status, guardId, siteId

Pattern: `(?N IS NULL OR field = ?N)` for optional filters.
Range filters use `(?N IS NULL OR field >= ?N)` pattern.
See `docs/API_FILTER.md` for full documentation.

## Data Relationships & Constraints

### Database Schema
- Table `employee`: Stores all employee types (uses `employee_type` discriminator column)
  - `employee_type = 'GUARD'` for SecurityGuard
- **Employee/SecurityGuard** -> **WorkSite** (Many-to-One, nullable)
- **CheckinRecord** -> **Employee** (Many-to-One, required, column: guard_id)
- **CheckinRecord** -> **WorkSite** (Many-to-One, required)
- All entities use auto-generated Long IDs

**Critical Deletion Order**:
1. When deleting WorkSite: First delete all CheckinRecords, then set Employee.site to null
2. When deleting Employee/SecurityGuard: First delete all associated CheckinRecords

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
- `deploy.sh` includes 10-retry mechanism for Git pull operations
- Network timeouts are handled with Git configuration optimizations
- Deployment fails if all 10 git pull attempts fail (no fallback to stale code)

**JWT Authentication Issues**:
- WeakKeyException: Ensure JWT_SECRET in .env is at least 256 bits (32+ characters)
- Generate secure key with: `openssl rand -base64 32`

## Testing

### Test Configuration
- Tests use `@ActiveProfiles("test")` annotation
- H2 in-memory database configured in `src/test/resources/application-test.properties`
- Redis auto-configuration disabled in test profile
- Unit tests use Mockito with `@ExtendWith(MockitoExtension.class)`

### Writing Tests
```java
// Unit tests (recommended)
@ExtendWith(MockitoExtension.class)
class YourServiceTest {
    @Mock private SomeRepository repository;
    @InjectMocks private YourService service;
}

// Integration tests (requires Redis - usually disabled)
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires Redis")
class IntegrationTest { }
```

### Running Tests
```bash
./mvnw test                              # Run all tests
./mvnw test -Dtest=WorkServiceTest       # Run specific test class
```
Test results: 57 tests, JaCoCo coverage reports generated in `target/site/jacoco/`

## Key Implementation Details

### Employee ID Generation
Employee IDs are auto-generated via `@PostPersist` in Employee entity:
- Format: `YYYYMMDD-0000001-Abc123` (date-sequence-random)
- Generated after entity is persisted (uses database-assigned ID)
- Applies to all Employee subclasses (SecurityGuard, future employee types)

### Authentication Flow
1. **Admin Login**: POST `/api/login` → returns JWT token
2. **WeChat Login**: POST `/api/wechat-login` with code → exchanges for session, returns JWT
3. **JWT Validation**: JwtFilter intercepts all requests except public endpoints
4. **Token Format**: Bearer token in Authorization header

### Dual Auth System
The app supports two types of users:
- **Admins**: Login via username/password, manage guards and sites
- **Security Guards**: Login via WeChat Mini Program, perform check-ins with face recognition

## Logging

### Configuration
- **Local development**: DEBUG level, outputs to console/terminal
- **Docker/Production**: INFO level, outputs to stdout (view via `docker compose logs`)

```properties
# application.properties (local)
logging.level.com.duhao.security.checkinapp=DEBUG

# application-docker.properties (production)
logging.level.com.duhao.security.checkinapp=INFO
logging.level.root=WARN
```

### Viewing Logs
```bash
# Local: logs appear in terminal

# Docker:
docker compose logs -f app              # Real-time app logs
docker compose logs app --tail 100      # Last 100 lines
docker compose logs app | grep "关键词"  # Search logs
```

No file logging configured by default (stdout only).

## Documentation

All project documentation should be placed in the `docs/` folder:

**Core API Documentation:**
- `API_PAGINATION.md` - Unified pagination format
- `API_FILTER.md` - Backend filtering documentation
- `API_DASHBOARD.md` - Dashboard statistics API
- `DATABASE_SCHEMA.md` - Database schema
- `WORK_SPOTCHECK_FEATURE.md` - Work session and spot check feature documentation

**WeChat Mini Program:**
- `小程序API调用文档.md` - Mini Program API docs
- `微信Token刷新API文档.md` - Token refresh API
- `微信小程序签到记录查询API文档.md` - Checkin records query

**Deployment:**
- `DEPLOYMENT.md` - Production deployment guide
- `DOCKER_DEPLOYMENT.md` - Docker deployment guide

## Production Server

- **Host**: ubuntu@62.234.150.58
- **Project path**: ~/security-checkin-backend
- **Domain**: duhaosecurity.com

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md
