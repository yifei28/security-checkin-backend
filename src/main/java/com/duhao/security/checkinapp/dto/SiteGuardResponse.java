package com.duhao.security.checkinapp.dto;

/**
 * 单位保安信息响应
 */
public class SiteGuardResponse {

    private Long id;
    private String name;
    private String employeeId;
    private String role;
    private String roleName;
    private String phoneNumber;     // 脱敏显示
    private boolean isOnDuty;
    private Long currentCheckinId;
    private int todayCheckinCount;

    public SiteGuardResponse() {
    }

    public SiteGuardResponse(Long id, String name, String employeeId, String role, String roleName,
                              String phoneNumber, boolean isOnDuty, Long currentCheckinId, int todayCheckinCount) {
        this.id = id;
        this.name = name;
        this.employeeId = employeeId;
        this.role = role;
        this.roleName = roleName;
        this.phoneNumber = phoneNumber;
        this.isOnDuty = isOnDuty;
        this.currentCheckinId = currentCheckinId;
        this.todayCheckinCount = todayCheckinCount;
    }

    /**
     * 手机号脱敏
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isOnDuty() {
        return isOnDuty;
    }

    public void setOnDuty(boolean onDuty) {
        isOnDuty = onDuty;
    }

    public Long getCurrentCheckinId() {
        return currentCheckinId;
    }

    public void setCurrentCheckinId(Long currentCheckinId) {
        this.currentCheckinId = currentCheckinId;
    }

    public int getTodayCheckinCount() {
        return todayCheckinCount;
    }

    public void setTodayCheckinCount(int todayCheckinCount) {
        this.todayCheckinCount = todayCheckinCount;
    }
}
