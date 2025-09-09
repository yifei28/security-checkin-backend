#!/bin/bash

# Deploy script for security-checkin application
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Deployment modes
MODE=${1:-local}
ACTION=${2:-deploy}

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Function to check dependencies
check_dependencies() {
    print_step "Checking dependencies..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    # Use docker compose v2 if available, otherwise fallback to docker-compose
    if docker compose version &> /dev/null; then
        DOCKER_COMPOSE="docker compose"
    else
        DOCKER_COMPOSE="docker-compose"
    fi
}

# Function to load environment
load_environment() {
    print_step "Loading environment configuration..."
    
    if [ "$MODE" == "production" ]; then
        ENV_FILE=".env.production"
    else
        ENV_FILE=".env"
    fi
    
    if [ -f "$ENV_FILE" ]; then
        print_info "Loading environment from $ENV_FILE"
        set -a
        source "$ENV_FILE"
        set +a
    elif [ -f ".env.example" ]; then
        print_warning "$ENV_FILE not found. Copying from .env.example..."
        cp .env.example "$ENV_FILE"
        print_warning "Please edit $ENV_FILE with your actual values before deploying to production!"
        if [ "$MODE" == "production" ]; then
            print_error "Cannot deploy to production without proper configuration"
            exit 1
        fi
    else
        print_error "No environment configuration found!"
        exit 1
    fi
}

# Function to create necessary directories
prepare_directories() {
    print_step "Preparing directories..."
    
    # Create init-scripts directory if it doesn't exist
    mkdir -p init-scripts
    
    # Copy SQL files if they exist
    if [ -f "src/main/resources/test-data.sql" ]; then
        cp src/main/resources/test-data.sql init-scripts/
        print_info "Copied test-data.sql to init-scripts/"
    fi
    
    # Create nginx config directory for production
    if [ "$MODE" == "production" ]; then
        mkdir -p nginx/ssl
    fi
}

# Function to build images
build_images() {
    print_step "Building Docker images..."
    
    if [ "$MODE" == "production" ]; then
        # Production build with version tag
        VERSION=${VERSION:-$(date +%Y%m%d-%H%M%S)}
        export VERSION
        print_info "Building production image with version: $VERSION"
        $DOCKER_COMPOSE build --no-cache
    else
        # Development build with cache
        $DOCKER_COMPOSE build
    fi
}

# Function to deploy application
deploy_application() {
    print_step "Deploying application..."
    
    case "$ACTION" in
        deploy)
            print_info "Starting containers..."
            $DOCKER_COMPOSE up -d
            ;;
        restart)
            print_info "Restarting containers..."
            $DOCKER_COMPOSE restart
            ;;
        stop)
            print_info "Stopping containers..."
            $DOCKER_COMPOSE stop
            ;;
        down)
            print_info "Removing containers..."
            $DOCKER_COMPOSE down
            ;;
        logs)
            print_info "Showing logs..."
            $DOCKER_COMPOSE logs -f
            ;;
        *)
            print_error "Unknown action: $ACTION"
            print_info "Valid actions: deploy, restart, stop, down, logs"
            exit 1
            ;;
    esac
}

# Function to wait for services
wait_for_services() {
    if [ "$ACTION" == "deploy" ] || [ "$ACTION" == "restart" ]; then
        print_step "Waiting for services to be healthy..."
        
        # Wait for MySQL
        print_info "Waiting for MySQL to be ready..."
        for i in {1..30}; do
            if $DOCKER_COMPOSE exec -T mysql mysqladmin ping -h localhost -u root -p${MYSQL_ROOT_PASSWORD:-Wodemimashi123a-} &> /dev/null; then
                print_info "MySQL is ready!"
                break
            fi
            echo -n "."
            sleep 2
        done
        
        # Wait for application
        print_info "Waiting for application to be ready..."
        for i in {1..30}; do
            if curl -f http://localhost:${APP_PORT:-8080}/actuator/health &> /dev/null; then
                print_info "Application is ready!"
                break
            fi
            echo -n "."
            sleep 2
        done
    fi
}

# Function to show status
show_status() {
    if [ "$ACTION" == "deploy" ] || [ "$ACTION" == "restart" ]; then
        print_step "Deployment Status"
        print_info "Container status:"
        $DOCKER_COMPOSE ps
        
        echo ""
        print_info "Application URLs:"
        print_info "  - Application: http://localhost:${APP_PORT:-8080}"
        print_info "  - Health Check: http://localhost:${APP_PORT:-8080}/actuator/health"
        print_info "  - MySQL: localhost:${MYSQL_PORT:-3306}"
        
        if [ "$MODE" == "production" ]; then
            print_info "  - Nginx (HTTP): http://localhost"
            print_info "  - Nginx (HTTPS): https://localhost"
        fi
    fi
}

# Function to backup database
backup_database() {
    print_step "Backing up database..."
    BACKUP_DIR="backups"
    mkdir -p "$BACKUP_DIR"
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="$BACKUP_DIR/backup_${TIMESTAMP}.sql"
    
    $DOCKER_COMPOSE exec -T mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD:-Wodemimashi123a-} security_db > "$BACKUP_FILE"
    
    if [ $? -eq 0 ]; then
        print_info "Database backed up to $BACKUP_FILE"
        # Keep only last 7 backups
        ls -t "$BACKUP_DIR"/backup_*.sql | tail -n +8 | xargs -r rm
    else
        print_error "Database backup failed"
    fi
}

# Function for production deployment to remote server
deploy_to_remote() {
    REMOTE_HOST=${REMOTE_HOST:-}
    REMOTE_USER=${REMOTE_USER:-root}
    REMOTE_PATH=${REMOTE_PATH:-/opt/security-checkin}
    
    if [ -z "$REMOTE_HOST" ]; then
        print_error "REMOTE_HOST not set. Please set it in .env.production"
        exit 1
    fi
    
    print_step "Deploying to remote server $REMOTE_HOST..."
    
    # Create deployment package
    print_info "Creating deployment package..."
    tar -czf deploy.tar.gz \
        Dockerfile \
        docker-compose.yml \
        .dockerignore \
        .env.production \
        mvnw \
        .mvn \
        pom.xml \
        src \
        init-scripts \
        nginx
    
    # Transfer to remote server
    print_info "Transferring files to remote server..."
    ssh "$REMOTE_USER@$REMOTE_HOST" "mkdir -p $REMOTE_PATH"
    scp deploy.tar.gz "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH/"
    
    # Deploy on remote server
    print_info "Deploying on remote server..."
    ssh "$REMOTE_USER@$REMOTE_HOST" "cd $REMOTE_PATH && \
        tar -xzf deploy.tar.gz && \
        docker compose -f docker-compose.yml up -d --build"
    
    # Clean up
    rm deploy.tar.gz
    
    print_info "Remote deployment complete!"
}

# Main execution
main() {
    echo "========================================="
    echo "Security Check-in Docker Deployment"
    echo "========================================="
    echo "Mode: $MODE | Action: $ACTION"
    echo ""
    
    check_dependencies
    load_environment
    prepare_directories
    
    if [ "$MODE" == "remote" ]; then
        deploy_to_remote
    else
        if [ "$ACTION" == "deploy" ]; then
            # Backup before deploy in production
            if [ "$MODE" == "production" ] && [ -z "$SKIP_BACKUP" ]; then
                backup_database 2>/dev/null || print_warning "No existing database to backup"
            fi
            build_images
        fi
        
        deploy_application
        wait_for_services
        show_status
    fi
    
    print_info "Operation completed successfully!"
}

# Show usage
if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    echo "Usage: $0 [MODE] [ACTION]"
    echo ""
    echo "MODE:"
    echo "  local       - Local development deployment (default)"
    echo "  production  - Production deployment"
    echo "  remote      - Deploy to remote server"
    echo ""
    echo "ACTION:"
    echo "  deploy      - Build and deploy (default)"
    echo "  restart     - Restart containers"
    echo "  stop        - Stop containers"
    echo "  down        - Remove containers"
    echo "  logs        - Show logs"
    echo ""
    echo "Examples:"
    echo "  $0                    # Local deployment"
    echo "  $0 production         # Production deployment"
    echo "  $0 local restart      # Restart local containers"
    echo "  $0 production logs    # Show production logs"
    echo "  $0 remote             # Deploy to remote server"
    exit 0
fi

# Run main function
main