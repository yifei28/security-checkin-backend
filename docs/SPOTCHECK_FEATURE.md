# 随机抽查功能设计文档

## 功能概述

在工作时间内随机选择保安进行抽查，要求 **15分钟内** 完成打卡验证（定位+人脸），防止保安打卡后离岗。

---

## 业务流程

```
┌─────────────────────────────────────────────────────────────────┐
│                         系统自动流程                              │
├─────────────────────────────────────────────────────────────────┤
│  每日 00:05                                                      │
│     ↓                                                            │
│  生成当天抽查计划（每人1-3次，随机时间）                            │
│     ↓                                                            │
│  到达抽查时间                                                     │
│     ↓                                                            │
│  创建抽查任务 → 发送微信订阅消息通知                               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         保安端流程                                │
├─────────────────────────────────────────────────────────────────┤
│  收到抽查通知 / 小程序轮询发现待处理抽查                           │
│     ↓                                                            │
│  打开抽查页面，显示倒计时                                         │
│     ↓                                                            │
│  采集位置 + 人脸拍照                                              │
│     ↓                                                            │
│  提交验证                                                         │
│     ↓                                                            │
│  ┌──────────┬──────────┬──────────┐                              │
│  │ 验证成功  │ 验证失败  │ 超时未完成 │                             │
│  │ COMPLETED │  FAILED  │  MISSED  │                              │
│  └──────────┴──────────┴──────────┘                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## 抽查状态

| 状态 | 英文 | 说明 |
|------|------|------|
| 待处理 | `PENDING` | 抽查已创建，等待保安完成 |
| 已完成 | `COMPLETED` | 验证通过 |
| 缺勤 | `MISSED` | 超时未完成 |
| 验证失败 | `FAILED` | 位置或人脸验证失败 |
| 已取消 | `CANCELLED` | 管理员取消 |

---

## API 接口

### 保安端接口

#### 1. 查询待处理抽查（轮询用）

```
GET /api/spot-check/pending
Authorization: Bearer <token>
```

**响应示例（有待处理抽查）**：
```json
{
  "success": true,
  "data": {
    "id": "spotcheck_123",
    "siteName": "上海市浦东新区陆家嘴金融中心",
    "siteLatitude": 31.2397,
    "siteLongitude": 121.4998,
    "allowedRadius": 100,
    "createdAt": "2026-01-01T10:00:00",
    "deadline": "2026-01-01T10:15:00",
    "remainingSeconds": 600,
    "status": "PENDING"
  }
}
```

**响应示例（无待处理抽查）**：
```json
{
  "success": true,
  "data": null
}
```

---

#### 2. 完成抽查验证

```
POST /api/spot-check/complete
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**：
```json
{
  "spotCheckId": "spotcheck_123",
  "latitude": 31.2398,
  "longitude": 121.4999,
  "faceImage": "base64编码的人脸图片..."
}
```

**响应示例（成功）**：
```json
{
  "success": true,
  "data": {
    "id": "spotcheck_123",
    "status": "COMPLETED",
    "completedAt": "2026-01-01T10:05:30",
    "message": "抽查完成"
  }
}
```

**响应示例（失败）**：
```json
{
  "success": false,
  "data": {
    "id": "spotcheck_123",
    "status": "FAILED",
    "reason": "位置验证失败：距离工作地点超过100米"
  }
}
```

---

#### 3. 我的抽查历史

```
GET /api/spot-check/my-history?page=1&pageSize=20
Authorization: Bearer <token>
```

**响应示例**：
```json
{
  "success": true,
  "data": [
    {
      "id": "spotcheck_123",
      "siteName": "陆家嘴金融中心",
      "createdAt": "2026-01-01T10:00:00",
      "completedAt": "2026-01-01T10:05:30",
      "status": "COMPLETED",
      "reason": null
    },
    {
      "id": "spotcheck_122",
      "siteName": "陆家嘴金融中心",
      "createdAt": "2025-12-31T14:00:00",
      "completedAt": null,
      "status": "MISSED",
      "reason": "超时未完成"
    }
  ],
  "pagination": {
    "total": 15,
    "page": 1,
    "pageSize": 20,
    "totalPages": 1
  }
}
```

---

### 管理端接口

#### 1. 手动触发抽查

```
POST /api/admin/spot-check/trigger
Authorization: Bearer <admin_token>
Content-Type: application/json
```

**请求体**：
```json
{
  "guardIds": ["guard_1", "guard_2", "guard_3"]
}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "triggered": 3,
    "message": "已向3名保安发送抽查通知"
  }
}
```

---

#### 2. 查询抽查记录

```
GET /api/admin/spot-check/records?page=1&pageSize=20&status=PENDING&guardId=guard_1&startDate=2026-01-01&endDate=2026-01-01
Authorization: Bearer <admin_token>
```

**筛选参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| status | string | PENDING/COMPLETED/MISSED/FAILED/CANCELLED |
| guardId | string | 保安ID |
| siteId | string | 单位ID |
| startDate | string | 开始日期 YYYY-MM-DD |
| endDate | string | 结束日期 YYYY-MM-DD |
| triggerType | string | AUTOMATIC/MANUAL |

---

#### 3. 今日抽查计划

```
GET /api/admin/spot-check/schedule/today
Authorization: Bearer <admin_token>
```

**响应**：
```json
{
  "success": true,
  "data": {
    "totalScheduled": 45,
    "completed": 12,
    "pending": 8,
    "missed": 2,
    "upcoming": 23,
    "schedule": [
      {
        "guardId": "guard_1",
        "guardName": "张伟",
        "siteName": "陆家嘴金融中心",
        "scheduledTime": "2026-01-01T10:30:00",
        "status": "PENDING"
      }
    ]
  }
}
```

---

#### 4. 取消抽查

```
DELETE /api/admin/spot-check/{id}
Authorization: Bearer <admin_token>
```

---

#### 5. 抽查统计

```
GET /api/admin/spot-check/statistics?startDate=2026-01-01&endDate=2026-01-31
Authorization: Bearer <admin_token>
```

**响应**：
```json
{
  "success": true,
  "data": {
    "total": 450,
    "completed": 380,
    "missed": 50,
    "failed": 15,
    "cancelled": 5,
    "completionRate": 84,
    "byGuard": [
      {
        "guardId": "guard_1",
        "guardName": "张伟",
        "total": 30,
        "completed": 28,
        "missed": 2
      }
    ]
  }
}
```

---

## 小程序端需求

### 1. 轮询机制

- 小程序前台运行时，每 **30秒** 调用 `/api/spot-check/pending`
- 发现待处理抽查时，显示弹窗提醒并跳转抽查页面

### 2. 抽查页面

**页面元素**：
- 倒计时显示（剩余时间）
- 工作地点名称
- 位置采集按钮（显示当前距离）
- 人脸拍照区域
- 提交验证按钮

**交互流程**：
1. 进入页面 → 自动获取位置
2. 显示与工作地点距离
3. 用户拍照人脸
4. 点击提交 → 调用 complete 接口
5. 显示结果（成功/失败）

### 3. 微信订阅消息

**消息模板内容**：
```
抽查通知
工作地点：{{siteName}}
截止时间：{{deadline}}
请在15分钟内完成验证
```

点击消息 → 跳转小程序抽查页面

### 4. 历史记录页面

- 显示抽查历史列表
- 状态标签（成功/缺勤/失败）
- 支持下拉刷新、上拉加载更多

---

## 配置参数

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| 抽查超时时间 | 15分钟 | 从创建到截止的时间 |
| 每日最大抽查次数 | 3次 | 每个保安每天最多被抽查次数 |
| 抽查时段 | 09:30-11:30, 13:30-15:30 | 随机抽查的时间范围 |
| 位置误差范围 | 与工作地点配置一致 | 允许的最大距离 |

---

## 时间线

| 阶段 | 内容 |
|------|------|
| 后端开发 | API 接口实现、定时任务、微信通知 |
| 前端开发 | 抽查页面、轮询机制、历史记录 |
| 联调测试 | 完整流程测试 |

---

## 更新日志

- **2026-01-01**: 初版设计文档
