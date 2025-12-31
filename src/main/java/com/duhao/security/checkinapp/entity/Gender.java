package com.duhao.security.checkinapp.entity;

/**
 * 性别枚举
 */
public enum Gender {
    MALE("男"),
    FEMALE("女");

    private final String description;

    Gender(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
