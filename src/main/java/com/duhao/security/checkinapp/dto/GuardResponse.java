package com.duhao.security.checkinapp.dto;

import com.duhao.security.checkinapp.entity.EmploymentStatus;
import com.duhao.security.checkinapp.entity.Gender;
import com.duhao.security.checkinapp.entity.GuardRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
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
        @JsonProperty("role")
        private GuardRole role;
        @JsonProperty("isActive")
        private boolean isActive;
        @JsonProperty("createdAt")
        private String createdAt;

        // 新增字段
        @JsonProperty("birthDate")
        private LocalDate birthDate;
        @JsonProperty("age")
        private Integer age;
        @JsonProperty("height")
        private Integer height;
        @JsonProperty("idCardNumber")
        private String idCardNumber;
        @JsonProperty("gender")
        private Gender gender;
        @JsonProperty("employmentStatus")
        private EmploymentStatus employmentStatus;
        @JsonProperty("originalHireDate")
        private LocalDate originalHireDate;
        @JsonProperty("latestHireDate")
        private LocalDate latestHireDate;
        @JsonProperty("resignDate")
        private LocalDate resignDate;

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
                        SiteInfo site, GuardRole role, boolean isActive, String createdAt,
                        LocalDate birthDate, Integer age, Integer height, String idCardNumber,
                        Gender gender, EmploymentStatus employmentStatus,
                        LocalDate originalHireDate, LocalDate latestHireDate, LocalDate resignDate) {
            this.id = id;
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.employeeId = employeeId;
            this.site = site;
            this.role = role;
            this.isActive = isActive;
            this.createdAt = createdAt;
            this.birthDate = birthDate;
            this.age = age;
            this.height = height;
            this.idCardNumber = idCardNumber;
            this.gender = gender;
            this.employmentStatus = employmentStatus;
            this.originalHireDate = originalHireDate;
            this.latestHireDate = latestHireDate;
            this.resignDate = resignDate;
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

        public GuardRole getRole() { return role; }
        public void setRole(GuardRole role) { this.role = role; }

        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        // 新增字段的 Getters and setters
        public LocalDate getBirthDate() { return birthDate; }
        public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }

        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }

        public String getIdCardNumber() { return idCardNumber; }
        public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }

        public Gender getGender() { return gender; }
        public void setGender(Gender gender) { this.gender = gender; }

        public EmploymentStatus getEmploymentStatus() { return employmentStatus; }
        public void setEmploymentStatus(EmploymentStatus employmentStatus) { this.employmentStatus = employmentStatus; }

        public LocalDate getOriginalHireDate() { return originalHireDate; }
        public void setOriginalHireDate(LocalDate originalHireDate) { this.originalHireDate = originalHireDate; }

        public LocalDate getLatestHireDate() { return latestHireDate; }
        public void setLatestHireDate(LocalDate latestHireDate) { this.latestHireDate = latestHireDate; }

        public LocalDate getResignDate() { return resignDate; }
        public void setResignDate(LocalDate resignDate) { this.resignDate = resignDate; }
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