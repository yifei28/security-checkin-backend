package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;

import java.util.List;

/**
 * 保安员 Repository
 * 继承 EmployeeRepository 获得通用员工查询方法
 * 并添加保安特有的查询方法
 */
public interface SecurityGuardRepository extends EmployeeRepository<SecurityGuard> {

    /**
     * 根据工作地点查找保安
     */
    List<SecurityGuard> findBySite(WorkSite site);

    /**
     * 查找所有有工作地点分配的保安
     */
    List<SecurityGuard> findBySiteIsNotNull();
}