package com.duhao.security.checkinapp.entity;

import jakarta.persistence.*;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * 员工抽象父类
 * 使用 SINGLE_TABLE 继承策略，所有员工类型存储在同一张表中
 * 通过 employee_type 列区分不同的员工子类
 */
@Entity
@Table(name = "employee")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "employee_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String employeeId;

    @Column(unique = true)
    private String openId;

    @Column(unique = true)
    private String phoneNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "id_card_number", unique = true, length = 18)
    private String idCardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status")
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "original_hire_date")
    private LocalDate originalHireDate;

    @Column(name = "latest_hire_date")
    private LocalDate latestHireDate;

    @Column(name = "resign_date")
    private LocalDate resignDate;

    public Employee() {}

    public Employee(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public Employee(String name, String phoneNumber, LocalDate birthDate) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
    }

    /**
     * 在持久化后，根据自增 id、当前日期和随机后缀生成唯一 employeeId
     * 格式：YYYYMMDD-7位序号-6位随机字符
     * 例如：20250715-0000123-Ab3xYz
     */
    @PostPersist
    private void assignEmployeeId() {
        if (this.employeeId == null) {
            String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String seq = String.format("%07d", this.id);
            String randomSuffix = RandomStringUtils.randomAlphanumeric(6);
            this.employeeId = String.format("%s-%s-%s", date, seq, randomSuffix);
        }
    }

    // ==================== Getters / Setters ====================

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

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(EmploymentStatus employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getOriginalHireDate() {
        return originalHireDate;
    }

    public void setOriginalHireDate(LocalDate originalHireDate) {
        this.originalHireDate = originalHireDate;
    }

    public LocalDate getLatestHireDate() {
        return latestHireDate;
    }

    public void setLatestHireDate(LocalDate latestHireDate) {
        this.latestHireDate = latestHireDate;
    }

    public LocalDate getResignDate() {
        return resignDate;
    }

    public void setResignDate(LocalDate resignDate) {
        this.resignDate = resignDate;
    }

    // ==================== 计算字段 ====================

    /**
     * 动态计算年龄（基于当前日期）
     * @return 当前年龄，如果生日未设置则返回null
     */
    @Transient
    public Integer getAge() {
        return birthDate != null ?
            Period.between(birthDate, LocalDate.now()).getYears() : null;
    }

    /**
     * 计算指定日期时的年龄
     * @param date 指定日期
     * @return 指定日期时的年龄，如果生日未设置则返回null
     */
    @Transient
    public Integer getAgeAt(LocalDate date) {
        return (birthDate != null && date != null) ?
            Period.between(birthDate, date).getYears() : null;
    }
}