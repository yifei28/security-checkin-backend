package com.duhao.security.checkinapp.entity;

/**
 * 抽查状态枚举
 */
public enum SpotCheckStatus {
    PENDING("待处理"),
    COMPLETED("已完成"),
    MISSED("缺勤"),
    FAILED("验证失败"),
    CANCELLED("已取消");

    private final String displayName;

    SpotCheckStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
