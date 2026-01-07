package com.duhao.security.checkinapp.dto;

import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.WorkStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作历史记录响应
 */
public class WorkHistoryResponse {

    private boolean success;
    private List<WorkHistoryItem> data;
    private Map<String, Object> pagination;
    private String message;

    // ==================== 静态工厂方法 ====================

    public static WorkHistoryResponse success(List<WorkHistoryItem> data, long total, int page, int pageSize) {
        WorkHistoryResponse response = new WorkHistoryResponse();
        response.success = true;
        response.data = data;
        response.pagination = Map.of(
                "total", total,
                "page", page,
                "pageSize", pageSize,
                "totalPages", (int) Math.ceil((double) total / pageSize)
        );
        return response;
    }

    public static WorkHistoryResponse error(String message) {
        WorkHistoryResponse response = new WorkHistoryResponse();
        response.success = false;
        response.message = message;
        return response;
    }

    // ==================== 内部类：历史记录项 ====================

    public static class WorkHistoryItem {
        private Long id;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private String statusName;
        private Long durationMinutes;
        private String siteName;

        public static WorkHistoryItem fromEntity(CheckinRecord record) {
            WorkHistoryItem item = new WorkHistoryItem();
            item.id = record.getId();
            item.startTime = record.getStartTime();
            item.endTime = record.getEndTime();

            WorkStatus workStatus = record.getStatus();
            item.status = workStatus.name();
            item.statusName = workStatus.getDisplayName();

            item.durationMinutes = record.getDurationMinutes();
            item.siteName = record.getSite() != null ? record.getSite().getName() : null;
            return item;
        }

        // Getters
        public Long getId() { return id; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public String getStatus() { return status; }
        public String getStatusName() { return statusName; }
        public Long getDurationMinutes() { return durationMinutes; }
        public String getSiteName() { return siteName; }
    }

    // ==================== Getters ====================

    public boolean isSuccess() { return success; }
    public List<WorkHistoryItem> getData() { return data; }
    public Map<String, Object> getPagination() { return pagination; }
    public String getMessage() { return message; }
}
