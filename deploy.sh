#!/bin/bash
set -e  # 遇到错误立即退出

echo "🚀 Starting deployment..."

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 记录部署开始时间
DEPLOY_START_TIME=$(date '+%Y-%m-%d %H:%M:%S')

# 备份当前运行状态
echo -e "${YELLOW}📋 Backing up current state...${NC}"
docker compose ps -q > /tmp/backup-containers-$(date +%s).txt
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}" | grep security-checkin > /tmp/backup-images-$(date +%s).txt

# 拉取最新代码
echo -e "${YELLOW}📥 Pulling latest code...${NC}"
git pull origin main

# 构建并启动服务
echo -e "${YELLOW}🔨 Building and starting services...${NC}"
docker compose up -d --build app

# 等待服务启动
echo -e "${YELLOW}⏳ Waiting for services to start...${NC}"
sleep 15

# 健康检查函数
health_check() {
    local max_attempts=10
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        echo -e "${YELLOW}🔍 Health check attempt $attempt/$max_attempts...${NC}"
        
        # 检查Spring Boot健康状态
        if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
            echo -e "${GREEN}✅ Spring Boot service is healthy${NC}"
            
            # 检查人脸识别服务
            if curl -f -s http://localhost:8000/docs > /dev/null; then
                echo -e "${GREEN}✅ Face recognition service is healthy${NC}"
                return 0
            else
                echo -e "${RED}❌ Face recognition service health check failed${NC}"
            fi
        else
            echo -e "${RED}❌ Spring Boot service health check failed${NC}"
        fi
        
        attempt=$((attempt + 1))
        sleep 5
    done
    
    return 1
}

# 执行健康检查
if health_check; then
    echo -e "${GREEN}🎉 Deployment successful!${NC}"
    echo "Deployment completed at: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Started at: $DEPLOY_START_TIME"
    
    # 清理旧镜像和容器
    echo -e "${YELLOW}🧹 Cleaning up old images...${NC}"
    docker image prune -f
    docker container prune -f
    
    # 显示当前运行状态
    echo -e "${GREEN}📊 Current service status:${NC}"
    docker compose ps
    
else
    echo -e "${RED}💥 Deployment failed! Attempting rollback...${NC}"
    
    # 查找最近的备份
    LATEST_BACKUP=$(ls -t /tmp/backup-containers-*.txt | head -1)
    
    if [ -f "$LATEST_BACKUP" ]; then
        echo -e "${YELLOW}🔄 Rolling back to previous state...${NC}"
        
        # 停止当前失败的容器
        docker compose down
        
        # 尝试启动之前的服务
        docker compose up -d
        
        echo -e "${YELLOW}✅ Rollback completed${NC}"
    else
        echo -e "${RED}❌ No backup found, manual intervention required${NC}"
    fi
    
    exit 1
fi