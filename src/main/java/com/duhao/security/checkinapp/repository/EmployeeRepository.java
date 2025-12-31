package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * 员工基础 Repository 接口
 * 提供所有员工类型共用的查询方法
 * 使用 @NoRepositoryBean 标记，不会被实例化，仅作为基类
 */
@NoRepositoryBean
public interface EmployeeRepository<T extends Employee> extends JpaRepository<T, Long> {

    /**
     * 根据手机号查找员工
     */
    T findByPhoneNumber(String phoneNumber);

    /**
     * 根据员工编号查找员工
     */
    T findByEmployeeId(String employeeId);

    /**
     * 根据微信 openId 查找员工
     */
    T findByOpenId(String openId);
}