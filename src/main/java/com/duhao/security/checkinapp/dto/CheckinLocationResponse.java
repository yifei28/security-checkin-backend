package com.duhao.security.checkinapp.dto;

/**
 * 签到地点响应DTO
 */
public class CheckinLocationResponse {

    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private Double allowedRadius;

    public CheckinLocationResponse() {
    }

    public CheckinLocationResponse(Long id, String name, Double latitude, Double longitude, Double allowedRadius) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadius = allowedRadius;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
