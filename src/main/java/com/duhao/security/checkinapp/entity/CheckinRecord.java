package com.duhao.security.checkinapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class CheckinRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联保安信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guard_id", nullable = false)
    private SecurityGuard guard;

    // 关联站点信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private WorkSite site;
    
    // 签到时间
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    // 签到位置
    @Column(nullable = false)
    private Double latitude;
    
    @Column(nullable = false)
    private Double longitude;
    
    // 人脸照片URL（可选）
    @Column(length = 500)
    private String faceImageUrl;
    
    // 签到状态
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CheckinStatus status;
    
    // 失败/待处理原因（可选）
    @Column(length = 500)
    private String reason;

    public CheckinRecord() {}

    public CheckinRecord(SecurityGuard guard, WorkSite site, Double latitude, Double longitude, 
                        LocalDateTime timestamp, String faceImageUrl, CheckinStatus status, String reason) {
        this.guard = guard;
        this.site = site;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.faceImageUrl = faceImageUrl;
        this.status = status;
        this.reason = reason;
    }

    // Getters and setters
    public Long getId() { 
        return id; 
    }

    public SecurityGuard getGuard() {
        return guard;
    }

    public void setGuard(SecurityGuard guard) {
        this.guard = guard;
    }

    public WorkSite getSite() {
        return site;
    }

    public void setSite(WorkSite site) {
        this.site = site;
    }

    public LocalDateTime getTimestamp() { 
        return timestamp; 
    }
    
    public void setTimestamp(LocalDateTime timestamp) { 
        this.timestamp = timestamp; 
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

    public CheckinStatus getStatus() {
        return status;
    }

    public void setStatus(CheckinStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
