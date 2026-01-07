package com.duhao.security.checkinapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作片段记录
 * 记录保安的上岗-下岗周期
 */
@Entity
@Table(name = "checkin_record")
public class CheckinRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 乐观锁版本号
     */
    @Version
    private Long version;

    /**
     * 关联保安
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guard_id", nullable = false)
    private SecurityGuard guard;

    /**
     * 关联工作地点
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private WorkSite site;

    // ==================== 上岗信息 ====================

    /**
     * 上岗时间
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 上岗位置-纬度
     */
    @Column(name = "start_latitude", nullable = false)
    private Double startLatitude;

    /**
     * 上岗位置-经度
     */
    @Column(name = "start_longitude", nullable = false)
    private Double startLongitude;

    /**
     * 上岗人脸照片URL
     */
    @Column(name = "start_face_image_url", length = 500)
    private String startFaceImageUrl;

    // ==================== 下岗信息 (null = 在岗中) ====================

    /**
     * 下岗时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 下岗位置-纬度
     */
    @Column(name = "end_latitude")
    private Double endLatitude;

    /**
     * 下岗位置-经度
     */
    @Column(name = "end_longitude")
    private Double endLongitude;

    /**
     * 下岗人脸照片URL
     */
    @Column(name = "end_face_image_url", length = 500)
    private String endFaceImageUrl;

    // ==================== 状态 ====================

    /**
     * 工作状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkStatus status;

    /**
     * 备注/原因（如超时原因）
     */
    @Column(length = 500)
    private String reason;

    // ==================== 冗余统计字段（下岗时计算） ====================

    /**
     * 工作时长（分钟）
     */
    @Column(name = "duration_minutes")
    private Long durationMinutes;

    /**
     * 抽查总次数
     */
    @Column(name = "spot_check_total")
    private Integer spotCheckTotal;

    /**
     * 抽查通过次数
     */
    @Column(name = "spot_check_passed")
    private Integer spotCheckPassed;

    // ==================== 构造函数 ====================

    public CheckinRecord() {}

    /**
     * 上岗时使用的构造函数
     */
    public CheckinRecord(SecurityGuard guard, WorkSite site,
                         Double startLatitude, Double startLongitude,
                         String startFaceImageUrl) {
        this.guard = guard;
        this.site = site;
        this.startTime = LocalDateTime.now();
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.startFaceImageUrl = startFaceImageUrl;
        this.status = WorkStatus.ACTIVE;
        this.spotCheckTotal = 0;
        this.spotCheckPassed = 0;
    }

    // ==================== 业务方法 ====================

    /**
     * 正常下岗
     */
    public void clockOut(Double endLatitude, Double endLongitude, String endFaceImageUrl) {
        this.endTime = LocalDateTime.now();
        this.endLatitude = endLatitude;
        this.endLongitude = endLongitude;
        this.endFaceImageUrl = endFaceImageUrl;
        this.status = WorkStatus.COMPLETED;
        this.calculateDuration();
    }

    /**
     * 超时自动下岗
     */
    public void timeout() {
        this.endTime = this.startTime.plusHours(16);
        this.status = WorkStatus.TIMEOUT;
        this.reason = "超过16小时未下岗，系统自动结束";
        this.calculateDuration();
    }

    /**
     * 计算工作时长
     */
    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            this.durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        }
    }

    /**
     * 更新抽查统计
     */
    public void updateSpotCheckStats(int total, int passed) {
        this.spotCheckTotal = total;
        this.spotCheckPassed = passed;
    }

    /**
     * 是否在岗
     */
    public boolean isActive() {
        return status == WorkStatus.ACTIVE;
    }

    /**
     * 获取在岗时长（分钟），用于在岗状态
     */
    public long getActiveMinutes() {
        if (startTime == null) return 0;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMinutes();
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Double getStartLatitude() {
        return startLatitude;
    }

    public void setStartLatitude(Double startLatitude) {
        this.startLatitude = startLatitude;
    }

    public Double getStartLongitude() {
        return startLongitude;
    }

    public void setStartLongitude(Double startLongitude) {
        this.startLongitude = startLongitude;
    }

    public String getStartFaceImageUrl() {
        return startFaceImageUrl;
    }

    public void setStartFaceImageUrl(String startFaceImageUrl) {
        this.startFaceImageUrl = startFaceImageUrl;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Double getEndLatitude() {
        return endLatitude;
    }

    public void setEndLatitude(Double endLatitude) {
        this.endLatitude = endLatitude;
    }

    public Double getEndLongitude() {
        return endLongitude;
    }

    public void setEndLongitude(Double endLongitude) {
        this.endLongitude = endLongitude;
    }

    public String getEndFaceImageUrl() {
        return endFaceImageUrl;
    }

    public void setEndFaceImageUrl(String endFaceImageUrl) {
        this.endFaceImageUrl = endFaceImageUrl;
    }

    public WorkStatus getStatus() {
        return status;
    }

    public void setStatus(WorkStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getSpotCheckTotal() {
        return spotCheckTotal;
    }

    public void setSpotCheckTotal(Integer spotCheckTotal) {
        this.spotCheckTotal = spotCheckTotal;
    }

    public Integer getSpotCheckPassed() {
        return spotCheckPassed;
    }

    public void setSpotCheckPassed(Integer spotCheckPassed) {
        this.spotCheckPassed = spotCheckPassed;
    }
}
