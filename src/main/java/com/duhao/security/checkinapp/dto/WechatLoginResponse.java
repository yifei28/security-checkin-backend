package com.duhao.security.checkinapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WechatLoginResponse {
    private boolean success;
    private String token;
    private UserInfo userInfo;
    private String message;
    @JsonProperty("expiresIn")
    private long expiresIn;
    @JsonProperty("error_code")
    private String errorCode;

    public static class UserInfo {
        private String openid;
        private String name;
        @JsonProperty("employeeId")
        private String employeeId;
        private String phone;
        private String department;

        // Constructors
        public UserInfo() {}

        public UserInfo(String openid, String name, String employeeId, String phone, String department) {
            this.openid = openid;
            this.name = name;
            this.employeeId = employeeId;
            this.phone = phone;
            this.department = department;
        }

        // Getters and Setters
        public String getOpenid() {
            return openid;
        }

        public void setOpenid(String openid) {
            this.openid = openid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }
    }

    // Constructors
    public WechatLoginResponse() {}

    public WechatLoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public WechatLoginResponse(boolean success, String token, UserInfo userInfo, String message, long expiresIn) {
        this.success = success;
        this.token = token;
        this.userInfo = userInfo;
        this.message = message;
        this.expiresIn = expiresIn;
    }

    // Static factory methods for success response
    public static WechatLoginResponse success(String token, UserInfo userInfo, long expiresIn) {
        return new WechatLoginResponse(true, token, userInfo, "Login successful", expiresIn);
    }

    public static WechatLoginResponse success(String token, UserInfo userInfo, String message, long expiresIn) {
        return new WechatLoginResponse(true, token, userInfo, message, expiresIn);
    }

    // Static factory methods for error response
    public static WechatLoginResponse error(String message) {
        return new WechatLoginResponse(false, message);
    }

    public static WechatLoginResponse error(String message, String errorCode) {
        WechatLoginResponse response = new WechatLoginResponse(false, message);
        response.setErrorCode(errorCode);
        return response;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}