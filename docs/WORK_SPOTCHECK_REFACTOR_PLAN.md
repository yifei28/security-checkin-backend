# 打卡与抽查系统重构计划

---

## 一、核心模型

```
上岗(START) ────── 在岗(ACTIVE) ────── 下岗(END)
                      │
                   可被抽查
```

| 规则 | 说明 |
|------|------|
| 上岗 | 随时可开始，验证位置+人脸 |
| 下岗 | 至少在岗1小时，验证位置+人脸 |
| 超时 | 16小时未下岗自动结束，标记 TIMEOUT |
| 多片段 | 一天可多次上岗/下岗 |

---

## 二、数据模型

### 2.1 CheckinRecord（工作片段）

```java
@Entity
public class CheckinRecord {
    @Id
    Long id;

    @Version
    Long version;  // 乐观锁

    @ManyToOne
    SecurityGuard guard;

    @ManyToOne
    WorkSite site;

    // 上岗
    LocalDateTime startTime;
    Double startLatitude, startLongitude;
    String startFaceImageUrl;

    // 下岗 (null = 在岗中)
    LocalDateTime endTime;
    Double endLatitude, endLongitude;
    String endFaceImageUrl;

    // 状态
    @Enumerated(EnumType.STRING)
    WorkStatus status;

    // 冗余统计（下岗时计算）
    Long durationMinutes;
    Integer spotCheckTotal;
    Integer spotCheckPassed;
}
```

### 2.2 WorkStatus

```java
public enum WorkStatus {
    ACTIVE,      // 在岗中
    COMPLETED,   // 正常下岗
    TIMEOUT,     // 16小时超时
    LEGACY       // 旧数据
}
```

### 2.3 SpotCheck（抽查记录）

```java
@Entity
public class SpotCheck {
    @Id
    Long id;

    @Version
    Long version;  // 乐观锁

    @ManyToOne
    CheckinRecord checkinRecord;  // FK，1:N 关系

    LocalDateTime createdAt;
    LocalDateTime deadline;       // +15分钟
    LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    SpotCheckStatus status;

    @Enumerated(EnumType.STRING)
    SpotCheckTriggerType triggerType;

    // 验证信息
    Double latitude, longitude;
    String faceImageUrl;
}
```

### 2.4 SpotCheckStatus

```java
public enum SpotCheckStatus {
    PENDING,  // 待处理（可重试验证）
    PASSED,   // 通过
    MISSED    // 超时未响应
}
```

---

## 三、事件驱动调度（Redis ZSET）

### 3.1 设计理念

**从轮询改为事件驱动**：在创建时预约未来事件，而非每分钟扫描全表。

### 3.2 队列设计

| 队列 Key | Score | Value | 用途 |
|----------|-------|-------|------|
| `timeout:session` | 超时时间戳 | `sessionId:version` | 16小时自动下岗 |
| `spotcheck:trigger` | 触发时间戳 | `sessionId:version` | 抽查触发 |
| `spotcheck:timeout` | 超时时间戳 | `spotCheckId:version` | 15分钟抽查超时 |

### 3.3 消费者

```java
@Scheduled(fixedRate = 1000)
public void processDelayedTasks() {
    long now = System.currentTimeMillis();

    // 处理超时下岗
    processQueue("timeout:session", now, this::onSessionTimeout);

    // 处理抽查触发
    processQueue("spotcheck:trigger", now, this::onSpotCheckTrigger);

    // 处理抽查超时
    processQueue("spotcheck:timeout", now, this::onSpotCheckTimeout);
}

private void processQueue(String key, long now, Consumer<String> handler) {
    Set<String> tasks = redis.zrangeByScore(key, 0, now);
    for (String taskValue : tasks) {
        handler.accept(taskValue);
        redis.zrem(key, taskValue);
    }
}
```

### 3.4 并发安全校验

```java
void onSessionTimeout(String taskValue) {
    String[] parts = taskValue.split(":");
    Long sessionId = Long.parseLong(parts[0]);
    Long expectedVersion = Long.parseLong(parts[1]);

    CheckinRecord session = findById(sessionId);

    // 三重校验
    if (session == null) return;                              // 记录存在
    if (!session.getVersion().equals(expectedVersion)) return; // version 一致
    if (session.getStatus() != ACTIVE) return;                 // 状态仍 ACTIVE

    // 安全执行
    doTimeout(session);
}
```

---

## 四、抽查调度算法（指数分布）

### 4.1 配置

```properties
spotcheck.avg-interval-minutes=120   # 平均2小时一次
spotcheck.min-interval-minutes=30    # 最少30分钟
spotcheck.max-interval-minutes=240   # 最多4小时
spotcheck.response-minutes=15        # 响应时间
```

### 4.2 算法

```java
private LocalDateTime generateNextSpotCheckTime(LocalDateTime from) {
    double lambda = 1.0 / avgIntervalMinutes;
    double interval = -Math.log(Math.random()) / lambda;

    // 边界保护
    interval = Math.max(minIntervalMinutes, interval);
    interval = Math.min(maxIntervalMinutes, interval);

    return from.plusMinutes((long) interval);
}
```

### 4.3 最后1小时规则

```java
void onSpotCheckTrigger(Long sessionId, Long expectedVersion) {
    CheckinRecord session = findById(sessionId);

    // 校验...

    // 计算距离超时的剩余时间
    LocalDateTime timeoutAt = session.getStartTime().plusHours(16);
    long remainingMinutes = Duration.between(now(), timeoutAt).toMinutes();

    // 最后1小时不安排新抽查
    if (remainingMinutes < 60) {
        return;
    }

    // 创建抽查并预约下次...
}
```

### 4.4 预期效果

| 在岗时长 | 预期抽查次数 |
|----------|--------------|
| 4小时 | ~2次 |
| 8小时 | ~4次 |
| 12小时 | ~6次 |

---

## 五、业务流程

### 5.1 上岗

```
1. 检查无 ACTIVE 记录
2. 验证位置 + 人脸
3. 创建 CheckinRecord (status=ACTIVE, version=0)
4. 预约超时：
   ZADD timeout:session <startTime+16h> "sessionId:0"
5. 生成首次抽查时间（指数分布，从 +30min 开始）
6. 预约抽查：
   ZADD spotcheck:trigger <nextTime> "sessionId:0"
```

### 5.2 下岗

```
1. 查找 ACTIVE 记录
2. 检查时长 ≥ 1小时
3. 验证位置 + 人脸
4. 更新记录：
   - endTime = now
   - status = COMPLETED
   - durationMinutes = 计算时长
   - spotCheckTotal = count(抽查)
   - spotCheckPassed = count(PASSED)
5. 取消预约：
   ZREM timeout:session "sessionId:*"
   ZREM spotcheck:trigger "sessionId:*"
6. 所有 PENDING 抽查 → MISSED
```

### 5.3 超时下岗（自动触发）

```
触发条件：timeout:session 到期

1. 三重校验（存在、version、ACTIVE）
2. 更新记录：
   - endTime = startTime + 16h
   - status = TIMEOUT
   - 计算冗余字段
3. 取消抽查预约
4. PENDING 抽查 → MISSED
```

### 5.4 抽查触发

```
触发条件：spotcheck:trigger 到期

1. 三重校验
2. 检查剩余时间 ≥ 1小时
3. 创建 SpotCheck (status=PENDING, version=0)
4. 预约抽查超时：
   ZADD spotcheck:timeout <+15min> "spotCheckId:0"
5. 生成下次抽查时间
6. 预约下次：
   ZADD spotcheck:trigger <nextTime> "sessionId:version"
7. 发送通知（可选）
```

### 5.5 完成抽查

```
1. 查找 SpotCheck，校验 status == PENDING
2. 验证位置 + 人脸
   → 失败：返回错误，保持 PENDING（可重试）
   → 成功：继续
3. 更新：
   - status = PASSED
   - completedAt = now
   - 位置/人脸信息
4. 取消超时预约：
   ZREM spotcheck:timeout "spotCheckId:*"
```

### 5.6 抽查超时（自动触发）

```
触发条件：spotcheck:timeout 到期

1. 校验 version 和 status
2. 更新 status = MISSED
```

---

## 六、API 设计

### 6.1 保安端

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/work/start` | POST | 上岗 |
| `/api/work/end` | POST | 下岗 |
| `/api/work/status` | GET | 当前状态（是否在岗、时长、待处理抽查） |
| `/api/work/history` | GET | 工作记录历史 |
| `/api/spot-check/pending` | GET | 待处理抽查 |
| `/api/spot-check/complete` | POST | 完成抽查验证 |

### 6.2 管理端

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/admin/work/records` | GET | 工作记录列表（含抽查摘要） |
| `/api/admin/work/{id}` | GET | 记录详情（含抽查明细） |
| `/api/admin/spot-check/trigger` | POST | 手动触发抽查 |
| `/api/admin/report/weekly` | GET | 周报 |
| `/api/admin/report/monthly` | GET | 月报 |

---

## 七、报表设计

### 7.1 查询规则

| 规则 | 说明 |
|------|------|
| 筛选依据 | startTime |
| 跨天归属 | 按上岗日期 |
| 时长统计 | 只算 COMPLETED / TIMEOUT 记录 |
| 在岗中 | 标记"进行中"，不计入时长 |

### 7.2 报表响应示例

```json
{
  "period": "2025-01",
  "summary": {
    "totalGuards": 50,
    "totalHours": 8800,
    "avgHoursPerPerson": 176,
    "spotCheckPassRate": "94%"
  },
  "guards": [
    {
      "name": "张三",
      "workDays": 22,
      "totalHours": 176.5,
      "sessions": 44,
      "spotCheck": {
        "total": 88,
        "passed": 85,
        "missed": 3,
        "passRate": "97%"
      },
      "anomalies": {
        "timeoutCount": 0
      },
      "currentStatus": null
    }
  ]
}
```

---

## 八、数据迁移

```sql
-- 1. 重命名字段
ALTER TABLE checkin_record
  RENAME COLUMN timestamp TO start_time,
  RENAME COLUMN latitude TO start_latitude,
  RENAME COLUMN longitude TO start_longitude,
  RENAME COLUMN face_image_url TO start_face_image_url;

-- 2. 新增字段
ALTER TABLE checkin_record
  ADD COLUMN version BIGINT DEFAULT 0,
  ADD COLUMN end_time DATETIME NULL,
  ADD COLUMN end_latitude DOUBLE NULL,
  ADD COLUMN end_longitude DOUBLE NULL,
  ADD COLUMN end_face_image_url VARCHAR(500) NULL,
  ADD COLUMN duration_minutes BIGINT NULL,
  ADD COLUMN spot_check_total INT DEFAULT 0,
  ADD COLUMN spot_check_passed INT DEFAULT 0;

-- 3. 迁移状态
UPDATE checkin_record SET status = 'LEGACY';

-- 4. SpotCheck 加 version
ALTER TABLE spot_check ADD COLUMN version BIGINT DEFAULT 0;
```

---

## 九、文件清单

### 修改

| 文件 | 改动 |
|------|------|
| `entity/CheckinRecord.java` | 重构为工作片段模型，加 @Version |
| `entity/SpotCheck.java` | 关联 CheckinRecord，加 @Version |
| `entity/SpotCheckStatus.java` | 简化为 3 种状态 |
| `repository/CheckinRepository.java` | 新增查询方法 |
| `repository/SpotCheckRepository.java` | 新增查询方法 |

### 新建

| 文件 | 说明 |
|------|------|
| `entity/WorkStatus.java` | 工作状态枚举 |
| `service/WorkService.java` | 上岗/下岗业务逻辑 |
| `service/SpotCheckService.java` | 抽查业务逻辑 |
| `service/DelayedTaskService.java` | Redis 延迟队列服务 |
| `controller/WorkController.java` | 保安端 API |
| `controller/WorkAdminController.java` | 管理端 API |
| `controller/ReportController.java` | 报表 API |
| `config/SpotCheckProperties.java` | 抽查配置 |
| `dto/*` | 请求/响应对象 |

### 删除

| 文件 | 原因 |
|------|------|
| `entity/CheckinStatus.java` | 被 WorkStatus 替代 |

---

## 十、实现顺序

| Phase | 内容 | 依赖 |
|-------|------|------|
| 1 | 数据模型改造 + 迁移 | - |
| 2 | Redis 延迟队列服务 | - |
| 3 | 上岗/下岗 API | Phase 1, 2 |
| 4 | 抽查触发 + 完成 | Phase 1, 2, 3 |
| 5 | 管理端 API | Phase 1, 4 |
| 6 | 报表 API | Phase 1 |

---

## 十一、抽查规则总结

| 规则 | 说明 |
|------|------|
| 触发算法 | 指数分布，平均2小时一次 |
| 最小间隔 | 30分钟 |
| 最大间隔 | 4小时 |
| 响应时间 | 15分钟 |
| 最后1小时 | 不安排新抽查 |
| 验证失败 | 保持 PENDING，可重试 |
| 超时 | 标记 MISSED |
| 下岗时 | PENDING → MISSED |

---

## 十二、时间线示例（8小时班）

```
08:00  上岗
       → 预约超时: 24:00
       → 预约首次抽查: ~08:45
08:45  第1次抽查触发 ✓
       → 预约下次: ~10:30
10:30  第2次抽查触发 ✓
       → 预约下次: ~12:15
12:15  第3次抽查触发 ✓
       → 预约下次: ~14:40
14:40  第4次抽查触发 ✓
       → 预约下次: ~16:30
15:00+ 距离超时(24:00)还有9小时，但距离预期下班(16:00)只剩1小时
       → 最后1小时规则：看距离16小时超时，不是下班时间
       → 所以 16:30 的抽查仍会触发（如果还在岗）
16:00  下岗
       → 取消所有预约
       → 如果 16:30 抽查已触发但未完成 → MISSED
```
