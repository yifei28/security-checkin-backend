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
     * 被抽查的保安
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guard_id", nullable = false)
    private SecurityGuard guard;

    /**
     * 抽查时应在的工作地点
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private WorkSite site;

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
    @Column(name = "face_image_url")
    private String faceImageUrl;

    /**
     * 失败或缺勤原因
     */
    @Column(name = "reason")
    private String reason;

    /**
     * 是否已发送通知
     */
    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent = false;

    /**
     * 触发类型（自动/手动）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private SpotCheckTriggerType triggerType = SpotCheckTriggerType.AUTOMATIC;

    /**
     * 手动触发的管理员（仅手动触发时有值）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_admin_id")
    private Admin triggeredByAdmin;

    /**
     * 计划触发时间（用于定时任务）
     */
    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    // ==================== 构造函数 ====================

    public SpotCheck() {}

    public SpotCheck(SecurityGuard guard, WorkSite site, SpotCheckTriggerType triggerType) {
        this.guard = guard;
        this.site = site;
        this.triggerType = triggerType;
        this.createdAt = LocalDateTime.now();
        this.deadline = this.createdAt.plusMinutes(15);
    }

    public SpotCheck(SecurityGuard guard, WorkSite site, SpotCheckTriggerType triggerType, Admin triggeredByAdmin) {
        this(guard, site, triggerType);
        this.triggeredByAdmin = triggeredByAdmin;
    }

    // ==================== 业务方法 ====================

    /**
     * 标记为已完成
     */
    public void markCompleted(Double latitude, Double longitude, String faceImageUrl) {
        this.status = SpotCheckStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.latitude = latitude;
        this.longitude = longitude;
        this.faceImageUrl = faceImageUrl;
    }

    /**
     * 标记为缺勤（超时）
     */
    public void markMissed() {
        this.status = SpotCheckStatus.MISSED;
        this.reason = "超时未完成验证";
    }

    /**
     * 标记为验证失败
     */
    public void markFailed(String reason) {
        this.status = SpotCheckStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.reason = reason;
    }

    /**
     * 取消抽查
     */
    public void cancel(String reason) {
        this.status = SpotCheckStatus.CANCELLED;
        this.reason = reason;
    }

    /**
     * 是否已过期
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

    // ==================== Getters / Setters ====================

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public SpotCheckTriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(SpotCheckTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public Admin getTriggeredByAdmin() {
        return triggeredByAdmin;
    }

    public void setTriggeredByAdmin(Admin triggeredByAdmin) {
        this.triggeredByAdmin = triggeredByAdmin;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
}
