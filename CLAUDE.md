# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.0 security check-in application for managing security guards and their check-in records at various work sites. The application supports dual authentication: traditional admin login and WeChat Mini Program integration for security guards.

## Build and Development Commands

### Build and Run
```bash
# Build the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run

# Build JAR package
./mvnw clean package

# Run tests
./mvnw test
```

### Database Setup
- Requires MySQL 8.0+ running on localhost:3306
- Database name: `security_db`
- Uses JPA with `hibernate.ddl-auto=update` for schema management

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
- `FaceRecognitionService`: Integrates with external face recognition API at localhost:8000

**Security Architecture**:
- JWT-based stateless authentication
- Role-based access control (Admin vs SuperAdmin)
- CORS configured for localhost:5173 (frontend development)
- Public endpoints: `/api/login`, `/api/wechat-*` endpoints
- All other endpoints require JWT authentication

### API Integration Points

**Face Recognition Service**: External service at `http://localhost:8000/recognize` for biometric verification

**WeChat Mini Program**: Uses weixin-java-miniapp SDK for:
- Code-to-session exchange
- User authentication
- Token refresh mechanisms

### Key Configuration

**JWT Configuration**:
- Secret: Configured in application.properties
- Expiration: 1 hour (3600000ms)
- Used for both admin and WeChat user sessions

**File Upload**: Configured for 5MB max file/request size for face recognition images

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

### Face Recognition (`/api`)
- `POST /face_recognition` - Upload face image for biometric verification

### Admin Management (`/api/admin`)
- `POST /` - Create new admin account
- `GET /` - List all admin accounts
- `DELETE /{id}` - Delete admin account

## Data Validation & Error Handling

### Common Validation Rules
- SecurityGuard creation requires valid site.id (non-null WorkSite reference)
- CheckinRecord requires both guard and site references
- Foreign key constraints prevent orphaned records

### Error Responses
- Missing site information: "单位信息不能为空"
- Site not found: "没有找到单位" 
- Guard not found: Returns 404 Not Found
- Validation failures return 400 Bad Request with Chinese error messages

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
└── util/           # Configuration and utility classes
```

### Security Configuration
- `SecurityConfig`: Main security configuration with JWT filter chain
- `JwtFilter`: Custom filter for JWT token validation
- `JwtUtil`: JWT token generation and validation utilities

### Database Relationships
- SecurityGuard -> WorkSite (Many-to-One, nullable)
- CheckinRecord -> SecurityGuard (Many-to-One, required)
- CheckinRecord -> WorkSite (Many-to-One, required)
- All entities use auto-generated Long IDs

**Important**: When deleting entities with foreign key relationships:
- Deleting WorkSite: Must first delete all CheckinRecords and set SecurityGuard.site to null
- Deleting SecurityGuard: Must first delete all CheckinRecords associated with that guard

### Testing & Demo Endpoints
- Standard Spring Boot test structure
- Main test class: `SecuityCheckinApplicationTests`
- Uses embedded test database configuration
- Demo endpoints available at `/demo/*` for testing UI integrations
- Test endpoints at `/api/test/*` for development and debugging

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md
