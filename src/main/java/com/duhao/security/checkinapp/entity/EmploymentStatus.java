package com.duhao.security.checkinapp.entity;

/**
 * 员工在职状态枚举
 */
public enum EmploymentStatus {
    ACTIVE("在职"),
    PROBATION("试用期"),
    SUSPENDED("停职"),
    RESIGNED("离职"),
    RETIRED("退休");

    private final String description;

    EmploymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
