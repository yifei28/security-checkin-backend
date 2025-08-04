package com.duhao.security.checkinapp.entity;

import jakarta.persistence.*;
import org.apache.commons.lang3.RandomStringUtils;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
public class SecurityGuard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String employeeId;

    @Column(unique = true)
    private String openId;

    private String phoneNumber;

    @ManyToOne
    @JoinColumn(name = "site_id")  // 数据库中的外键字段 site_id
    private WorkSite site;

    public SecurityGuard() {}

    public SecurityGuard(String name, String phoneNumber, WorkSite site) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.site = site;
    }

    // 在持久化后，根据自增 id、当前日期和随机后缀生成唯一 employeeId
    @PostPersist
    private void assignEmployeeId() {
        // 1. 日期前缀（YYYYMMDD）
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        // 2. 7 位自增序号，不足前置补零
        String seq = String.format("%07d", this.id);
        // 3. 4 位随机数字后缀，增强不可预测性
        String randomSuffix = RandomStringUtils.randomAlphanumeric(6);
        // 4. 最终拼接格式，例如：20250715-0000123-4829
        this.employeeId = String.format("%s-%s-%s", date, seq, randomSuffix);
    }

    // ---------- Getter / Setter ----------

    public Long getId() {
        return id;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public WorkSite getSite() {
        return site;
    }

    public void setSite(WorkSite site) {
        this.site = site;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }
}