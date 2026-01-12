package com.duhao.security.checkinapp.dto;

import java.time.LocalDateTime;

/**
 * 在岗保安位置信息（用于地图显示）
 */
public class OnDutyGuardInfo {

    private Long id;                // 保安ID
    private String name;            // 保安姓名
    private Double lat;             // 上岗位置纬度
    private Double lng;             // 上岗位置经度
    private LocalDateTime startTime; // 上岗时间

    public OnDutyGuardInfo() {
    }

    public OnDutyGuardInfo(Long id, String name, Double lat, Double lng, LocalDateTime startTime) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.startTime = startTime;
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

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
}
