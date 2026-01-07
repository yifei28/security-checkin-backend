package com.duhao.security.checkinapp.dto;

import java.time.LocalDateTime;

/**
 * 抽查响应
 */
public class SpotCheckResponse {

    private boolean success;
    private String message;
    private SpotCheckData data;

    public static SpotCheckResponse success(String message, SpotCheckData data) {
        SpotCheckResponse response = new SpotCheckResponse();
        response.success = true;
        response.message = message;
        response.data = data;
        return response;
    }

    public static SpotCheckResponse error(String message) {
        SpotCheckResponse response = new SpotCheckResponse();
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

    public SpotCheckData getData() {
        return data;
    }

    public void setData(SpotCheckData data) {
        this.data = data;
    }

    /**
     * 抽查数据
     */
    public static class SpotCheckData {
        private Long id;
        private LocalDateTime createdAt;
        private LocalDateTime deadline;
        private LocalDateTime completedAt;
        private String status;
        private long remainingSeconds;

        // Getters and Setters

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getRemainingSeconds() {
            return remainingSeconds;
        }

        public void setRemainingSeconds(long remainingSeconds) {
            this.remainingSeconds = remainingSeconds;
        }
    }
}
