package com.duhao.security.checkinapp.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * 保安员实体类
 * 继承自 Employee，包含保安特有的字段
 */
@Entity
@DiscriminatorValue("GUARD")
public class SecurityGuard extends Employee {

    @ManyToOne
    @JoinColumn(name = "site_id")
    private WorkSite site;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private GuardRole role = GuardRole.TEAM_MEMBER;

    @Column(name = "height")
    private Integer height;

    public SecurityGuard() {
        super();
    }

    public SecurityGuard(String name, String phoneNumber, WorkSite site) {
        super(name, phoneNumber);
        this.site = site;
    }

    public SecurityGuard(String name, String phoneNumber, WorkSite site, LocalDate birthDate) {
        super(name, phoneNumber, birthDate);
        this.site = site;
    }

    public SecurityGuard(String name, String phoneNumber, WorkSite site, LocalDate birthDate, Integer height) {
        super(name, phoneNumber, birthDate);
        this.site = site;
        this.height = height;
    }

    // ==================== Getters / Setters ====================

    public WorkSite getSite() {
        return site;
    }

    public void setSite(WorkSite site) {
        this.site = site;
    }

    public GuardRole getRole() {
        return role;
    }

    public void setRole(GuardRole role) {
        this.role = role;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}