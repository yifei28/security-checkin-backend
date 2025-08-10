package com.duhao.security.checkinapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class CheckinRecordResponse {
    private boolean success;
    private List<CheckinRecordData> data;
    private PaginationInfo pagination;
    private CheckinStatistics statistics;

    public static class CheckinRecordData {
        private String id;
        @JsonProperty("guardId")
        private String guardId;
        @JsonProperty("siteId")
        private String siteId;
        private String timestamp;
        private LocationInfo location;
        @JsonProperty("faceImageUrl")
        private String faceImageUrl;
        private String status;
        private String reason;

        public static class LocationInfo {
            private Double lat;
            private Double lng;

            public LocationInfo() {}

            public LocationInfo(Double lat, Double lng) {
                this.lat = lat;
                this.lng = lng;
            }

            // Getters and setters
            public Double getLat() { return lat; }
            public void setLat(Double lat) { this.lat = lat; }
            
            public Double getLng() { return lng; }
            public void setLng(Double lng) { this.lng = lng; }
        }

        // Constructors
        public CheckinRecordData() {}

        public CheckinRecordData(String id, String guardId, String siteId, String timestamp,
                                LocationInfo location, String faceImageUrl, String status, String reason) {
            this.id = id;
            this.guardId = guardId;
            this.siteId = siteId;
            this.timestamp = timestamp;
            this.location = location;
            this.faceImageUrl = faceImageUrl;
            this.status = status;
            this.reason = reason;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getGuardId() { return guardId; }
        public void setGuardId(String guardId) { this.guardId = guardId; }

        public String getSiteId() { return siteId; }
        public void setSiteId(String siteId) { this.siteId = siteId; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public LocationInfo getLocation() { return location; }
        public void setLocation(LocationInfo location) { this.location = location; }

        public String getFaceImageUrl() { return faceImageUrl; }
        public void setFaceImageUrl(String faceImageUrl) { this.faceImageUrl = faceImageUrl; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class PaginationInfo {
        private long total;
        private int page;
        @JsonProperty("pageSize")
        private int pageSize;
        @JsonProperty("totalPages")
        private int totalPages;

        public PaginationInfo() {}

        public PaginationInfo(long total, int page, int pageSize, int totalPages) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = totalPages;
        }

        // Getters and setters
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }

    // Constructors
    public CheckinRecordResponse() {}

    public CheckinRecordResponse(boolean success, List<CheckinRecordData> data, PaginationInfo pagination) {
        this.success = success;
        this.data = data;
        this.pagination = pagination;
    }

    public CheckinRecordResponse(boolean success, List<CheckinRecordData> data, PaginationInfo pagination, CheckinStatistics statistics) {
        this.success = success;
        this.data = data;
        this.pagination = pagination;
        this.statistics = statistics;
    }

    // Static factory methods
    public static CheckinRecordResponse success(List<CheckinRecordData> data, PaginationInfo pagination) {
        return new CheckinRecordResponse(true, data, pagination);
    }

    public static CheckinRecordResponse success(List<CheckinRecordData> data, PaginationInfo pagination, CheckinStatistics statistics) {
        return new CheckinRecordResponse(true, data, pagination, statistics);
    }

    public static CheckinRecordResponse success(List<CheckinRecordData> data) {
        return new CheckinRecordResponse(true, data, null);
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<CheckinRecordData> getData() { return data; }
    public void setData(List<CheckinRecordData> data) { this.data = data; }

    public PaginationInfo getPagination() { return pagination; }
    public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }

    public CheckinStatistics getStatistics() { return statistics; }
    public void setStatistics(CheckinStatistics statistics) { this.statistics = statistics; }
}