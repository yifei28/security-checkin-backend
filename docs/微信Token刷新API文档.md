# 微信Token刷新API文档

## 概述

微信Token刷新API (`POST /api/wechat-refresh-token`) 用于在微信小程序JWT token即将过期或已过期时，生成新的token，延长用户的登录状态。该API支持token过期后的优雅续期机制。

## 接口信息

- **接口路径**: `POST /api/wechat-refresh-token`
- **认证要求**: 需要在Authorization头中提供JWT token（即使已过期）
- **内容类型**: `application/json`

## 请求格式

### 请求头
```http
Authorization: Bearer <expired_or_valid_jwt_token>
Content-Type: application/json
```

### 请求体
该接口不需要请求体，所有必需信息都从Authorization头中的JWT token提取。

## 响应格式

### 成功响应 (200 OK)
```json
{
    "success": true,
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
        "openid": "oxxx_xxxxxxxxxxxxxxxxx",
        "name": "张三",
        "employeeId": "20250815-0000001-AbCdEf",
        "phone": "13888888888",
        "department": "某某大厦"
    },
    "message": "Token refresh successful",
    "expiresIn": 7200
}
```

### 错误响应

#### 1. 缺少Authorization头 (401 Unauthorized)
```json
{
    "success": false,
    "message": "缺少或无效的 Authorization 头",
    "error_code": "40003"
}
```

#### 2. Token格式无效 (401 Unauthorized)
```json
{
    "success": false,
    "message": "token无效",
    "error_code": "40004"
}
```

#### 3. Token格式错误 (401 Unauthorized)
```json
{
    "success": false,
    "message": "token格式无效",
    "error_code": "40005"
}
```

#### 4. 用户不存在 (401 Unauthorized)
```json
{
    "success": false,
    "message": "不存在绑定该openid的用户",
    "error_code": "40006"
}
```

#### 5. 系统错误 (401 Unauthorized)
```json
{
    "success": false,
    "message": "系统内部错误",
    "error_code": "50000"
}
```

## 字段说明

### 响应字段

| 字段名 | 类型 | 描述 |
|--------|------|------|
| success | boolean | 刷新是否成功 |
| token | string | 新生成的JWT token (成功时返回) |
| userInfo | object | 用户信息对象 (成功时返回) |
| message | string | 响应消息 |
| expiresIn | number | token有效期（秒），默认7200秒（2小时） |
| error_code | string | 错误代码 (失败时返回) |

### userInfo对象字段

| 字段名 | 类型 | 描述 |
|--------|------|------|
| openid | string | 微信用户唯一标识 |
| name | string | 保安姓名 |
| employeeId | string | 员工编号 |
| phone | string | 手机号码 |
| department | string | 工作单位名称 |

## 业务逻辑

### 处理流程

1. **提取JWT token**: 从Authorization头中提取Bearer token
2. **解析token**: 支持已过期token的解析，从中提取openid
3. **验证用户**: 根据openid查询数据库中的保安信息
4. **生成新token**: 使用相同的openid生成新的JWT token
5. **返回用户信息**: 包含完整的用户信息和新token

### 特殊处理

- **过期token支持**: 即使token已过期，系统仍能从中提取openid进行续期
- **签名验证**: 只有签名有效的token才能被刷新
- **用户状态检查**: 确保openid对应的用户仍然存在且有效

## 错误代码说明

| 错误代码 | 描述 | 解决方案 |
|----------|------|----------|
| 40003 | 缺少或无效的 Authorization 头 | 检查请求头格式是否正确 |
| 40004 | token无效 | token签名错误或格式损坏，需要重新登录 |
| 40005 | token格式无效 | token中缺少openid信息，需要重新登录 |
| 40006 | 用户不存在 | openid对应的用户已被删除，需要重新注册 |
| 50000 | 系统内部错误 | 服务器内部错误，稍后重试 |

## 使用场景

1. **定时刷新**: 小程序可以在token过期前定时调用此接口
2. **接口调用失败时**: 当其他API返回401时，可先尝试刷新token再重试
3. **应用启动时**: 检查本地存储的token是否需要刷新

## 安全考虑

- 只能刷新签名有效的token，防止伪造
- 即使token过期也要验证签名，确保token来源可信
- 刷新后的token有效期重新计算（2小时）
- 用户状态实时验证，确保账户仍然有效

## 示例代码

### JavaScript/小程序调用示例

```javascript
// 刷新token
async function refreshToken() {
    const oldToken = wx.getStorageSync('token');
    
    try {
        const response = await wx.request({
            url: 'https://api.example.com/api/wechat-refresh-token',
            method: 'POST',
            header: {
                'Authorization': `Bearer ${oldToken}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.data.success) {
            // 保存新token
            wx.setStorageSync('token', response.data.token);
            wx.setStorageSync('userInfo', response.data.userInfo);
            console.log('Token刷新成功');
            return response.data.token;
        } else {
            console.error('Token刷新失败:', response.data.message);
            // 跳转到登录页面
            wx.redirectTo({ url: '/pages/login/login' });
            return null;
        }
    } catch (error) {
        console.error('刷新token请求失败:', error);
        return null;
    }
}

// 自动重试机制
async function apiCallWithRefresh(apiCall) {
    try {
        return await apiCall();
    } catch (error) {
        if (error.statusCode === 401) {
            // token过期，尝试刷新
            const newToken = await refreshToken();
            if (newToken) {
                // 用新token重试
                return await apiCall();
            }
        }
        throw error;
    }
}
```

## 相关接口

- [微信登录API文档](./小程序API调用文档.md)
- [签到API文档](./签到API文档.md)

---

*最后更新: 2024年*