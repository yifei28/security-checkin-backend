package com.duhao.security.checkinapp.entity;

public enum GuardRole {
    TEAM_MEMBER("队员"),
    TEAM_LEADER("队长");
    
    private final String displayName;
    
    GuardRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}