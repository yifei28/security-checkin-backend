# 更新日志 - 2026-01-07

## 1. 修复 403 Forbidden 权限问题

**问题描述**：多个管理端 API 返回 403 Forbidden 错误

**根本原因**：`SecurityConfig.java` 中的请求匹配规则顺序不正确，`/api/work/**`（GUARD权限）被放置在 `/api/admin/**`（ADMIN权限）之前，导致 `/api/admin/work/**` 被错误匹配。

**修复内容** (`util/SecurityConfig.java`)：
- 重新组织规则顺序，ADMIN 规则优先于 GUARD 规则
- 移除无效的 `/api/dashboard/**` 规则（无对应 Controller）
- 添加 `/api/statistics/**` 规则

---

## 2. 新增工作记录抽查查询接口

**新增端点**：`GET /api/admin/work/{id}/spot-checks`

**功能**：查询指定工作记录关联的所有抽查记录

**修改文件**：
- `repository/SpotCheckRepository.java` - 新增 `findByCheckinRecordId()` 方法
- `controller/WorkAdminController.java` - 新增端点和 `SpotCheckRecord` DTO

**响应示例**：
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "createdAt": "2026-01-07T10:00:00",
      "deadline": "2026-01-07T10:15:00",
      "completedAt": "2026-01-07T10:05:00",
      "status": "PASSED",
      "statusName": "已通过",
      "triggerType": "AUTOMATIC",
      "triggerTypeName": "自动触发",
      "latitude": 39.9219,
      "longitude": 116.4551,
      "faceImageUrl": "..."
    }
  ],
  "count": 1
}
```

---

## 3. 工作记录 API 添加位置信息

**端点**：`GET /api/admin/work/records`

**修改内容**：响应数据新增位置字段

**修改文件**：`controller/WorkAdminController.java`

**新增字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| startLatitude | Double | 上岗位置纬度 |
| startLongitude | Double | 上岗位置经度 |
| endLatitude | Double | 下岗位置纬度 |
| endLongitude | Double | 下岗位置经度 |

---

## 4. Dashboard API 结构调整

**端点**：`GET /api/statistics/dashboard`

**修改内容**：

### 移除
- `checkins.overall` 部分（successCount, failedCount, successRate）
- `OverallStats` 类

### 新增
- `checkins.today.onDutyCount` - 当前在岗人数

**修改文件**：
- `dto/DashboardResponse.java` - 调整 DTO 结构
- `controller/StatisticsController.java` - 更新统计逻辑

**响应结构变化**：
```json
// 修改前
{
  "checkins": {
    "today": { "total", "uniqueGuards", "checkinRate" },
    "weekly": { ... },
    "overall": { "successCount", "failedCount", "successRate" }
  }
}

// 修改后
{
  "checkins": {
    "today": {
      "total",
      "uniqueGuards",
      "checkinRate",
      "onDutyCount"
    },
    "weekly": { ... }
  }
}
```

**onDutyCount 计算逻辑**：查询 `CheckinRecord` 表中 `status = 'ACTIVE'` 的记录数量

---

## 文件变更汇总

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `util/SecurityConfig.java` | 修改 | 修复权限规则顺序 |
| `repository/SpotCheckRepository.java` | 修改 | 新增查询方法 |
| `controller/WorkAdminController.java` | 修改 | 新增端点、位置字段、DTO |
| `dto/DashboardResponse.java` | 修改 | 移除 overall，新增 onDutyCount |
| `controller/StatisticsController.java` | 修改 | 更新统计逻辑 |
