package com.duhao.security.checkinapp.dto;

import java.time.LocalDateTime;

/**
 * 上岗/下岗操作响应
 */
public class WorkResponse {

    private boolean success;
    private String message;
    private WorkData data;

    public static WorkResponse success(String message, WorkData data) {
        WorkResponse response = new WorkResponse();
        response.success = true;
        response.message = message;
        response.data = data;
        return response;
    }

    public static WorkResponse error(String message) {
        WorkResponse response = new WorkResponse();
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

    public WorkData getData() {
        return data;
    }

    public void setData(WorkData data) {
        this.data = data;
    }

    /**
     * 工作片段数据
     */
    public static class WorkData {
        private Long sessionId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private Long durationMinutes;
        private String siteName;

        // Getters and Setters

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

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Long durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public String getSiteName() {
            return siteName;
        }

        public void setSiteName(String siteName) {
            this.siteName = siteName;
        }
    }
}
