package com.duhao.security.checkinapp.dto;

public class CheckinRequest {
    private String employeeId;  // 员工编号（从登录响应获取）
    private Double latitude;    // 签到纬度
    private Double longitude;   // 签到经度
    private String faceImageUrl; // 人脸图片URL（可选）

    // 构造函数
    public CheckinRequest() {}

    public CheckinRequest(String employeeId, Double latitude, Double longitude, String faceImageUrl) {
        this.employeeId = employeeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.faceImageUrl = faceImageUrl;
    }

    // Getters and Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getFaceImageUrl() {
        return faceImageUrl;
    }

    public void setFaceImageUrl(String faceImageUrl) {
        this.faceImageUrl = faceImageUrl;
    }
}