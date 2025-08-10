package com.duhao.security.checkinapp.entity;

/**
 * 签到状态枚举
 */
public enum CheckinStatus {
    SUCCESS("success", "签到成功"),
    FAILED("failed", "签到失败"), 
    PENDING("pending", "待处理");

    private final String value;
    private final String description;

    CheckinStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static CheckinStatus fromValue(String value) {
        for (CheckinStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status value: " + value);
    }
}