# 微信小程序签到记录查询API文档

## 接口概览

本接口专为微信小程序端设计，用于查询指定员工的最近20条签到记录。与通用的 `/api/checkin/my-records` 接口相比，该接口固定返回20条记录，简化了小程序端的调用复杂度。

## 接口信息

- **接口地址**: `/api/wechat-checkin/records`
- **请求方法**: `GET`
- **认证方式**: JWT Token（Bearer认证）
- **内容类型**: `application/json; charset=UTF-8`

## 请求参数

### Query参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| employeeId | String | 是 | 员工ID，格式为：YYYYMMDD-7位数字-6位随机字符 |

### 请求示例

```http
GET /api/wechat-checkin/records?employeeId=20250816-0000027-PIw1XF HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0Iiwi...
Content-Type: application/json; charset=UTF-8
```

### cURL示例

```bash
curl -X GET "http://localhost:8080/api/wechat-checkin/records?employeeId=20250816-0000027-PIw1XF" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

## 响应格式

### 成功响应 (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "id": "checkin_42",
      "guardInfo": "李易非",
      "siteInfo": "办公大楼A座", 
      "timestamp": "2025-01-16T18:45:00",
      "location": {
        "latitude": 39.878185,
        "longitude": 116.620212
      },
      "faceImageUrl": "/images/test/face3.jpg",
      "status": "成功",
      "reason": null
    },
    {
      "id": "checkin_41",
      "guardInfo": "李易非",
      "siteInfo": "办公大楼A座",
      "timestamp": "2025-01-16T12:15:00", 
      "location": {
        "latitude": 39.878185,
        "longitude": 116.620212
      },
      "faceImageUrl": "/images/test/face2.jpg",
      "status": "成功",
      "reason": null
    }
  ],
  "pagination": {
    "totalCount": 12,
    "currentPage": 1,
    "pageSize": 20,
    "totalPages": 1
  },
  "statistics": null
}
```

### 错误响应

#### 401 Unauthorized - 认证失败
```json
{
  "error": "Authorization header is missing or malformed"
}
```

#### 404 Not Found - 员工不存在
```json
{
  "success": false,
  "data": null,
  "pagination": null
}
```

#### 500 Internal Server Error - 服务器内部错误
```json
{
  "success": false, 
  "data": null,
  "pagination": null
}
```

## 响应字段说明

### 主体结构

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | Boolean | 请求是否成功 |
| data | Array | 签到记录数组 |
| pagination | Object | 分页信息 |
| statistics | Object | 统计信息（此接口固定为null） |

### 签到记录对象 (data数组元素)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | String | 签到记录ID，格式：checkin_{数字ID} |
| guardInfo | String | 保安员姓名 |
| siteInfo | String | 工作站点名称 |
| timestamp | String | 签到时间，ISO 8601格式 |
| location | Object | 签到位置坐标 |
| location.latitude | Number | 纬度 |
| location.longitude | Number | 经度 |
| faceImageUrl | String | 人脸识别图片URL |
| status | String | 签到状态：成功/失败/待处理 |
| reason | String | 失败原因（成功时为null） |

### 分页信息对象 (pagination)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| totalCount | Number | 总记录数 |
| currentPage | Number | 当前页码（固定为1） |
| pageSize | Number | 每页记录数（固定为20） |
| totalPages | Number | 总页数 |

## 接口特性

### 固定返回数量
- 该接口固定返回最近的20条签到记录
- 按签到时间倒序排列（最新的在前）
- 不支持分页参数

### 专为小程序设计
- 返回数据结构适合小程序展示
- guardInfo和siteInfo字段返回名称而非ID
- 简化了调用参数，无需复杂的分页和排序参数

### 与通用接口的区别

| 特性 | 微信小程序接口 | 通用接口 `/api/checkin/my-records` |
|------|---------------|----------------------------------|
| 返回记录数 | 固定20条 | 可配置pageSize参数 |
| 分页支持 | 不支持 | 支持完整分页 |
| 排序支持 | 固定按时间倒序 | 支持多种排序方式 |
| guardInfo字段 | 返回保安员姓名 | 返回保安员姓名 |
| siteInfo字段 | 返回站点名称 | 返回站点名称 |

## 使用示例

### JavaScript/微信小程序示例

```javascript
// 微信小程序中调用示例
wx.request({
  url: 'https://your-domain.com/api/wechat-checkin/records',
  method: 'GET',
  data: {
    employeeId: '20250816-0000027-PIw1XF'
  },
  header: {
    'Authorization': 'Bearer ' + wx.getStorageSync('token'),
    'Content-Type': 'application/json'
  },
  success: function(res) {
    if (res.data.success) {
      console.log('最近20条签到记录:', res.data.data);
      // 处理签到记录数据
      const checkinRecords = res.data.data;
      // 更新页面数据...
    } else {
      console.error('查询失败');
    }
  },
  fail: function(error) {
    console.error('请求失败:', error);
  }
});
```

## 错误码说明

| HTTP状态码 | 错误说明 | 解决方案 |
|-----------|----------|----------|
| 401 | JWT Token无效或过期 | 重新登录获取有效Token |
| 404 | 员工ID不存在 | 检查employeeId参数是否正确 |
| 500 | 服务器内部错误 | 稍后重试或联系管理员 |

## 注意事项

1. **认证要求**: 该接口需要有效的JWT Token认证，请确保在请求头中包含正确的Authorization字段
2. **员工ID格式**: employeeId必须是系统自动生成的格式：YYYYMMDD-7位数字-6位随机字符
3. **数据量限制**: 接口固定返回最近20条记录，如需查看更多历史记录，请使用通用的分页接口
4. **时间格式**: 所有时间字段采用ISO 8601格式，时区为UTC
5. **缓存策略**: 建议客户端实现适当的缓存策略，避免频繁请求

## 更新日志

- **2025-01-16**: 初版接口发布，专为微信小程序端设计