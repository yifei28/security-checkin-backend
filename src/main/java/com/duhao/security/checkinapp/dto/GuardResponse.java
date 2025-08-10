package com.duhao.security.checkinapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GuardResponse {
    private boolean success;
    private List<GuardData> data;

    public static class GuardData {
        private String id;
        private String name;
        @JsonProperty("phoneNumber")
        private String phoneNumber;
        @JsonProperty("employeeId")
        private String employeeId;
        private SiteInfo site;
        @JsonProperty("isActive")
        private boolean isActive;
        @JsonProperty("createdAt")
        private String createdAt;

        public static class SiteInfo {
            private String id;
            private String name;

            public SiteInfo() {}

            public SiteInfo(String id, String name) {
                this.id = id;
                this.name = name;
            }

            // Getters and setters
            public String getId() { return id; }
            public void setId(String id) { this.id = id; }

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }

        // Constructors
        public GuardData() {}

        public GuardData(String id, String name, String phoneNumber, String employeeId,
                        SiteInfo site, boolean isActive, String createdAt) {
            this.id = id;
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.employeeId = employeeId;
            this.site = site;
            this.isActive = isActive;
            this.createdAt = createdAt;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

        public SiteInfo getSite() { return site; }
        public void setSite(SiteInfo site) { this.site = site; }

        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // Constructors
    public GuardResponse() {}

    public GuardResponse(boolean success, List<GuardData> data) {
        this.success = success;
        this.data = data;
    }

    // Static factory methods
    public static GuardResponse success(List<GuardData> data) {
        return new GuardResponse(true, data);
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<GuardData> getData() { return data; }
    public void setData(List<GuardData> data) { this.data = data; }
}