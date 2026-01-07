package com.duhao.security.checkinapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SiteResponse {
    private boolean success;
    private List<SiteData> data;
    private PaginationInfo pagination;

    public static class SiteData {
        private Long id;
        private String name;
        private Double latitude;
        private Double longitude;
        @JsonProperty("allowedRadiusMeters")
        private Double allowedRadiusMeters;
        @JsonProperty("assignedGuardIds")
        private List<String> assignedGuardIds;
        @JsonProperty("isActive")
        private boolean isActive;
        @JsonProperty("createdAt")
        private String createdAt;
        // 新增统计字段
        @JsonProperty("locationCount")
        private int locationCount;       // 签到地点数量
        @JsonProperty("guardCount")
        private int guardCount;          // 保安数量
        @JsonProperty("onDutyNow")
        private int onDutyNow;           // 当前在岗人数

        // Constructors
        public SiteData() {}

        public SiteData(Long id, String name, Double latitude, Double longitude,
                       Double allowedRadiusMeters, List<String> assignedGuardIds,
                       boolean isActive, String createdAt) {
            this.id = id;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.allowedRadiusMeters = allowedRadiusMeters;
            this.assignedGuardIds = assignedGuardIds;
            this.isActive = isActive;
            this.createdAt = createdAt;
        }

        public SiteData(Long id, String name, Double latitude, Double longitude,
                       Double allowedRadiusMeters, List<String> assignedGuardIds,
                       boolean isActive, String createdAt,
                       int locationCount, int guardCount, int onDutyNow) {
            this.id = id;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.allowedRadiusMeters = allowedRadiusMeters;
            this.assignedGuardIds = assignedGuardIds;
            this.isActive = isActive;
            this.createdAt = createdAt;
            this.locationCount = locationCount;
            this.guardCount = guardCount;
            this.onDutyNow = onDutyNow;
        }

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }

        public Double getAllowedRadiusMeters() { return allowedRadiusMeters; }
        public void setAllowedRadiusMeters(Double allowedRadiusMeters) { this.allowedRadiusMeters = allowedRadiusMeters; }

        public List<String> getAssignedGuardIds() { return assignedGuardIds; }
        public void setAssignedGuardIds(List<String> assignedGuardIds) { this.assignedGuardIds = assignedGuardIds; }

        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public int getLocationCount() { return locationCount; }
        public void setLocationCount(int locationCount) { this.locationCount = locationCount; }

        public int getGuardCount() { return guardCount; }
        public void setGuardCount(int guardCount) { this.guardCount = guardCount; }

        public int getOnDutyNow() { return onDutyNow; }
        public void setOnDutyNow(int onDutyNow) { this.onDutyNow = onDutyNow; }
    }

    // Constructors
    public SiteResponse() {}

    public SiteResponse(boolean success, List<SiteData> data) {
        this.success = success;
        this.data = data;
    }

    public SiteResponse(boolean success, List<SiteData> data, PaginationInfo pagination) {
        this.success = success;
        this.data = data;
        this.pagination = pagination;
    }

    // Static factory methods
    public static SiteResponse success(List<SiteData> data) {
        return new SiteResponse(true, data);
    }

    public static SiteResponse success(List<SiteData> data, PaginationInfo pagination) {
        return new SiteResponse(true, data, pagination);
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<SiteData> getData() { return data; }
    public void setData(List<SiteData> data) { this.data = data; }

    public PaginationInfo getPagination() { return pagination; }
    public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
}