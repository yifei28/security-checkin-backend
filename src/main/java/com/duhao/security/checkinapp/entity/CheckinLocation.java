package com.duhao.security.checkinapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 签到地点实体
 * 每个单位(WorkSite)可以有多个签到地点
 */
@Entity
@Table(name = "checkin_location")
public class CheckinLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 地点名称，如"东门"、"西门"、"停车场"
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 纬度
     */
    @Column(nullable = false)
    private Double latitude;

    /**
     * 经度
     */
    @Column(nullable = false)
    private Double longitude;

    /**
     * 签到允许半径（米），默认100米
     */
    @Column(name = "allowed_radius", nullable = false)
    private Double allowedRadius = 100.0;

    /**
     * 所属单位
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private WorkSite site;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ==================== 构造函数 ====================

    public CheckinLocation() {
    }

    public CheckinLocation(String name, Double latitude, Double longitude, Double allowedRadius, WorkSite site) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadius = allowedRadius != null ? allowedRadius : 100.0;
        this.site = site;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ==================== Getters / Setters ====================

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

    public WorkSite getSite() {
        return site;
    }

    public void setSite(WorkSite site) {
        this.site = site;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
