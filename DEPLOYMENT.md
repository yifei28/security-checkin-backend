# CI/CD 部署配置指南

## 🚀 概述

这个项目使用 GitHub Actions 实现自动化 CI/CD 流程，支持：
- ✅ 自动测试和构建
- ✅ 自动部署到生产服务器
- ✅ 健康检查和自动回滚
- ✅ Maven 依赖缓存优化
- ✅ Docker 容器管理

## 📋 前置要求

### 1. 服务器准备
确保你的云服务器已安装：
- Docker & Docker Compose
- Git
- curl

### 2. SSH 密钥配置
```bash
# 在本地生成SSH密钥对
ssh-keygen -t ed25519 -a 200 -C "your_email@example.com"

# 将公钥添加到服务器
cat ~/.ssh/id_ed25519.pub | ssh ubuntu@your-server.com 'cat >> ~/.ssh/authorized_keys'

# 复制私钥内容（用于GitHub Secrets）
cat ~/.ssh/id_ed25519
```

## 🔐 GitHub Secrets 配置

在 GitHub 仓库设置中添加以下 Secrets：

### 必需的 Secrets

| 名称 | 描述 | 示例值 |
|------|------|--------|
| `SERVER_HOST` | 服务器IP或域名 | `duhaosecurity.com` |
| `SERVER_USER` | SSH用户名 | `ubuntu` |
| `SERVER_SSH_KEY` | SSH私钥内容 | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `SERVER_PORT` | SSH端口（可选） | `22` |

### 配置步骤

1. **进入 GitHub 仓库设置**
   - 仓库页面 → Settings → Secrets and variables → Actions

2. **添加每个 Secret**
   ```
   Name: SERVER_HOST
   Secret: duhaosecurity.com
   
   Name: SERVER_USER  
   Secret: ubuntu
   
   Name: SERVER_SSH_KEY
   Secret: [粘贴完整的SSH私钥内容，包括头尾]
   
   Name: SERVER_PORT (可选)
   Secret: 22
   ```

## 🔄 CI/CD 流程说明

### 触发条件
- **推送到 main 分支**: 触发完整的 CI/CD 流程
- **Pull Request**: 只运行测试，不部署

### 流程阶段

#### 1. 🧪 测试阶段 (CI)
```yaml
- 检出代码
- 设置 JDK 17
- 缓存 Maven 依赖
- 运行测试: ./mvnw test
- 构建应用: ./mvnw clean package
- 上传构建产物
```

#### 2. 🚀 部署阶段 (CD)
```yaml
- 通过SSH连接服务器
- 拉取最新代码
- 执行部署脚本 deploy.sh
- 健康检查和自动回滚
```

#### 3. 📢 通知阶段
```yaml
- 发送部署状态通知
- 记录流程执行结果
```

## 🛠️ 服务器端配置

### 1. 项目目录结构
```
~/secuity-checkin/                 # 主项目目录
├── .git/                          # Git仓库
├── docker-compose.yml             # Docker编排文件
├── deploy.sh                      # 自动部署脚本
└── src/                           # 源代码

~/facial-recognition-service/      # 人脸识别服务目录
├── Dockerfile
├── models/                        # AI模型文件
└── face_service.py
```

### 2. deploy.sh 脚本功能
- ✅ 自动备份当前服务状态
- ✅ 构建和启动更新的服务
- ✅ 健康检查验证
- ✅ 失败时自动回滚
- ✅ 清理旧的Docker镜像

## 🔧 本地测试

### 测试SSH连接
```bash
# 测试SSH密钥认证
ssh -i ~/.ssh/id_ed25519 ubuntu@duhaosecurity.com

# 测试部署脚本
ssh ubuntu@duhaosecurity.com 'cd ~/secuity-checkin && ./deploy.sh'
```

### 手动部署
```bash
# 如果CI/CD失败，可以手动部署
ssh ubuntu@duhaosecurity.com
cd ~/secuity-checkin
git pull
sudo docker compose up -d --build app
```

## 📊 监控和排错

### 查看部署日志
```bash
# GitHub Actions日志
# 在仓库的 Actions 标签页查看工作流执行情况

# 服务器应用日志  
sudo docker compose logs app -f

# 部署脚本执行记录
# 查看GitHub Actions的部署步骤输出
```

### 常见问题排解

#### 1. SSH连接失败
```bash
# 检查SSH密钥格式
cat ~/.ssh/id_ed25519 | head -1
# 应该是: -----BEGIN OPENSSH PRIVATE KEY-----

# 测试连接
ssh -o ConnectTimeout=10 ubuntu@duhaosecurity.com
```

#### 2. Docker构建失败
```bash
# 服务器上手动检查
cd ~/secuity-checkin
sudo docker compose config --quiet  # 验证配置
sudo docker compose build app       # 手动构建
```

#### 3. 健康检查失败
```bash
# 检查服务状态
sudo docker compose ps
sudo docker compose logs app --tail=50

# 手动健康检查
curl -f http://localhost:8080/actuator/health
```

## 🔄 回滚操作

### 自动回滚
- 部署脚本检测到健康检查失败时自动触发
- 恢复到上一个稳定的容器状态

### 手动回滚
```bash
# 回滚到指定Git提交
ssh ubuntu@duhaosecurity.com
cd ~/secuity-checkin
git log --oneline -5              # 查看最近提交
git reset --hard <commit-id>      # 回滚到指定版本
sudo docker compose up -d --build app
```

## 🚨 安全注意事项

1. **SSH密钥管理**
   - 使用ED25519密钥（更安全）
   - 定期轮换SSH密钥
   - 不要在代码中硬编码密钥

2. **GitHub Secrets**
   - 只添加必需的Secrets
   - 定期审查Secret使用情况
   - 使用最小权限原则

3. **服务器安全**
   - 定期更新系统和Docker
   - 配置防火墙规则
   - 监控异常访问

## 📈 性能优化

### 1. 构建缓存
- Maven依赖自动缓存
- Docker镜像层缓存
- 避免重复下载

### 2. 部署优化
- 增量更新策略
- 健康检查超时设置
- 自动清理旧镜像

### 3. 监控指标
- 部署成功率
- 构建时间
- 健康检查响应时间

---

## 🎯 快速开始

1. **配置GitHub Secrets** (一次性)
2. **推送代码到main分支**  
3. **在GitHub Actions中查看部署进度**
4. **访问 https://duhaosecurity.com 验证部署**

就这么简单！🎉