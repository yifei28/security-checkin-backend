#!/bin/bash

# CI/CD部署测试脚本
# 用于验证GitHub Actions CI/CD配置是否正确

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🧪 CI/CD部署测试脚本${NC}"
echo "=================================="

# 检查必需的工具
check_requirements() {
    echo -e "${YELLOW}📋 检查必需工具...${NC}"
    
    if ! command -v git &> /dev/null; then
        echo -e "${RED}❌ Git未安装${NC}"
        exit 1
    fi
    
    if ! command -v ssh &> /dev/null; then
        echo -e "${RED}❌ SSH未安装${NC}"
        exit 1
    fi
    
    if ! command -v curl &> /dev/null; then
        echo -e "${RED}❌ curl未安装${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ 所有必需工具已安装${NC}"
}

# 检查Git配置
check_git_config() {
    echo -e "${YELLOW}🔍 检查Git配置...${NC}"
    
    if [ ! -d ".git" ]; then
        echo -e "${RED}❌ 当前目录不是Git仓库${NC}"
        exit 1
    fi
    
    # 检查是否有远程仓库
    if ! git remote -v | grep -q origin; then
        echo -e "${RED}❌ 没有配置origin远程仓库${NC}"
        exit 1
    fi
    
    # 检查当前分支
    CURRENT_BRANCH=$(git branch --show-current)
    echo -e "${GREEN}✅ 当前分支: ${CURRENT_BRANCH}${NC}"
    
    # 检查是否有未提交的更改
    if [ -n "$(git status --porcelain)" ]; then
        echo -e "${YELLOW}⚠️  有未提交的更改${NC}"
        git status --short
    else
        echo -e "${GREEN}✅ 工作目录干净${NC}"
    fi
}

# 检查GitHub Actions工作流文件
check_github_actions() {
    echo -e "${YELLOW}🔧 检查GitHub Actions配置...${NC}"
    
    WORKFLOW_FILE=".github/workflows/deploy.yml"
    if [ ! -f "$WORKFLOW_FILE" ]; then
        echo -e "${RED}❌ GitHub Actions工作流文件不存在: $WORKFLOW_FILE${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ GitHub Actions工作流文件存在${NC}"
    
    # 检查workflow文件语法（基本检查）
    if grep -q "name:" "$WORKFLOW_FILE" && grep -q "jobs:" "$WORKFLOW_FILE"; then
        echo -e "${GREEN}✅ 工作流文件语法看起来正确${NC}"
    else
        echo -e "${RED}❌ 工作流文件语法可能有问题${NC}"
    fi
}

# 检查部署脚本
check_deploy_script() {
    echo -e "${YELLOW}📜 检查部署脚本...${NC}"
    
    if [ ! -f "deploy.sh" ]; then
        echo -e "${RED}❌ deploy.sh脚本不存在${NC}"
        exit 1
    fi
    
    if [ ! -x "deploy.sh" ]; then
        echo -e "${YELLOW}⚠️  deploy.sh没有执行权限，正在修复...${NC}"
        chmod +x deploy.sh
        echo -e "${GREEN}✅ 已添加执行权限${NC}"
    else
        echo -e "${GREEN}✅ deploy.sh脚本存在且有执行权限${NC}"
    fi
}

# 检查Docker配置
check_docker_config() {
    echo -e "${YELLOW}🐳 检查Docker配置...${NC}"
    
    if [ ! -f "docker-compose.yml" ]; then
        echo -e "${RED}❌ docker-compose.yml不存在${NC}"
        exit 1
    fi
    
    if [ ! -f "Dockerfile" ]; then
        echo -e "${RED}❌ Dockerfile不存在${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Docker配置文件存在${NC}"
}

# 测试SSH连接（可选）
test_ssh_connection() {
    echo -e "${YELLOW}🔐 测试SSH连接（可选）...${NC}"
    
    read -p "是否要测试SSH连接到服务器？(y/N): " -r
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        read -p "请输入服务器地址 (如: ubuntu@duhaosecurity.com): " SERVER_ADDRESS
        read -p "请输入SSH端口 (默认: 22): " SSH_PORT
        SSH_PORT=${SSH_PORT:-22}
        
        echo -e "${YELLOW}正在测试SSH连接到 $SERVER_ADDRESS:$SSH_PORT...${NC}"
        
        if ssh -o ConnectTimeout=10 -o BatchMode=yes -p $SSH_PORT $SERVER_ADDRESS 'echo "SSH连接成功"'; then
            echo -e "${GREEN}✅ SSH连接测试成功${NC}"
        else
            echo -e "${RED}❌ SSH连接测试失败${NC}"
            echo -e "${YELLOW}请检查：${NC}"
            echo "- 服务器地址和端口是否正确"
            echo "- SSH密钥是否正确配置"
            echo "- 服务器是否运行"
            echo "- 防火墙设置"
        fi
    fi
}

# 检查Maven配置
check_maven_config() {
    echo -e "${YELLOW}☕ 检查Maven配置...${NC}"
    
    if [ ! -f "pom.xml" ]; then
        echo -e "${RED}❌ pom.xml不存在${NC}"
        exit 1
    fi
    
    if [ ! -f "mvnw" ]; then
        echo -e "${RED}❌ Maven Wrapper不存在${NC}"
        exit 1
    fi
    
    if [ ! -x "mvnw" ]; then
        echo -e "${YELLOW}⚠️  mvnw没有执行权限，正在修复...${NC}"
        chmod +x mvnw
        echo -e "${GREEN}✅ 已添加执行权限${NC}"
    fi
    
    echo -e "${GREEN}✅ Maven配置正确${NC}"
}

# 模拟测试构建
test_build() {
    echo -e "${YELLOW}🔨 测试构建（可选）...${NC}"
    
    read -p "是否要运行测试构建？这将花费一些时间 (y/N): " -r
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}正在运行测试...${NC}"
        if ./mvnw test; then
            echo -e "${GREEN}✅ 测试通过${NC}"
        else
            echo -e "${RED}❌ 测试失败${NC}"
            return 1
        fi
        
        echo -e "${YELLOW}正在构建应用...${NC}"
        if ./mvnw clean package -DskipTests; then
            echo -e "${GREEN}✅ 构建成功${NC}"
        else
            echo -e "${RED}❌ 构建失败${NC}"
            return 1
        fi
    fi
}

# 显示GitHub Secrets检查清单
show_secrets_checklist() {
    echo -e "${BLUE}🔐 GitHub Secrets配置清单${NC}"
    echo "=================================="
    echo "请确保在GitHub仓库设置中配置了以下Secrets："
    echo ""
    echo -e "${YELLOW}必需的Secrets:${NC}"
    echo "□ SERVER_HOST - 服务器地址 (如: duhaosecurity.com)"
    echo "□ SERVER_USER - SSH用户名 (如: ubuntu)"  
    echo "□ SERVER_SSH_KEY - SSH私钥内容"
    echo "□ SERVER_PORT - SSH端口 (可选, 默认: 22)"
    echo ""
    echo -e "${YELLOW}配置路径:${NC}"
    echo "GitHub仓库 → Settings → Secrets and variables → Actions → New repository secret"
    echo ""
}

# 显示部署后验证步骤
show_verification_steps() {
    echo -e "${BLUE}🔍 部署后验证步骤${NC}"
    echo "=================================="
    echo "1. 检查GitHub Actions执行状态："
    echo "   - 访问仓库的 Actions 标签页"
    echo "   - 查看最新工作流的执行情况"
    echo ""
    echo "2. 验证服务器部署："
    echo "   - 访问 https://duhaosecurity.com"
    echo "   - 检查API健康状态: https://duhaosecurity.com/api/actuator/health"
    echo ""
    echo "3. 检查服务器状态："
    echo "   ssh ubuntu@duhaosecurity.com 'cd ~/secuity-checkin && sudo docker compose ps'"
    echo ""
}

# 主函数
main() {
    check_requirements
    echo ""
    
    check_git_config
    echo ""
    
    check_github_actions
    echo ""
    
    check_deploy_script
    echo ""
    
    check_docker_config
    echo ""
    
    check_maven_config
    echo ""
    
    test_ssh_connection
    echo ""
    
    test_build
    echo ""
    
    show_secrets_checklist
    echo ""
    
    show_verification_steps
    
    echo -e "${GREEN}🎉 部署测试完成！${NC}"
    echo ""
    echo -e "${YELLOW}下一步:${NC}"
    echo "1. 配置GitHub Secrets（如果还没有配置）"
    echo "2. 推送代码到main分支触发部署"
    echo "3. 在GitHub Actions中观察部署过程"
    echo "4. 验证部署结果"
}

# 运行主函数
main "$@"