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
- `WorkSite`: Locations where security guards are assigned
- `CheckinRecord`: Individual check-in events with timestamps
- `Admin`: System administrators with role-based permissions

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
- SecurityGuard -> WorkSite (Many-to-One)
- CheckinRecord -> SecurityGuard (Many-to-One)
- All entities use auto-generated Long IDs

### Testing
- Standard Spring Boot test structure
- Main test class: `SecuityCheckinApplicationTests`
- Uses embedded test database configuration

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md
