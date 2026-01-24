# 工作片段与随机抽查功能文档

## 目录

1. [功能概述](#1-功能概述)
2. [🚀 小程序前端快速开始](#2-小程序前端快速开始)
3. [架构设计](#3-架构设计)
4. [数据模型](#4-数据模型)
5. [API 端点](#5-api-端点)
6. [配置项](#6-配置项)
7. [业务流程](#7-业务流程)
8. [部署说明](#8-部署说明)
9. [小程序集成指南](#9-小程序集成指南)

---

## 1. 功能概述

### 1.1 背景

原系统使用简单的打卡记录模式，无法有效监控保安在岗情况。本次更新引入**工作片段模型**和**随机抽查机制**，实现对保安工作状态的全程监控。

### 1.2 核心功能

| 功能 | 描述 |
|------|------|
| 上岗/下岗 | 保安通过小程序上岗开始工作，下岗结束工作 |
| 随机抽查 | 系统在工作期间随机触发抽查，要求保安90分钟内完成验证 |
| 手动抽查 | 管理员可随时对指定保安触发抽查 |
| 抽查统计 | 记录每个工作片段的抽查次数和通过率 |
| 报表分析 | 提供周报、月报和趋势分析 |

### 1.3 业务规则

- **上岗时间**: 不限制，保安可随时上岗
- **最短工时**: 至少工作1小时才能下岗
- **自动超时**: 上岗后16小时自动超时下岗
- **抽查次数**: 每个工作片段最多3次抽查
- **时间点模式**: 上岗时一次性预约所有抽查时间（见下表）
- **抽查响应**: **90分钟（1.5小时）**内完成验证，超时记为缺勤
- **下岗限制**: 有待处理抽查（PENDING）时不能下岗

| 抽查次序 | 触发时间点 | 随机范围 |
|---------|-----------|---------|
| 第1次 | 1小时mark | 45-75分钟 (±15分钟) |
| 第2次 | 5小时mark | 255-345分钟 (±45分钟) |
| 第3次 | 9小时mark | 495-585分钟 (±45分钟) |

**设计目标**:
- 第1次抽查在上岗后1小时左右，确保保安确实在岗
- 第2次抽查在5小时左右，覆盖工作中段
- 第3次抽查在9小时左右，适配8-12小时不同班次
- 90分钟响应窗口，给保安充足的响应时间

---

## 2. 🚀 小程序前端快速开始

> 前端开发者看这一节就够了！

### 2.1 需要实现的功能清单

| 序号 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| 1 | 上岗功能 | 必须 | 采集位置+人脸，调用上岗API |
| 2 | 下岗功能 | 必须 | 采集位置+人脸，调用下岗API |
| 3 | 工作状态显示 | 必须 | 显示是否在岗、工作时长 |
| 4 | 订阅消息授权 | 必须 | 上岗成功后请求订阅 |
| 5 | 抽查轮询 | 必须 | 每30秒检查是否有待处理抽查 |
| 6 | 抽查响应页面 | 必须 | 倒计时+位置+人脸验证 |
| 7 | 抽查历史 | 可选 | 查看历史抽查记录 |

### 2.2 需要调用的 API

**基础URL**: `https://你的域名` 或 `http://localhost:8080`（开发环境）

| 功能 | 方法 | 端点 | 请求体 |
|------|------|------|--------|
| 上岗 | POST | `/api/work/start` | `{latitude, longitude, faceImageUrl}` |
| 下岗 | POST | `/api/work/end` | `{latitude, longitude, faceImageUrl}` |
| 获取工作状态 | GET | `/api/work/status` | - |
| 查询待处理抽查 | GET | `/api/spot-check/pending` | - |
| 完成抽查验证 | POST | `/api/spot-check/complete` | `{spotCheckId, latitude, longitude, faceImageUrl}` |
| 抽查历史 | GET | `/api/spot-check/my-history` | - |

所有 API 都需要在 Header 中携带 JWT Token：
```
Authorization: Bearer {jwt_token}
```

### 2.3 订阅消息配置

系统支持**3个模板轮换发送**，解决单模板订阅次数限制问题：

| 模板 | 模板ID | 字段 |
|------|--------|------|
| 模板1 | `VUGZV2uhT8XZJs294gTjIKeIgfVwvsCPll075s9PQl4` | phrase1, date2, thing6 |
| 模板2 | `tQrVYA-PLyvpDU9l9bMINfj3emX1_BhvaK0Q2h1AFoE` | thing1, thing2, time5, thing16 |
| 模板3 | `gv4OkSahTcxYlGAXlDzZFReNKvhEpKu2E97HFGXTxmo` | time1, thing2, thing5, thing3 |

```javascript
// 上岗成功后请求订阅消息授权（建议请求全部3个模板）
wx.requestSubscribeMessage({
  tmplIds: [
    'VUGZV2uhT8XZJs294gTjIKeIgfVwvsCPll075s9PQl4',
    'tQrVYA-PLyvpDU9l9bMINfj3emX1_BhvaK0Q2h1AFoE',
    'gv4OkSahTcxYlGAXlDzZFReNKvhEpKu2E97HFGXTxmo'
  ],
  success(res) {
    console.log('订阅结果:', res);
  }
});
```

**注意**: 用户需要对3个模板都授权订阅，系统会轮换使用，每个模板订阅一次可发送一条消息。

### 2.4 核心代码示例

#### 2.4.1 上岗流程

```javascript
async function startWork() {
  // 1. 获取位置
  const location = await wx.getLocation({ type: 'gcj02' });

  // 2. 拍摄人脸照片（调用你的人脸组件）
  const faceImageUrl = await uploadFaceImage();

  // 3. 调用上岗API
  const res = await wx.request({
    url: `${BASE_URL}/api/work/start`,
    method: 'POST',
    header: { 'Authorization': `Bearer ${getToken()}` },
    data: {
      latitude: location.latitude,
      longitude: location.longitude,
      faceImageUrl: faceImageUrl
    }
  });

  if (res.data.success) {
    wx.showToast({ title: '上岗成功', icon: 'success' });

    // 4. 请求订阅消息授权
    wx.requestSubscribeMessage({
      tmplIds: ['VUGZV2uhT8XZJs294gTjIKeIgfVwvsCPll075s9PQl4']
    });

    // 5. 开始轮询抽查
    startSpotCheckPolling();
  } else {
    wx.showToast({ title: res.data.message, icon: 'error' });
  }
}
```

#### 2.4.2 抽查轮询

```javascript
let pollingTimer = null;

function startSpotCheckPolling() {
  // 立即检查一次
  checkPendingSpotCheck();

  // 每30秒轮询一次
  pollingTimer = setInterval(checkPendingSpotCheck, 30000);
}

function stopSpotCheckPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
}

async function checkPendingSpotCheck() {
  try {
    const res = await wx.request({
      url: `${BASE_URL}/api/spot-check/pending`,
      method: 'GET',
      header: { 'Authorization': `Bearer ${getToken()}` }
    });

    if (res.data.success && res.data.data) {
      // 有待处理抽查，跳转到抽查页面
      const spotCheck = res.data.data;
      wx.navigateTo({
        url: `/pages/spot-check/index?id=${spotCheck.id}&deadline=${spotCheck.deadline}&remainingSeconds=${spotCheck.remainingSeconds}`
      });
    }
  } catch (err) {
    console.error('轮询抽查失败:', err);
  }
}
```

#### 2.4.3 抽查响应页面

新建页面 `pages/spot-check/index`：

```javascript
// pages/spot-check/index.js
Page({
  data: {
    spotCheckId: null,
    deadline: null,
    remainingSeconds: 0,
    countdownText: '',
    isExpired: false,
    submitting: false
  },

  timer: null,

  onLoad(options) {
    this.setData({
      spotCheckId: options.id,
      deadline: options.deadline,
      remainingSeconds: parseInt(options.remainingSeconds) || 0
    });
    this.startCountdown();
  },

  startCountdown() {
    this.updateCountdownText();
    this.timer = setInterval(() => {
      let remaining = this.data.remainingSeconds - 1;
      if (remaining <= 0) {
        this.setData({ remainingSeconds: 0, isExpired: true, countdownText: '已超时' });
        clearInterval(this.timer);
        wx.showModal({
          title: '抽查超时',
          content: '您未在规定时间内完成验证',
          showCancel: false,
          success: () => wx.navigateBack()
        });
        return;
      }
      this.setData({ remainingSeconds: remaining });
      this.updateCountdownText();
    }, 1000);
  },

  updateCountdownText() {
    const seconds = this.data.remainingSeconds;
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    this.setData({
      countdownText: `${min}分${sec.toString().padStart(2, '0')}秒`
    });
  },

  async submitVerification() {
    if (this.data.submitting || this.data.isExpired) return;
    this.setData({ submitting: true });

    try {
      // 1. 获取位置
      const location = await wx.getLocation({ type: 'gcj02' });

      // 2. 拍摄人脸
      const faceImageUrl = await this.captureFace();

      // 3. 提交验证
      const res = await wx.request({
        url: `${BASE_URL}/api/spot-check/complete`,
        method: 'POST',
        header: { 'Authorization': `Bearer ${getToken()}` },
        data: {
          spotCheckId: this.data.spotCheckId,
          latitude: location.latitude,
          longitude: location.longitude,
          faceImageUrl: faceImageUrl
        }
      });

      if (res.data.success) {
        wx.showToast({ title: '验证成功', icon: 'success' });
        setTimeout(() => wx.navigateBack(), 1500);
      } else {
        wx.showToast({ title: res.data.message, icon: 'error' });
      }
    } catch (err) {
      wx.showToast({ title: '验证失败', icon: 'error' });
    } finally {
      this.setData({ submitting: false });
    }
  },

  async captureFace() {
    // TODO: 调用你的人脸拍摄组件，返回图片URL
    // 这里需要根据你现有的人脸拍摄逻辑实现
  },

  onUnload() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }
});
```

```html
<!-- pages/spot-check/index.wxml -->
<view class="container">
  <view class="header">
    <text class="title">随机抽查</text>
    <text class="subtitle">请在规定时间内完成验证</text>
  </view>

  <view class="countdown-box {{isExpired ? 'expired' : ''}}">
    <text class="label">剩余时间</text>
    <text class="countdown">{{countdownText}}</text>
  </view>

  <view class="tips">
    <view class="tip-item">📍 确保在工作地点范围内</view>
    <view class="tip-item">📷 拍摄清晰的人脸照片</view>
  </view>

  <button
    class="verify-btn"
    type="primary"
    loading="{{submitting}}"
    disabled="{{isExpired || submitting}}"
    bindtap="submitVerification">
    {{isExpired ? '已超时' : '立即验证'}}
  </button>
</view>
```

```css
/* pages/spot-check/index.wxss */
.container {
  padding: 40rpx;
  min-height: 100vh;
  background: #f5f5f5;
}

.header {
  text-align: center;
  margin-bottom: 60rpx;
}

.title {
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
  display: block;
}

.subtitle {
  font-size: 28rpx;
  color: #666;
  margin-top: 16rpx;
}

.countdown-box {
  background: #fff;
  border-radius: 20rpx;
  padding: 60rpx;
  text-align: center;
  margin-bottom: 40rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.1);
}

.countdown-box.expired {
  background: #ffebee;
}

.countdown-box .label {
  font-size: 28rpx;
  color: #999;
  display: block;
  margin-bottom: 20rpx;
}

.countdown-box .countdown {
  font-size: 72rpx;
  font-weight: bold;
  color: #ff5722;
}

.countdown-box.expired .countdown {
  color: #f44336;
}

.tips {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 60rpx;
}

.tip-item {
  font-size: 28rpx;
  color: #666;
  padding: 16rpx 0;
}

.verify-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  font-size: 34rpx;
  border-radius: 48rpx;
}
```

### 2.5 注意事项

1. **位置权限**: 需要在 `app.json` 中配置位置权限
   ```json
   {
     "permission": {
       "scope.userLocation": {
         "desc": "用于验证您的工作位置"
       }
     }
   }
   ```

2. **轮询时机**:
   - 上岗成功后开始轮询
   - 下岗成功后停止轮询
   - 小程序切到后台时轮询无效，依赖订阅消息

3. **订阅消息**:
   - 每次订阅只能发一条消息
   - 建议每次上岗都请求订阅
   - 用户拒绝后需要引导去设置开启

4. **抽查页面跳转**:
   - 收到订阅消息点击跳转: `pages/spot-check/index?spotCheckId=xxx`
   - 轮询发现抽查跳转: 同上

---

## 3. 架构设计

### 3.1 技术栈

```
┌─────────────────────────────────────────────────────────┐
│                    小程序端                              │
├─────────────────────────────────────────────────────────┤
│                 Spring Boot 3.5.0                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │Controller│  │ Service  │  │Repository│              │
│  └──────────┘  └──────────┘  └──────────┘              │
├─────────────────────────────────────────────────────────┤
│      MySQL 8.0        │       Redis (DB 1)             │
│   (持久化存储)         │     (延迟队列)                 │
└─────────────────────────────────────────────────────────┘
```

### 3.2 延迟队列设计

使用 Redis ZSET 实现事件驱动的延迟队列，避免轮询扫描：

```
Redis ZSET 队列:
┌────────────────────────────────────────────────────────┐
│ timeout:session    - 工作片段超时队列                   │
│ spotcheck:trigger  - 抽查触发队列                       │
│ spotcheck:timeout  - 抽查超时队列                       │
└────────────────────────────────────────────────────────┘

Value 格式: id:version (例: "123:0")
Score: 触发时间戳 (毫秒)
```

### 3.3 乐观锁机制

使用 `@Version` 注解实现乐观锁，防止并发问题：

```java
@Version
private Long version;
```

延迟任务执行时进行**三重校验**：
1. 记录是否存在
2. 版本号是否匹配
3. 状态是否正确

### 3.4 时间点模式抽查算法

使用**时间点模式**，上岗时一次性预约所有抽查时间：

```java
/**
 * 生成指定次序的抽查触发时间
 * @param startTime 上岗时间
 * @param checkIndex 抽查次序 (1, 2, 3)
 * @return 触发时间，如果超出配置范围返回 null
 */
public LocalDateTime generateCheckTime(LocalDateTime startTime, int checkIndex) {
    int[] range = getCheckTimeRange(checkIndex);
    if (range == null) return null;

    // 在范围内随机选择一个时间点
    int randomMinutes = range[0] + (int)(Math.random() * (range[1] - range[0] + 1));
    return startTime.plusMinutes(randomMinutes);
}
```

**时间点配置**:

| 抽查次序 | 时间点 | 最小分钟 | 最大分钟 | 随机范围 |
|---------|--------|---------|---------|---------|
| 第1次 | 1小时mark | 45 | 75 | ±15分钟 |
| 第2次 | 5小时mark | 255 | 345 | ±45分钟 |
| 第3次 | 9小时mark | 495 | 585 | ±45分钟 |

**特点**：
- **预约式调度**：上岗时一次性预约所有抽查，避免间隔模式的累积误差
- **固定时间点**：围绕1h、5h、9h三个mark随机浮动
- **90分钟响应窗口**：给保安充足的响应时间
- **下岗限制**：有PENDING抽查时不能下岗，必须先完成验证

---

## 4. 数据模型

### 4.1 实体关系图

```
┌─────────────────┐       ┌─────────────────┐
│  SecurityGuard  │       │    WorkSite     │
│  (保安)         │       │   (工作地点)     │
└────────┬────────┘       └────────┬────────┘
         │ 1                       │ 1
         │                         │
         │ N                       │ N
┌────────┴─────────────────────────┴────────┐
│              CheckinRecord                 │
│              (工作片段)                     │
│  - startTime (上岗时间)                    │
│  - endTime (下岗时间, nullable)            │
│  - status (ACTIVE/COMPLETED/TIMEOUT)       │
│  - spotCheckTotal (抽查总数)               │
│  - spotCheckPassed (通过数)                │
└────────────────────┬──────────────────────┘
                     │ 1
                     │
                     │ N
         ┌───────────┴───────────┐
         │      SpotCheck        │
         │      (抽查记录)        │
         │  - createdAt          │
         │  - deadline           │
         │  - status (PENDING/   │
         │    PASSED/MISSED)     │
         └───────────────────────┘
```

### 4.2 CheckinRecord (工作片段)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| version | Long | 乐观锁版本号 |
| guard | SecurityGuard | 保安 |
| site | WorkSite | 工作地点 |
| startTime | LocalDateTime | 上岗时间 |
| startLatitude | Double | 上岗纬度 |
| startLongitude | Double | 上岗经度 |
| startFaceImageUrl | String | 上岗人脸照片 |
| endTime | LocalDateTime | 下岗时间 (null=在岗中) |
| endLatitude | Double | 下岗纬度 |
| endLongitude | Double | 下岗经度 |
| endFaceImageUrl | String | 下岗人脸照片 |
| status | WorkStatus | 状态 |
| durationMinutes | Long | 工作时长(分钟) |
| spotCheckTotal | Integer | 抽查总数 |
| spotCheckPassed | Integer | 抽查通过数 |

### 4.3 WorkStatus (工作状态)

| 值 | 显示名 | 说明 |
|------|------|------|
| ACTIVE | 在岗中 | 正在工作 |
| COMPLETED | 已下岗 | 正常下岗 |
| TIMEOUT | 超时下岗 | 16小时自动超时 |
| LEGACY | 旧数据 | 迁移前的历史数据 |

### 4.4 SpotCheck (抽查记录)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| version | Long | 乐观锁版本号 |
| checkinRecord | CheckinRecord | 关联的工作片段 |
| createdAt | LocalDateTime | 创建时间 |
| deadline | LocalDateTime | 截止时间 (创建时间+90分钟) |
| completedAt | LocalDateTime | 完成时间 |
| status | SpotCheckStatus | 状态 |
| triggerType | SpotCheckTriggerType | 触发类型 |
| latitude | Double | 验证位置纬度 |
| longitude | Double | 验证位置经度 |
| faceImageUrl | String | 人脸照片 |
| failReason | String | 失败原因 |

### 4.5 SpotCheckStatus (抽查状态)

| 值 | 显示名 | 说明 |
|------|------|------|
| PENDING | 待处理 | 等待保安响应 |
| PASSED | 已通过 | 验证成功 |
| MISSED | 超时未响应 | 90分钟内未完成 |

### 4.6 SpotCheckTriggerType (触发类型)

| 值 | 显示名 | 说明 |
|------|------|------|
| AUTOMATIC | 自动触发 | 系统随机触发 |
| MANUAL | 手动触发 | 管理员触发 |

---

## 5. API 端点

### 5.1 保安端 - 工作管理

#### 上岗

```http
POST /api/work/start
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "latitude": 31.2304,
  "longitude": 121.4737,
  "faceImageUrl": "https://example.com/face.jpg"
}
```

**响应:**

```json
{
  "success": true,
  "message": "上岗成功",
  "data": {
    "sessionId": 123,
    "startTime": "2024-01-15T09:00:00",
    "status": "在岗中",
    "siteName": "总部大楼"
  }
}
```

**错误情况:**
- 保安不存在
- 已在岗中
- 未分配工作地点
- 位置超出允许范围

#### 下岗

```http
POST /api/work/end
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "latitude": 31.2304,
  "longitude": 121.4737,
  "faceImageUrl": "https://example.com/face.jpg"
}
```

**响应:**

```json
{
  "success": true,
  "message": "下岗成功",
  "data": {
    "sessionId": 123,
    "startTime": "2024-01-15T09:00:00",
    "endTime": "2024-01-15T18:00:00",
    "status": "已下岗",
    "durationMinutes": 540,
    "siteName": "总部大楼"
  }
}
```

**错误情况:**
- 未在岗
- 工作时长不足1小时
- 位置超出允许范围

#### 获取工作状态

```http
GET /api/work/status
Authorization: Bearer {jwt_token}
```

**响应:**

```json
{
  "success": true,
  "data": {
    "isWorking": true,
    "sessionId": 123,
    "startTime": "2024-01-15T09:00:00",
    "activeMinutes": 120,
    "siteName": "总部大楼",
    "pendingSpotCheck": {
      "id": 456,
      "deadline": "2024-01-15T11:15:00",
      "remainingSeconds": 300
    }
  }
}
```

### 5.2 保安端 - 抽查管理

#### 查询待处理抽查

```http
GET /api/spot-check/pending
Authorization: Bearer {jwt_token}
```

**响应 (有待处理抽查):**

```json
{
  "success": true,
  "message": "有待处理抽查",
  "data": {
    "id": 456,
    "createdAt": "2024-01-15T11:00:00",
    "deadline": "2024-01-15T11:15:00",
    "status": "待处理",
    "remainingSeconds": 300
  }
}
```

**响应 (无待处理抽查):**

```json
{
  "success": true,
  "message": "当前无待处理抽查",
  "data": null
}
```

#### 完成抽查验证

```http
POST /api/spot-check/complete
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "spotCheckId": 456,
  "latitude": 31.2304,
  "longitude": 121.4737,
  "faceImageUrl": "https://example.com/face.jpg"
}
```

**响应:**

```json
{
  "success": true,
  "message": "验证成功",
  "data": {
    "id": 456,
    "createdAt": "2024-01-15T11:00:00",
    "deadline": "2024-01-15T11:15:00",
    "completedAt": "2024-01-15T11:05:00",
    "status": "已通过",
    "remainingSeconds": 0
  }
}
```

**错误情况:**
- 抽查记录不存在
- 抽查不属于当前用户
- 抽查已处理
- 抽查已超时
- 位置超出允许范围

#### 查询抽查历史

```http
GET /api/spot-check/my-history?page=1&pageSize=20
Authorization: Bearer {jwt_token}
```

**响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 456,
      "createdAt": "2024-01-15T11:00:00",
      "deadline": "2024-01-15T11:15:00",
      "completedAt": "2024-01-15T11:05:00",
      "status": "已通过",
      "remainingSeconds": 0
    }
  ],
  "pagination": {
    "total": 50,
    "page": 1,
    "pageSize": 20,
    "totalPages": 3
  }
}
```

#### 查询今日统计

```http
GET /api/spot-check/today-stats
Authorization: Bearer {jwt_token}
```

**响应:**

```json
{
  "success": true,
  "data": {
    "todaySpotChecks": 3
  }
}
```

### 5.3 管理端 - 抽查管理

#### 手动触发抽查

```http
POST /api/admin/spot-check/trigger
Authorization: Bearer {admin_jwt_token}
Content-Type: application/json

{
  "guardIds": [1, 2, 3]
}
```

**响应:**

```json
{
  "success": true,
  "message": "成功触发 2 个抽查",
  "data": [
    {
      "id": 789,
      "sessionId": 123,
      "guardId": 1,
      "guardName": "张三",
      "siteId": 1,
      "siteName": "总部大楼",
      "createdAt": "2024-01-15T14:00:00",
      "deadline": "2024-01-15T14:15:00",
      "status": "PENDING",
      "statusName": "待处理",
      "triggerType": "MANUAL"
    }
  ]
}
```

#### 查询抽查记录

```http
GET /api/admin/spot-check/records?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59&status=PASSED&guardId=1&siteId=1&triggerType=AUTOMATIC&page=1&pageSize=20&sortBy=createdAt&sortOrder=desc
Authorization: Bearer {admin_jwt_token}
```

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startDate | DateTime | 否 | 开始时间 |
| endDate | DateTime | 否 | 结束时间 |
| status | String | 否 | 状态: PENDING/PASSED/MISSED |
| guardId | Long | 否 | 保安ID |
| siteId | Long | 否 | 站点ID |
| triggerType | String | 否 | 触发类型: AUTOMATIC/MANUAL |
| page | int | 否 | 页码 (默认1) |
| pageSize | int | 否 | 每页数量 (默认20) |
| sortBy | String | 否 | 排序字段 (默认createdAt) |
| sortOrder | String | 否 | 排序方向: asc/desc |

#### 取消抽查

```http
DELETE /api/admin/spot-check/{id}?reason=测试取消
Authorization: Bearer {admin_jwt_token}
```

#### 获取抽查统计

```http
GET /api/admin/spot-check/statistics?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59&guardId=1&siteId=1
Authorization: Bearer {admin_jwt_token}
```

**响应:**

```json
{
  "success": true,
  "data": {
    "totalCount": 100,
    "completedCount": 85,
    "missedCount": 15,
    "pendingCount": 0,
    "completionRate": 85
  }
}
```

#### 获取今日抽查

```http
GET /api/admin/spot-check/today
Authorization: Bearer {admin_jwt_token}
```

### 5.4 管理端 - 工作记录管理

#### 查询工作记录

```http
GET /api/admin/work/records?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59&status=COMPLETED&guardId=1&siteId=1&page=1&pageSize=20
Authorization: Bearer {admin_jwt_token}
```

**查询参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startDate | DateTime | 否 | 开始时间 |
| endDate | DateTime | 否 | 结束时间 |
| status | String | 否 | 状态: ACTIVE/COMPLETED/TIMEOUT |
| guardId | Long | 否 | 保安ID |
| siteId | Long | 否 | 站点ID |

**响应:**

```json
{
  "success": true,
  "data": [
    {
      "id": 123,
      "guardId": 1,
      "guardName": "张三",
      "siteId": 1,
      "siteName": "总部大楼",
      "startTime": "2024-01-15T09:00:00",
      "endTime": "2024-01-15T18:00:00",
      "status": "COMPLETED",
      "statusName": "已下岗",
      "durationMinutes": 540,
      "spotCheckTotal": 4,
      "spotCheckPassed": 3
    }
  ],
  "pagination": {
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

#### 查询当前在岗保安

```http
GET /api/admin/work/active
Authorization: Bearer {admin_jwt_token}
```

#### 获取工作记录详情

```http
GET /api/admin/work/{id}
Authorization: Bearer {admin_jwt_token}
```

### 5.5 管理端 - 报表

#### 周报

```http
GET /api/admin/report/weekly?weekStart=2024-01-15&guardId=1&siteId=1
Authorization: Bearer {admin_jwt_token}
```

**响应:**

```json
{
  "success": true,
  "data": {
    "work": {
      "totalSessions": 50,
      "completedSessions": 45,
      "timeoutSessions": 5,
      "activeSessions": 0,
      "completionRate": 90
    },
    "spotCheck": {
      "total": 200,
      "passed": 180,
      "missed": 20,
      "pending": 0,
      "passRate": 90
    }
  },
  "period": {
    "type": "weekly",
    "start": "2024-01-15",
    "end": "2024-01-21"
  }
}
```

#### 月报

```http
GET /api/admin/report/monthly?year=2024&month=1&guardId=1&siteId=1
Authorization: Bearer {admin_jwt_token}
```

#### 自定义时间范围报告

```http
GET /api/admin/report/custom?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59&guardId=1&siteId=1
Authorization: Bearer {admin_jwt_token}
```

#### 每日趋势

```http
GET /api/admin/report/daily-trend?days=7&guardId=1&siteId=1
Authorization: Bearer {admin_jwt_token}
```

**响应:**

```json
{
  "success": true,
  "data": [
    {
      "date": "2024-01-15",
      "workSessions": 10,
      "completedSessions": 9,
      "spotCheckTotal": 40,
      "spotCheckPassed": 36,
      "spotCheckPassRate": 90
    }
  ]
}
```

#### 概览统计

```http
GET /api/admin/report/overview
Authorization: Bearer {admin_jwt_token}
```

**响应:**

```json
{
  "success": true,
  "data": {
    "activeGuards": 15,
    "today": {
      "sessions": 20,
      "spotChecks": 50,
      "spotCheckPassed": 45,
      "passRate": 90
    },
    "thisWeek": {
      "sessions": 100,
      "spotChecks": 300,
      "spotCheckPassed": 270,
      "passRate": 90
    },
    "thisMonth": {
      "sessions": 400,
      "spotChecks": 1200,
      "spotCheckPassed": 1080,
      "passRate": 90
    }
  }
}
```

---

## 6. 配置项

### 6.1 application.properties

```properties
# ==================== Redis 配置 ====================
# 使用 DB 1 (人脸识别服务使用 DB 0)
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.database=1
spring.data.redis.timeout=5000ms

# ==================== 抽查配置（时间点模式） ====================
# 每个工作片段最多抽查次数
spotcheck.max-checks-per-session=3

# 时间点模式配置（上岗时一次性预约所有抽查）
# 第1次抽查：1小时mark (±15分钟)
spotcheck.check1-min-minutes=45
spotcheck.check1-max-minutes=75
# 第2次抽查：5小时mark (±45分钟)
spotcheck.check2-min-minutes=255
spotcheck.check2-max-minutes=345
# 第3次抽查：9小时mark (±45分钟)
spotcheck.check3-min-minutes=495
spotcheck.check3-max-minutes=585

# 抽查响应时限（分钟）- 90分钟响应窗口
spotcheck.response-minutes=90

# 工作片段超时时间（小时）
spotcheck.session-timeout-hours=16

# 最短工作时长（小时）
spotcheck.min-work-hours=1

# ==================== 微信通知配置 ====================
# 是否启用微信订阅消息通知
wx.notification.enabled=${WX_NOTIFICATION_ENABLED:true}

# 多模板轮换配置（逗号分隔）- 解决单模板订阅次数限制
wx.notification.spot-check-template-ids=${WX_SPOTCHECK_TEMPLATE_IDS:}

# 点击通知跳转的小程序页面
wx.notification.spot-check-page=pages/spot-check/index

# 小程序环境版本: developer/trial/formal
wx.notification.miniprogram-state=${WX_MINIPROGRAM_STATE:formal}

# 语言
wx.notification.lang=zh_CN
```

### 6.2 docker-compose.yml

```yaml
services:
  app:
    depends_on:
      redis:
        condition: service_healthy
    environment:
      REDIS_HOST: redis
      REDIS_PORT: 6379

  redis:
    image: redis:7-alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5
```

---

## 7. 业务流程

### 7.1 上岗流程

```
┌─────────────────────────────────────────────────────────────────┐
│                         保安上岗流程                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  小程序                     服务端                    Redis      │
│    │                          │                        │        │
│    │── POST /api/work/start ──│                        │        │
│    │   (位置+人脸)             │                        │        │
│    │                          │                        │        │
│    │                     ┌────┴────┐                   │        │
│    │                     │ 验证位置 │                   │        │
│    │                     │ 创建工作片段                 │        │
│    │                     │ 保存到MySQL                 │        │
│    │                     └────┬────┘                   │        │
│    │                          │                        │        │
│    │                          │── 预约16h超时 ─────────│        │
│    │                          │── 预约3次抽查(时间点模式)│       │
│    │                          │   第1次: 45-75分钟后    │        │
│    │                          │   第2次: 255-345分钟后  │        │
│    │                          │   第3次: 495-585分钟后  │        │
│    │                          │                        │        │
│    │◀── 返回工作片段ID ───────│                        │        │
│    │                          │                        │        │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 抽查触发流程

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          系统抽查触发流程                                   │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  DelayedTaskProcessor        Redis          MySQL         微信服务器      │
│        │                       │              │               │          │
│        │── 每秒轮询 ───────────│              │               │          │
│        │                       │              │               │          │
│        │◀─ 返回到期任务 ───────│              │               │          │
│        │   (id:version)        │              │               │          │
│   ┌────┴────┐                  │              │               │          │
│   │ 三重校验 │                  │              │               │          │
│   │ 1.存在?  │◀───────────────────── 查询 ────│               │          │
│   │ 2.版本?  │                  │              │               │          │
│   │ 3.状态?  │                  │              │               │          │
│   └────┬────┘                  │              │               │          │
│        │                       │              │               │          │
│   ┌────┴────┐                  │              │               │          │
│   │创建抽查  │                  │              │               │          │
│   │记录     │──────────────────────── 保存 ───│               │          │
│   └────┬────┘                  │              │               │          │
│        │                       │              │               │          │
│   ┌────┴────┐                  │              │               │          │
│   │发送微信  │                  │              │               │          │
│   │订阅消息  │───────────────────────────────────── POST ────│          │
│   │(异步)   │                  │              │    /cgi-bin/  │          │
│   └────┬────┘                  │              │    message/   │          │
│        │                       │              │    subscribe/ │          │
│        │                       │              │    send       │          │
│        │── 预约抽查超时 ───────│              │               │          │
│        │   (90分钟后)          │              │               │          │
│        │                       │              │               │          │
│        │   (时间点模式：无需再调度下次抽查，已在上岗时预约)              │
│        │                       │              │               │          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 7.3 抽查完成流程

```
┌─────────────────────────────────────────────────────────────────┐
│                       保安完成抽查流程                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  小程序                     服务端                               │
│    │                          │                                 │
│    │── GET /api/spot-check/pending ──│                          │
│    │                          │                                 │
│    │◀─ 返回待处理抽查(含倒计时) ─────│                          │
│    │                          │                                 │
│    │                          │                                 │
│    │   [保安收集位置+拍照]     │                                 │
│    │                          │                                 │
│    │                          │                                 │
│    │── POST /api/spot-check/complete ──│                        │
│    │   (spotCheckId+位置+人脸) │                                 │
│    │                          │                                 │
│    │                     ┌────┴────┐                            │
│    │                     │ 验证位置 │                            │
│    │                     │ 更新状态为PASSED                      │
│    │                     └────┬────┘                            │
│    │                          │                                 │
│    │◀── 返回验证结果 ─────────│                                 │
│    │                          │                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 7.4 下岗流程

```
┌─────────────────────────────────────────────────────────────────┐
│                         保安下岗流程                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  小程序                     服务端                    Redis      │
│    │                          │                        │        │
│    │── POST /api/work/end ────│                        │        │
│    │   (位置+人脸)             │                        │        │
│    │                          │                        │        │
│    │                     ┌────┴────┐                   │        │
│    │                     │ 验证位置 │                   │        │
│    │                     │ 检查工时>=1h                │        │
│    │                     │ 更新工作片段                │        │
│    │                     │ 统计抽查通过率              │        │
│    │                     └────┬────┘                   │        │
│    │                          │                        │        │
│    │                          │── 取消超时预约 ────────│        │
│    │                          │── 取消抽查预约 ────────│        │
│    │                          │                        │        │
│    │                     ┌────┴────┐                   │        │
│    │                     │待处理抽查→MISSED            │        │
│    │                     └────┬────┘                   │        │
│    │                          │                        │        │
│    │◀── 返回工作汇总 ─────────│                        │        │
│    │                          │                        │        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 8. 部署说明

### 8.1 数据库迁移

运行 Flyway 迁移脚本：

```sql
-- V2__refactor_checkin_to_work_session.sql

-- 1. 添加乐观锁版本号
ALTER TABLE checkin_record ADD COLUMN version BIGINT DEFAULT 0;

-- 2. 重命名字段
ALTER TABLE checkin_record RENAME COLUMN timestamp TO start_time;
ALTER TABLE checkin_record RENAME COLUMN latitude TO start_latitude;
ALTER TABLE checkin_record RENAME COLUMN longitude TO start_longitude;
ALTER TABLE checkin_record RENAME COLUMN face_image_url TO start_face_image_url;

-- 3. 添加下岗相关字段
ALTER TABLE checkin_record ADD COLUMN end_time DATETIME NULL;
ALTER TABLE checkin_record ADD COLUMN end_latitude DOUBLE NULL;
ALTER TABLE checkin_record ADD COLUMN end_longitude DOUBLE NULL;
ALTER TABLE checkin_record ADD COLUMN end_face_image_url VARCHAR(500) NULL;

-- 4. 添加统计字段
ALTER TABLE checkin_record ADD COLUMN duration_minutes BIGINT NULL;
ALTER TABLE checkin_record ADD COLUMN spot_check_total INT DEFAULT 0;
ALTER TABLE checkin_record ADD COLUMN spot_check_passed INT DEFAULT 0;

-- 5. 迁移状态值
UPDATE checkin_record SET status = 'LEGACY'
WHERE status IN ('SUCCESS', 'FAILED', 'PENDING');

-- 6. SpotCheck 表添加版本号和工作片段关联
ALTER TABLE spot_check ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE spot_check ADD COLUMN checkin_record_id BIGINT;

-- 7. 添加外键约束
ALTER TABLE spot_check
ADD CONSTRAINT fk_spot_check_checkin_record
FOREIGN KEY (checkin_record_id) REFERENCES checkin_record(id);
```

### 8.2 Redis 配置

确保 Redis 服务可用，并且 `.env` 文件配置正确：

```bash
REDIS_HOST=localhost
REDIS_PORT=6379
```

### 8.3 Docker 部署

```bash
# 重新构建并启动
docker compose down
docker compose up -d --build

# 查看日志
docker compose logs -f app
```

### 8.4 验证部署

```bash
# 检查 Redis 连接
docker compose exec redis redis-cli -n 1 KEYS '*'

# 检查延迟队列
docker compose exec redis redis-cli -n 1 ZRANGE timeout:session 0 -1 WITHSCORES
docker compose exec redis redis-cli -n 1 ZRANGE spotcheck:trigger 0 -1 WITHSCORES
```

---

## 9. 小程序集成指南

### 9.1 通知机制

#### 9.1.1 订阅消息（主要通知方式）

用户上岗时请求订阅消息授权，系统触发抽查时会发送微信订阅消息：

```javascript
// 上岗时请求订阅消息授权
async function requestSubscription() {
  try {
    const res = await wx.requestSubscribeMessage({
      tmplIds: [
        'YOUR_SPOTCHECK_TEMPLATE_ID'  // 抽查通知模板ID
      ]
    });
    console.log('订阅结果:', res);
    // res[templateId] = 'accept' | 'reject' | 'ban'
  } catch (err) {
    console.error('订阅消息请求失败:', err);
  }
}

// 在上岗成功后调用
async function onWorkStart() {
  const res = await wx.request({
    url: '/api/work/start',
    method: 'POST',
    data: { latitude, longitude, faceImageUrl }
  });

  if (res.data.success) {
    // 上岗成功后请求订阅消息
    await requestSubscription();
  }
}
```

**订阅消息说明：**
- 用户勾选"总是保持以上选择"并接受 → 后续自动订阅无弹窗
- 用户勾选"总是保持以上选择"并拒绝 → 永久拒绝，需引导用户在设置中开启
- 每次订阅只能发送一条消息，建议每次上岗都请求订阅

#### 9.1.2 轮询机制（备用方式）

如用户未订阅消息，小程序需在保安在岗期间定期轮询待处理抽查：

```javascript
// 每30秒轮询一次（仅当小程序在前台时有效）
setInterval(async () => {
  if (!isWorking) return;

  const res = await wx.request({
    url: '/api/spot-check/pending',
    header: { Authorization: `Bearer ${token}` }
  });

  if (res.data.success && res.data.data) {
    // 有待处理抽查，跳转到抽查页面
    wx.navigateTo({
      url: '/pages/spot-check/index',
      data: res.data.data
    });
  }
}, 30000);
```

**注意：** 轮询仅在小程序前台时有效，用户退出小程序后无法收到通知。

### 9.2 抽查页面

```javascript
Page({
  data: {
    spotCheck: null,
    remainingSeconds: 0,
    location: null,
    faceImage: null
  },

  onLoad(options) {
    this.setData({ spotCheck: options });
    this.startCountdown();
  },

  startCountdown() {
    const timer = setInterval(() => {
      const remaining = this.data.remainingSeconds - 1;
      if (remaining <= 0) {
        clearInterval(timer);
        wx.showToast({ title: '抽查已超时', icon: 'error' });
        wx.navigateBack();
        return;
      }
      this.setData({ remainingSeconds: remaining });
    }, 1000);
  },

  async getLocation() {
    const location = await wx.getLocation({ type: 'gcj02' });
    this.setData({ location });
  },

  async takeFacePhoto() {
    // 调用人脸识别组件
    const faceImage = await this.selectComponent('#faceCamera').capture();
    this.setData({ faceImage });
  },

  async submit() {
    const { spotCheck, location, faceImage } = this.data;

    const res = await wx.request({
      url: '/api/spot-check/complete',
      method: 'POST',
      header: { Authorization: `Bearer ${token}` },
      data: {
        spotCheckId: spotCheck.id,
        latitude: location.latitude,
        longitude: location.longitude,
        faceImageUrl: faceImage.url
      }
    });

    if (res.data.success) {
      wx.showToast({ title: '验证成功', icon: 'success' });
      wx.navigateBack();
    } else {
      wx.showToast({ title: res.data.message, icon: 'error' });
    }
  }
});
```

### 9.3 订阅消息模板（多模板轮换）

系统支持**3个不同标题的模板轮换发送**，解决单模板订阅次数限制问题。

#### 模板1：打卡提醒

| 模板字段 | 字段类型 | 说明 | 示例值 |
|----------|----------|------|--------|
| phrase1 | phrase | 打卡类型 | 随机抽查 |
| date2 | date | 打卡时间 | 2024-01-15 11:15 |
| thing6 | thing | 打卡内容 | 请在15分钟内完成身份验证 |

#### 模板2：签到提醒

| 模板字段 | 字段类型 | 说明 | 示例值 |
|----------|----------|------|--------|
| thing1 | thing | 活动名称 | 随机抽查-总部大楼 |
| thing2 | thing | 签到方式 | 人脸识别 |
| time5 | time | 时间 | 2024-01-15 11:15 |
| thing16 | thing | 温馨提示 | 请在15分钟内完成验证，辛苦了 |

#### 模板3：检查通知

| 模板字段 | 字段类型 | 说明 | 示例值 |
|----------|----------|------|--------|
| time1 | time | 检查日期 | 2024-01-15 11:15 |
| thing2 | thing | 检查人员 | 张三 |
| thing5 | thing | 检查情况 | 请在15分钟内完成身份验证 |
| thing3 | thing | 检查类型 | 随机抽查 |

**配置步骤：**
1. 登录微信小程序后台 → 功能 → 订阅消息
2. 选择或创建3个**不同标题**的模板（同标题模板无法重复添加）
3. 复制3个模板ID，用逗号分隔配置到 `.env` 文件：
   ```bash
   WX_SPOTCHECK_TEMPLATE_IDS=模板1ID,模板2ID,模板3ID
   ```

**轮换机制**：系统使用 AtomicInteger 计数器在3个模板间循环选择，确保每次发送使用不同模板。

---

## 附录

### A. 新增文件清单

```
src/main/java/com/duhao/security/checkinapp/
├── config/
│   ├── SpotCheckProperties.java          # 抽查配置类
│   └── WechatNotificationProperties.java # 微信通知配置类
├── controller/
│   ├── WorkController.java               # 保安端工作API
│   ├── SpotCheckController.java          # 保安端抽查API
│   ├── WorkAdminController.java          # 管理端工作API
│   ├── SpotCheckAdminController.java     # 管理端抽查API
│   └── ReportController.java             # 报表API
├── dto/
│   ├── WorkStartRequest.java             # 上岗请求
│   ├── WorkEndRequest.java               # 下岗请求
│   ├── WorkResponse.java                 # 工作响应
│   ├── WorkStatusResponse.java           # 工作状态响应
│   ├── SpotCheckCompleteRequest.java     # 完成抽查请求
│   └── SpotCheckResponse.java            # 抽查响应
├── entity/
│   ├── WorkStatus.java                   # 工作状态枚举
│   ├── SpotCheckStatus.java              # 抽查状态枚举 (修改)
│   ├── CheckinRecord.java                # 工作片段实体 (重构)
│   └── SpotCheck.java                    # 抽查实体 (重构)
├── impl/
│   ├── SpotCheckServiceImpl.java         # 抽查服务实现
│   └── WechatNotificationServiceImpl.java # 微信通知服务实现
├── service/
│   ├── WorkService.java                  # 工作服务
│   ├── DelayedTaskService.java           # 延迟任务服务
│   ├── DelayedTaskProcessor.java         # 延迟任务处理器 (含通知调用)
│   ├── SpotCheckService.java             # 抽查服务接口 (已存在)
│   └── WechatNotificationService.java    # 微信通知服务接口
└── repository/
    ├── CheckinRepository.java            # 签到仓库 (修改)
    └── SpotCheckRepository.java          # 抽查仓库 (修改)
```

### B. 删除文件清单

```
src/main/java/com/duhao/security/checkinapp/entity/
└── CheckinStatus.java                    # 已被 WorkStatus 替代
```

### C. 错误码

| 错误信息 | 错误码 | 说明 |
|----------|--------|------|
| 保安不存在 | - | 无效的保安ID |
| 您已在岗中，请先下岗 | - | 重复上岗 |
| 您未分配工作地点，请联系管理员 | - | 保安未关联站点 |
| 位置超出允许范围 | - | GPS 距离超过站点允许半径 |
| 您未在岗，无法下岗 | - | 尝试下岗但没有活跃工作片段 |
| 工作时长不足 | - | 未满足最短工作时长要求 |
| 请先完成抽查验证后再下班 | PENDING_SPOT_CHECK | 有待处理抽查时不能下岗 |
| 抽查记录不存在 | - | 无效的抽查ID |
| 抽查已处理 | - | 抽查状态非 PENDING |
| 抽查已超时 | - | 超过90分钟响应时限 |
| 无效的认证信息 | - | JWT 令牌无效或过期 |

---

## 更新日志

### 2026-01-24: 时间点模式重构

**变更概述**: 将抽查调度从"间隔模式"重构为"时间点模式"

#### 主要变更

1. **调度模式变更**
   - 旧模式：每次抽查完成后动态计算下一次间隔
   - 新模式：上岗时一次性预约所有3次抽查的触发时间

2. **响应窗口延长**
   - 旧配置：15分钟响应窗口
   - 新配置：90分钟（1.5小时）响应窗口

3. **时间点配置**
   | 抽查次序 | 时间点 | 随机范围 |
   |---------|--------|---------|
   | 第1次 | 1小时mark | 45-75分钟 |
   | 第2次 | 5小时mark | 255-345分钟 |
   | 第3次 | 9小时mark | 495-585分钟 |

4. **下岗限制**
   - 新增规则：有PENDING状态抽查时不能下岗
   - 返回错误码 `PENDING_SPOT_CHECK`，包含抽查ID、截止时间、剩余分钟数

#### 修改文件

| 文件 | 变更说明 |
|------|---------|
| `SpotCheckProperties.java` | 重构为时间点配置，新增 `generateCheckTime()` 方法 |
| `SpotCheck.java` | 构造函数支持自定义响应窗口分钟数 |
| `DelayedTaskProcessor.java` | `scheduleForNewSession()` 一次性预约所有抽查 |
| `DelayedTaskHandler.java` | 移除间隔调度逻辑，新增PENDING检查防止重叠 |
| `WorkService.java` | 下岗前检查是否有PENDING抽查 |
| `WorkResponse.java` | 新增 `PENDING_SPOT_CHECK` 错误响应 |
| `SpotCheckRepository.java` | 新增 `existsByCheckinRecordIdAndStatus()` 方法 |
| `application.properties` | 更新为时间点模式配置 |

#### 配置变更

```properties
# 旧配置（已废弃）
spotcheck.stage1-min-interval=90
spotcheck.stage1-max-interval=150
spotcheck.response-minutes=15

# 新配置
spotcheck.check1-min-minutes=45
spotcheck.check1-max-minutes=75
spotcheck.check2-min-minutes=255
spotcheck.check2-max-minutes=345
spotcheck.check3-min-minutes=495
spotcheck.check3-max-minutes=585
spotcheck.response-minutes=90
```

#### API 变更

下岗接口新增错误响应：

```json
{
  "success": false,
  "message": "请先完成抽查验证后再下班",
  "code": "PENDING_SPOT_CHECK",
  "pendingSpotCheck": {
    "spotCheckId": 35,
    "deadline": "2026-01-25T05:49:26",
    "remainingMinutes": 80
  }
}
```

#### 其他修复

- 修复人脸识别服务 Redis 连接缺少密码的问题（`face_service.py`）
