package com.duhao.security.checkinapp.dto;

import java.time.LocalDateTime;

/**
 * 工作状态响应
 */
public class WorkStatusResponse {

    private boolean success;
    private String message;
    private WorkStatusData data;

    public static WorkStatusResponse success(WorkStatusData data) {
        WorkStatusResponse response = new WorkStatusResponse();
        response.success = true;
        response.data = data;
        return response;
    }

    public static WorkStatusResponse error(String message) {
        WorkStatusResponse response = new WorkStatusResponse();
        response.success = false;
        response.message = message;
        return response;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public WorkStatusData getData() {
        return data;
    }

    public void setData(WorkStatusData data) {
        this.data = data;
    }

    /**
     * 工作状态数据
     */
    public static class WorkStatusData {
        private boolean isWorking;
        private Long sessionId;
        private LocalDateTime startTime;
        private long activeMinutes;
        private String siteName;
        private PendingSpotCheck pendingSpotCheck;

        // Getters and Setters

        public boolean isWorking() {
            return isWorking;
        }

        public void setWorking(boolean working) {
            isWorking = working;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public long getActiveMinutes() {
            return activeMinutes;
        }

        public void setActiveMinutes(long activeMinutes) {
            this.activeMinutes = activeMinutes;
        }

        public String getSiteName() {
            return siteName;
        }

        public void setSiteName(String siteName) {
            this.siteName = siteName;
        }

        public PendingSpotCheck getPendingSpotCheck() {
            return pendingSpotCheck;
        }

        public void setPendingSpotCheck(PendingSpotCheck pendingSpotCheck) {
            this.pendingSpotCheck = pendingSpotCheck;
        }
    }

    /**
     * 待处理抽查信息
     */
    public static class PendingSpotCheck {
        private Long id;
        private LocalDateTime deadline;
        private long remainingSeconds;

        public PendingSpotCheck(Long id, LocalDateTime deadline, long remainingSeconds) {
            this.id = id;
            this.deadline = deadline;
            this.remainingSeconds = remainingSeconds;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public LocalDateTime getDeadline() {
            return deadline;
        }

        public void setDeadline(LocalDateTime deadline) {
            this.deadline = deadline;
        }

        public long getRemainingSeconds() {
            return remainingSeconds;
        }

        public void setRemainingSeconds(long remainingSeconds) {
            this.remainingSeconds = remainingSeconds;
        }
    }
}
