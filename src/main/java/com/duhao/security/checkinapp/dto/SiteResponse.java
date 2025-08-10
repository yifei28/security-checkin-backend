package com.duhao.security.checkinapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SiteResponse {
    private boolean success;
    private List<SiteData> data;

    public static class SiteData {
        private String id;
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

        // Constructors
        public SiteData() {}

        public SiteData(String id, String name, Double latitude, Double longitude,
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

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

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
    }

    // Constructors
    public SiteResponse() {}

    public SiteResponse(boolean success, List<SiteData> data) {
        this.success = success;
        this.data = data;
    }

    // Static factory methods
    public static SiteResponse success(List<SiteData> data) {
        return new SiteResponse(true, data);
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<SiteData> getData() { return data; }
    public void setData(List<SiteData> data) { this.data = data; }
}