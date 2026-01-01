package com.duhao.security.checkinapp.entity;

/**
 * 抽查触发类型枚举
 */
public enum SpotCheckTriggerType {
    AUTOMATIC("自动触发"),
    MANUAL("手动触发");

    private final String displayName;

    SpotCheckTriggerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
