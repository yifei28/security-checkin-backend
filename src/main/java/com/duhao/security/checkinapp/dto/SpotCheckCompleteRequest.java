package com.duhao.security.checkinapp.dto;

/**
 * 完成抽查请求
 */
public class SpotCheckCompleteRequest {

    private Long spotCheckId;
    private Double latitude;
    private Double longitude;
    private String faceImageUrl;

    public Long getSpotCheckId() {
        return spotCheckId;
    }

    public void setSpotCheckId(Long spotCheckId) {
        this.spotCheckId = spotCheckId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getFaceImageUrl() {
        return faceImageUrl;
    }

    public void setFaceImageUrl(String faceImageUrl) {
        this.faceImageUrl = faceImageUrl;
    }
}
