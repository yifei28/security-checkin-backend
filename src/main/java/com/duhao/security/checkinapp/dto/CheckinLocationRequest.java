package com.duhao.security.checkinapp.dto;

/**
 * 签到地点请求DTO
 */
public class CheckinLocationRequest {

    private String name;
    private Double latitude;
    private Double longitude;
    private Double allowedRadius;

    public CheckinLocationRequest() {
    }

    public CheckinLocationRequest(String name, Double latitude, Double longitude, Double allowedRadius) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadius = allowedRadius;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Double getAllowedRadius() {
        return allowedRadius;
    }

    public void setAllowedRadius(Double allowedRadius) {
        this.allowedRadius = allowedRadius;
    }
}
