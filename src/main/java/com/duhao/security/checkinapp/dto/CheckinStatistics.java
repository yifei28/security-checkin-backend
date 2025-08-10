package com.duhao.security.checkinapp.dto;

public class CheckinStatistics {
    private long totalRecords;
    private long successCount;
    private long failedCount;
    private long pendingCount;
    private int successRate;

    public CheckinStatistics() {}

    public CheckinStatistics(long totalRecords, long successCount, long failedCount, long pendingCount) {
        this.totalRecords = totalRecords;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.pendingCount = pendingCount;
        this.successRate = totalRecords > 0 ? (int) Math.round((double) successCount / totalRecords * 100) : 0;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public int getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(int successRate) {
        this.successRate = successRate;
    }
}