package com.duhao.security.checkinapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AdminResponse {
    private boolean success;
    private List<AdminData> data;
    private PaginationInfo pagination;

    public static class AdminData {
        private Long id;
        private String username;
        @JsonProperty("isSuperAdmin")
        private boolean isSuperAdmin;

        public AdminData() {}

        public AdminData(Long id, String username, boolean isSuperAdmin) {
            this.id = id;
            this.username = username;
            this.isSuperAdmin = isSuperAdmin;
        }

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public boolean isSuperAdmin() { return isSuperAdmin; }
        public void setSuperAdmin(boolean superAdmin) { isSuperAdmin = superAdmin; }
    }

    // Constructors
    public AdminResponse() {}

    public AdminResponse(boolean success, List<AdminData> data) {
        this.success = success;
        this.data = data;
    }

    public AdminResponse(boolean success, List<AdminData> data, PaginationInfo pagination) {
        this.success = success;
        this.data = data;
        this.pagination = pagination;
    }

    // Static factory methods
    public static AdminResponse success(List<AdminData> data) {
        return new AdminResponse(true, data);
    }

    public static AdminResponse success(List<AdminData> data, PaginationInfo pagination) {
        return new AdminResponse(true, data, pagination);
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<AdminData> getData() { return data; }
    public void setData(List<AdminData> data) { this.data = data; }

    public PaginationInfo getPagination() { return pagination; }
    public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
}
