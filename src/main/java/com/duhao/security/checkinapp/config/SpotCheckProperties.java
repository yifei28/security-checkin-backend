package com.duhao.security.checkinapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 抽查配置属性
 *
 * 动态间隔策略：根据已抽查次数调整间隔
 * - 0次已抽 → 间隔 90-150分钟
 * - 1次已抽 → 间隔 120-200分钟
 * - 2次已抽 → 间隔 150-250分钟
 * - 3次已抽 → 不再安排
 */
@Configuration
@ConfigurationProperties(prefix = "spotcheck")
public class SpotCheckProperties {

    /**
     * 每个工作片段最多抽查次数
     */
    private int maxChecksPerSession = 3;

    /**
     * 阶段1（0次已抽）最小间隔（分钟）
     */
    private int stage1MinInterval = 90;

    /**
     * 阶段1（0次已抽）最大间隔（分钟）
     */
    private int stage1MaxInterval = 150;

    /**
     * 阶段2（1次已抽）最小间隔（分钟）
     */
    private int stage2MinInterval = 120;

    /**
     * 阶段2（1次已抽）最大间隔（分钟）
     */
    private int stage2MaxInterval = 200;

    /**
     * 阶段3（2次已抽）最小间隔（分钟）
     */
    private int stage3MinInterval = 150;

    /**
     * 阶段3（2次已抽）最大间隔（分钟）
     */
    private int stage3MaxInterval = 250;

    // 以下保留用于兼容，但不再使用
    @Deprecated
    private int avgIntervalMinutes = 120;
    @Deprecated
    private int minIntervalMinutes = 30;
    @Deprecated
    private int maxIntervalMinutes = 240;

    /**
     * 抽查响应时间（分钟）
     */
    private int responseMinutes = 15;

    /**
     * 工作片段超时时间（小时）
     */
    private int sessionTimeoutHours = 16;

    /**
     * 最小工作时长（小时），下岗前必须满足
     */
    private int minWorkHours = 1;

    /**
     * 最后1小时不安排新抽查
     */
    private boolean lastHourNoCheck = true;

    // Getters and Setters

    public int getMaxChecksPerSession() {
        return maxChecksPerSession;
    }

    public void setMaxChecksPerSession(int maxChecksPerSession) {
        this.maxChecksPerSession = maxChecksPerSession;
    }

    public int getStage1MinInterval() {
        return stage1MinInterval;
    }

    public void setStage1MinInterval(int stage1MinInterval) {
        this.stage1MinInterval = stage1MinInterval;
    }

    public int getStage1MaxInterval() {
        return stage1MaxInterval;
    }

    public void setStage1MaxInterval(int stage1MaxInterval) {
        this.stage1MaxInterval = stage1MaxInterval;
    }

    public int getStage2MinInterval() {
        return stage2MinInterval;
    }

    public void setStage2MinInterval(int stage2MinInterval) {
        this.stage2MinInterval = stage2MinInterval;
    }

    public int getStage2MaxInterval() {
        return stage2MaxInterval;
    }

    public void setStage2MaxInterval(int stage2MaxInterval) {
        this.stage2MaxInterval = stage2MaxInterval;
    }

    public int getStage3MinInterval() {
        return stage3MinInterval;
    }

    public void setStage3MinInterval(int stage3MinInterval) {
        this.stage3MinInterval = stage3MinInterval;
    }

    public int getStage3MaxInterval() {
        return stage3MaxInterval;
    }

    public void setStage3MaxInterval(int stage3MaxInterval) {
        this.stage3MaxInterval = stage3MaxInterval;
    }

    /**
     * 根据已完成抽查次数获取间隔范围
     * @param completedChecks 已完成的抽查次数
     * @return [minInterval, maxInterval]，如果已达上限返回 null
     */
    public int[] getIntervalRange(int completedChecks) {
        if (completedChecks >= maxChecksPerSession) {
            return null; // 已达上限，不再安排
        }
        return switch (completedChecks) {
            case 0 -> new int[]{stage1MinInterval, stage1MaxInterval};
            case 1 -> new int[]{stage2MinInterval, stage2MaxInterval};
            default -> new int[]{stage3MinInterval, stage3MaxInterval};
        };
    }

    // 以下保留用于兼容
    @Deprecated
    public int getAvgIntervalMinutes() {
        return avgIntervalMinutes;
    }

    @Deprecated
    public void setAvgIntervalMinutes(int avgIntervalMinutes) {
        this.avgIntervalMinutes = avgIntervalMinutes;
    }

    @Deprecated
    public int getMinIntervalMinutes() {
        return minIntervalMinutes;
    }

    @Deprecated
    public void setMinIntervalMinutes(int minIntervalMinutes) {
        this.minIntervalMinutes = minIntervalMinutes;
    }

    @Deprecated
    public int getMaxIntervalMinutes() {
        return maxIntervalMinutes;
    }

    @Deprecated
    public void setMaxIntervalMinutes(int maxIntervalMinutes) {
        this.maxIntervalMinutes = maxIntervalMinutes;
    }

    public int getResponseMinutes() {
        return responseMinutes;
    }

    public void setResponseMinutes(int responseMinutes) {
        this.responseMinutes = responseMinutes;
    }

    public int getSessionTimeoutHours() {
        return sessionTimeoutHours;
    }

    public void setSessionTimeoutHours(int sessionTimeoutHours) {
        this.sessionTimeoutHours = sessionTimeoutHours;
    }

    public int getMinWorkHours() {
        return minWorkHours;
    }

    public void setMinWorkHours(int minWorkHours) {
        this.minWorkHours = minWorkHours;
    }

    public boolean isLastHourNoCheck() {
        return lastHourNoCheck;
    }

    public void setLastHourNoCheck(boolean lastHourNoCheck) {
        this.lastHourNoCheck = lastHourNoCheck;
    }
}
