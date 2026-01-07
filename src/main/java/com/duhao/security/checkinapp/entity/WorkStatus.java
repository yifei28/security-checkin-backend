package com.duhao.security.checkinapp.entity;

/**
 * 工作状态枚举
 */
public enum WorkStatus {
    ACTIVE("在岗中"),
    COMPLETED("已下岗"),
    TIMEOUT("超时下岗"),
    LEGACY("旧数据");

    private final String displayName;

    WorkStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
