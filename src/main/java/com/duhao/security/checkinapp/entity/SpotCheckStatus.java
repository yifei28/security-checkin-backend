package com.duhao.security.checkinapp.entity;

/**
 * 抽查状态枚举（简化版）
 */
public enum SpotCheckStatus {
    PENDING("待处理"),
    PASSED("已通过"),
    MISSED("超时未响应");

    private final String displayName;

    SpotCheckStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
