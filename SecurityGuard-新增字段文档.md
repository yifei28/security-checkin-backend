# SecurityGuard 新增字段功能文档

## 概述

为 SecurityGuard（安保员）实体添加了两个新的个人信息字段：**生日（birthDate）** 和 **身高（height）**，以完善安保员的基础信息管理。

## 新增字段详细说明

### 1. 生日字段（birthDate）

#### 技术实现
- **数据类型**: `LocalDate`
- **数据库字段**: `birth_date` (DATE类型)
- **允许为空**: 是（兼容现有数据）
- **实现方式**: 存储生日，动态计算年龄

#### 核心特性
- **自动年龄计算**: 提供 `@Transient getAge()` 方法，实时计算当前年龄
- **历史年龄查询**: 提供 `getAgeAt(LocalDate date)` 方法，可计算指定日期时的年龄
- **永久准确性**: 年龄每天自动更新，无需维护定时任务

#### 示例代码
```java
SecurityGuard guard = new SecurityGuard();
guard.setBirthDate(LocalDate.of(1990, 5, 15));

// 动态获取当前年龄
Integer currentAge = guard.getAge(); // 返回35（基于2025年）

// 获取特定日期的年龄
Integer ageIn2020 = guard.getAgeAt(LocalDate.of(2020, 6, 1)); // 返回30
```

### 2. 身高字段（height）

#### 技术实现
- **数据类型**: `Integer`
- **数据库字段**: `height` (INT类型)
- **单位**: 厘米（cm）
- **允许为空**: 是（可选信息）

#### 特性
- **存储方式**: 直接存储身高数值
- **数据验证**: 前端可添加合理范围验证（如150-220cm）
- **显示格式**: 可在前端格式化为 "175cm" 形式

#### 示例
```java
SecurityGuard guard = new SecurityGuard();
guard.setHeight(175); // 175厘米
```

## 数据库变更

系统采用 Hibernate 自动 DDL 更新，在应用启动时自动执行：

```sql
-- 生日字段
ALTER TABLE security_guard ADD COLUMN birth_date DATE;

-- 身高字段
ALTER TABLE security_guard ADD COLUMN height INTEGER;
```

## API 变更

### POST /api/guards - 创建安保员

**请求示例**:
```json
{
    "name": "张三",
    "phoneNumber": "13800138000",
    "birthDate": "1990-05-15",
    "height": 175,
    "site": {"id": 1}
}
```

**响应示例**:
```json
{
    "id": 32,
    "name": "张三",
    "employeeId": "20250923-0000032-MM2V6s",
    "phoneNumber": "13800138000",
    "birthDate": "1990-05-15",
    "height": 175,
    "age": 35,
    "site": {
        "id": 1,
        "name": "办公大楼A座"
    },
    "role": "TEAM_MEMBER"
}
```

### PUT /api/guards/{id} - 更新安保员

现在支持更新生日和身高字段：

```json
{
    "name": "张三",
    "phoneNumber": "13800138000",
    "birthDate": "1985-12-25",
    "height": 180,
    "site": {"id": 1}
}
```

## 构造函数更新

新增了支持生日和身高参数的构造函数：

```java
// 原有构造函数（保持兼容）
public SecurityGuard(String name, String phoneNumber, WorkSite site)

// 支持生日的构造函数
public SecurityGuard(String name, String phoneNumber, WorkSite site, LocalDate birthDate)

// 支持生日和身高的完整构造函数
public SecurityGuard(String name, String phoneNumber, WorkSite site, LocalDate birthDate, Integer height)
```

## 测试验证

### 单元测试
已添加 `SecurityGuardAgeTest.java` 测试类，包含：
- ✅ 基本年龄计算测试
- ✅ 指定日期年龄计算测试
- ✅ 空生日处理测试

### API 测试
已验证的功能：
- ✅ 创建带生日和身高的安保员
- ✅ 年龄自动计算（1995年生→30岁）
- ✅ 更新生日和身高字段
- ✅ JSON 序列化包含所有新字段

## 兼容性说明

### 数据兼容性
- 现有安保员数据完全兼容
- 新字段允许为空，不影响现有功能
- 生日为空时，`getAge()` 返回 `null`

### API 兼容性
- 现有 API 调用不受影响
- 新字段为可选，前端可渐进式支持
- 响应 JSON 增加了 `birthDate`、`height`、`age` 字段

## 业务价值

### 人员管理优化
- **年龄统计**: 可按年龄段统计安保员分布
- **体能要求**: 身高信息有助于工作岗位匹配
- **档案完整**: 提升安保员基础信息完整度

### 数据分析支持
- **年龄趋势**: 分析安保员年龄结构
- **岗位适配**: 根据身高匹配合适岗位
- **人员规划**: 支持基于年龄的人员发展规划

## 技术优势

### 年龄字段设计
- **实时准确**: 无需定期更新，永远准确
- **高性能**: 计算开销极小
- **灵活查询**: 可查询任意时间点年龄

### 数据库设计
- **自动迁移**: Hibernate DDL 自动处理
- **索引友好**: 可按生日或身高建立索引
- **存储优化**: 最小化存储空间使用

## 未来扩展建议

1. **数据验证**: 添加年龄和身高合理性验证
2. **统计报表**: 基于年龄和身高的统计分析
3. **岗位匹配**: 基于身高要求的岗位推荐
4. **档案导出**: 支持包含新字段的档案导出

---

**文档版本**: 1.0
**更新日期**: 2025-09-23
**相关功能**: SecurityGuard 实体管理、API接口、数据库设计