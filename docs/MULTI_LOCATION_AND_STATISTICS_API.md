# 单位多签到地点 & 统计API 文档

> 版本：1.1
> 更新日期：2026-01-08

## 功能概述

本次更新实现了两个主要功能：

1. **多签到地点支持**：每个单位(WorkSite)可配置多个签到地点(CheckinLocation)，每个地点有独立的坐标和签到半径
2. **单位统计API**：独立的API返回单位的保安数量、签到统计、保安列表等信息

---

## 一、签到地点管理 API

### 1.1 获取签到地点列表

**GET** `/api/sites/{siteId}/locations`

**请求头**
```
Authorization: Bearer {token}
```

**响应示例**
```json
{
  "success": true,
  "count": 2,
  "data": [
    {
      "id": 1,
      "name": "东门",
      "latitude": 39.922,
      "longitude": 116.456,
      "allowedRadius": 100.0
    },
    {
      "id": 2,
      "name": "西门",
      "latitude": 39.921,
      "longitude": 116.454,
      "allowedRadius": 80.0
    }
  ]
}
```

### 1.2 添加签到地点

**POST** `/api/sites/{siteId}/locations`

**请求头**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**
```json
{
  "name": "东门",
  "latitude": 39.922,
  "longitude": 116.456,
  "allowedRadius": 100
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 地点名称（如：东门、西门、停车场） |
| latitude | Double | 是 | 纬度 |
| longitude | Double | 是 | 经度 |
| allowedRadius | Double | 否 | 签到半径（米），默认100米 |

**响应示例**
```json
{
  "success": true,
  "message": "签到地点添加成功",
  "data": {
    "id": 1,
    "name": "东门",
    "latitude": 39.922,
    "longitude": 116.456,
    "allowedRadius": 100.0
  }
}
```

### 1.3 修改签到地点

**PUT** `/api/sites/{siteId}/locations/{locationId}`

**请求头**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**
```json
{
  "name": "东大门",
  "latitude": 39.922,
  "longitude": 116.456,
  "allowedRadius": 120
}
```

**响应示例**
```json
{
  "success": true,
  "message": "签到地点修改成功",
  "data": {
    "id": 1,
    "name": "东大门",
    "latitude": 39.922,
    "longitude": 116.456,
    "allowedRadius": 120.0
  }
}
```

### 1.4 删除签到地点

**DELETE** `/api/sites/{siteId}/locations/{locationId}`

**请求头**
```
Authorization: Bearer {token}
```

**响应示例**
```json
{
  "success": true,
  "message": "签到地点删除成功"
}
```

---

## 二、单位统计 API

### 2.1 获取单位统计数据

**GET** `/api/sites/{siteId}/statistics`

**请求头**
```
Authorization: Bearer {token}
```

**响应示例**
```json
{
  "success": true,
  "data": {
    "siteId": 1,
    "siteName": "北京市朝阳区万达广场",
    "guardCount": 4,
    "todayStats": {
      "checkinCount": 3,
      "uniqueGuards": 3,
      "checkinRate": 75,
      "onDutyNow": 2
    },
    "weeklyStats": {
      "totalCheckins": 25,
      "avgDailyCheckins": 3.6
    }
  }
}
```

**字段说明**

| 字段 | 说明 |
|------|------|
| guardCount | 单位保安总数 |
| todayStats.checkinCount | 今日签到次数 |
| todayStats.uniqueGuards | 今日签到人数（去重） |
| todayStats.checkinRate | 今日签到率（百分比） |
| todayStats.onDutyNow | 当前在岗人数 |
| weeklyStats.totalCheckins | 过去7天签到总次数 |
| weeklyStats.avgDailyCheckins | 日均签到次数 |

### 2.2 获取单位保安列表

**GET** `/api/sites/{siteId}/guards`

**请求头**
```
Authorization: Bearer {token}
```

**查询参数**

| 参数 | 默认值 | 说明 |
|------|--------|------|
| page | 1 | 页码 |
| pageSize | 20 | 每页数量 |
| sortBy | id | 排序字段 |
| sortOrder | asc | 排序方向（asc/desc） |

**响应示例**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "张伟",
      "employeeId": "20251231-0000001-Abc123",
      "role": "TEAM_LEADER",
      "roleName": "队长",
      "phoneNumber": "138****0001",
      "onDuty": true,
      "currentCheckinId": 172,
      "todayCheckinCount": 1
    },
    {
      "id": 2,
      "name": "李强",
      "employeeId": "20251231-0000002-Def456",
      "role": "TEAM_MEMBER",
      "roleName": "队员",
      "phoneNumber": "139****5678",
      "onDuty": false,
      "currentCheckinId": null,
      "todayCheckinCount": 0
    }
  ],
  "pagination": {
    "total": 4,
    "page": 1,
    "pageSize": 20,
    "totalPages": 1
  }
}
```

**字段说明**

| 字段 | 说明 |
|------|------|
| phoneNumber | 手机号（脱敏显示，如：138****0001） |
| onDuty | 是否在岗 |
| currentCheckinId | 当前在岗记录ID（不在岗时为null） |
| todayCheckinCount | 今日签到次数 |

---

## 三、增强的单位 API

### 3.1 单位列表（增强）

**GET** `/api/sites`

原有响应字段基础上，新增以下字段：

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "北京市朝阳区万达广场",
      "latitude": 39.9219,
      "longitude": 116.4551,
      "allowedRadiusMeters": 200.0,
      "assignedGuardIds": [1, 2],
      "isActive": true,
      "createdAt": "2026-01-07T16:43:28.941",
      "locationCount": 2,
      "guardCount": 4,
      "onDutyNow": 1
    }
  ],
  "pagination": { ... }
}
```

**新增字段**

| 字段 | 说明 |
|------|------|
| locationCount | 签到地点数量 |
| guardCount | 保安数量 |
| onDutyNow | 当前在岗人数 |

### 3.2 单位详情（新增）

**GET** `/api/sites/{id}`

**响应示例**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "北京市朝阳区万达广场",
    "latitude": 39.9219,
    "longitude": 116.4551,
    "allowedRadiusMeters": 200.0,
    "locations": [
      {
        "id": 1,
        "name": "东门",
        "latitude": 39.922,
        "longitude": 116.456,
        "allowedRadius": 100.0
      },
      {
        "id": 2,
        "name": "西门",
        "latitude": 39.921,
        "longitude": 116.454,
        "allowedRadius": 80.0
      }
    ],
    "guardCount": 4,
    "onDutyNow": 1
  }
}
```

---

## 四、签到验证逻辑

### 4.1 多地点验证规则

当保安签到/抽查时，系统按以下逻辑验证位置：

1. **有签到地点配置时**：检查是否在**任意一个**签到地点的允许范围内
2. **无签到地点配置时**：使用单位(WorkSite)原有的坐标和半径进行验证（向后兼容）

### 4.2 验证失败提示

如果位置验证失败，返回包含最近地点距离的提示：

```
位置超出允许范围（距最近地点[东门]：150米）
```

### 4.3 单位坐标（WorkSite.latitude/longitude/allowedRadiusMeters）的用途

| 场景 | 签到验证使用 | 单位坐标用途 |
|------|-------------|-------------|
| **有签到地点** | `CheckinLocation` 的坐标和半径 | 仅用于地图显示中心点 |
| **无签到地点** | `WorkSite` 的坐标和半径 | 签到验证 + 地图显示 |

**说明**：
- 单位的 `latitude`/`longitude`/`allowedRadiusMeters` 主要用于**向后兼容**
- 如果单位配置了签到地点，这些字段**不再参与签到验证**，仅作为地图中心点显示
- 推荐为每个单位至少配置一个签到地点，以便更精确地控制签到范围

---

## 五、数据库变更

### 5.1 新增表：checkin_location

```sql
CREATE TABLE checkin_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '地点名称',
    latitude DOUBLE NOT NULL COMMENT '纬度',
    longitude DOUBLE NOT NULL COMMENT '经度',
    allowed_radius DOUBLE NOT NULL DEFAULT 100 COMMENT '允许签到半径（米）',
    site_id BIGINT NOT NULL COMMENT '所属单位ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_location_site FOREIGN KEY (site_id)
        REFERENCES work_site(id) ON DELETE CASCADE
);

CREATE INDEX idx_checkin_location_site ON checkin_location(site_id);
```

### 5.2 数据迁移

Flyway迁移脚本：`V3__add_checkin_location.sql`

自动为现有单位创建默认签到地点（使用WorkSite原有坐标）。

---

## 六、权限要求

所有新增API均需要 **ADMIN** 角色权限。

| 端点 | 权限 |
|------|------|
| `/api/sites/{siteId}/locations/**` | ADMIN |
| `/api/sites/{siteId}/statistics` | ADMIN |
| `/api/sites/{siteId}/guards` | ADMIN |

---

## 七、文件变更清单

### 新建文件

| 文件 | 说明 |
|------|------|
| `entity/CheckinLocation.java` | 签到地点实体 |
| `repository/CheckinLocationRepository.java` | 签到地点数据访问层 |
| `controller/CheckinLocationController.java` | 签到地点CRUD控制器 |
| `controller/SiteStatisticsController.java` | 单位统计控制器 |
| `dto/CheckinLocationRequest.java` | 签到地点请求DTO |
| `dto/CheckinLocationResponse.java` | 签到地点响应DTO |
| `dto/SiteStatisticsResponse.java` | 单位统计响应DTO |
| `dto/SiteGuardResponse.java` | 单位保安列表响应DTO |
| `db/migration/V3__add_checkin_location.sql` | 数据库迁移脚本 |

### 修改文件

| 文件 | 修改内容 |
|------|----------|
| `entity/WorkSite.java` | 添加 locations 关联 |
| `dto/SiteResponse.java` | 添加 locationCount, guardCount, onDutyNow 字段 |
| `controller/WorkSiteController.java` | 增强列表响应，新增详情端点 |
| `service/WorkService.java` | 多地点签到验证逻辑 |
| `impl/SpotCheckServiceImpl.java` | 多地点抽查验证逻辑 |
| `repository/SecurityGuardRepository.java` | 新增统计查询方法 |
| `repository/CheckinRepository.java` | 新增单位统计查询方法 |
