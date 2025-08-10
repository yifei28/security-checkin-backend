package com.duhao.security.checkinapp.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class WorkSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double latitude;
    private double longitude;

    private double allowedRadiusMeters = 300; // 默认300米

    @OneToMany(mappedBy = "site")
    private List<SecurityGuard> guards;

    public WorkSite() {}

    public WorkSite(String name, double latitude, double longitude, double radius) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadiusMeters = radius;
    }

    public boolean isInRange(double lat, double lon) {
        double distance = distanceInMeters(lat, lon, this.latitude, this.longitude);
        return distance <= this.allowedRadiusMeters;
    }

    private double distanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // 地球半径（米）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Getter / Setter
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getAllowedRadiusMeters() { return allowedRadiusMeters; }
    public void setAllowedRadiusMeters(double allowedRadiusMeters) { this.allowedRadiusMeters = allowedRadiusMeters; }
}
