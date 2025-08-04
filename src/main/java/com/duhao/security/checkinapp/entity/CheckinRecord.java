package com.duhao.security.checkinapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
public class CheckinRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String guardName;
    private Double latitude;
    private Double longitude;
    private String phoneNumber;
    private String employeeId;
    private LocalDateTime timestamp;
    private String period;
    private LocalDate date;

    public CheckinRecord() {}

    public CheckinRecord(String guardName, Double latitude, Double longitude, String phoneNumber, String employeeId) {
        this.guardName = guardName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phoneNumber = phoneNumber;
        this.employeeId = employeeId;
    }

    // Getters and setters
    public Long getId() { return id; }

    public String getGuardName() { return guardName; }
    public void setGuardName(String guardName) { this.guardName = guardName; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
}
