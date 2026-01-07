package com.duhao.security.checkinapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 随机抽查实体
 * 记录对保安的抽查任务及验证结果
 */
@Entity
@Table(name = "spot_check")
public class SpotCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 乐观锁版本号
     */
    @Version
    private Long version;

    /**
     * 关联的工作片段（1:N 关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_record_id", nullable = false)
    private CheckinRecord checkinRecord;

    /**
     * 抽查创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 抽查截止时间（创建时间 + 15分钟）
     */
    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;

    /**
     * 完成验证的时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 抽查状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SpotCheckStatus status = SpotCheckStatus.PENDING;

    /**
     * 触发类型（自动/手动）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private SpotCheckTriggerType triggerType = SpotCheckTriggerType.AUTOMATIC;

    /**
     * 验证时的纬度
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * 验证时的经度
     */
    @Column(name = "longitude")
    private Double longitude;

    /**
     * 人脸验证照片URL
     */
    @Column(name = "face_image_url", length = 500)
    private String faceImageUrl;

    /**
     * 失败原因（验证失败时记录）
     */
    @Column(name = "fail_reason", length = 500)
    private String failReason;

    // ==================== 构造函数 ====================

    public SpotCheck() {}

    /**
     * 创建抽查任务
     */
    public SpotCheck(CheckinRecord checkinRecord, SpotCheckTriggerType triggerType) {
        this.checkinRecord = checkinRecord;
        this.triggerType = triggerType;
        this.createdAt = LocalDateTime.now();
        this.deadline = this.createdAt.plusMinutes(15);
        this.status = SpotCheckStatus.PENDING;
    }

    // ==================== 业务方法 ====================

    /**
     * 标记为通过
     */
    public void markPassed(Double latitude, Double longitude, String faceImageUrl) {
        this.status = SpotCheckStatus.PASSED;
        this.completedAt = LocalDateTime.now();
        this.latitude = latitude;
        this.longitude = longitude;
        this.faceImageUrl = faceImageUrl;
    }

    /**
     * 标记为超时
     */
    public void markMissed() {
        this.status = SpotCheckStatus.MISSED;
    }

    /**
     * 是否已过期（超过截止时间且仍为待处理）
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(deadline) && status == SpotCheckStatus.PENDING;
    }

    /**
     * 获取剩余时间（秒）
     */
    public long getRemainingSeconds() {
        if (status != SpotCheckStatus.PENDING) {
            return 0;
        }
        long remaining = java.time.Duration.between(LocalDateTime.now(), deadline).getSeconds();
        return Math.max(0, remaining);
    }

    /**
     * 获取关联的保安（通过 checkinRecord）
     */
    public SecurityGuard getGuard() {
        return checkinRecord != null ? checkinRecord.getGuard() : null;
    }

    /**
     * 获取关联的工作地点（通过 checkinRecord）
     */
    public WorkSite getSite() {
        return checkinRecord != null ? checkinRecord.getSite() : null;
    }

    // ==================== Getters / Setters ====================

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public CheckinRecord getCheckinRecord() {
        return checkinRecord;
    }

    public void setCheckinRecord(CheckinRecord checkinRecord) {
        this.checkinRecord = checkinRecord;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public SpotCheckStatus getStatus() {
        return status;
    }

    public void setStatus(SpotCheckStatus status) {
        this.status = status;
    }

    public SpotCheckTriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(SpotCheckTriggerType triggerType) {
        this.triggerType = triggerType;
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

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }
}
