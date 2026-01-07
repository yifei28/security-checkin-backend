package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.EmploymentStatus;
import com.duhao.security.checkinapp.entity.GuardRole;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

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
     * 根据单位ID分页查询保安
     */
    Page<SecurityGuard> findBySiteId(Long siteId, Pageable pageable);

    /**
     * 统计单位的保安数量
     */
    long countBySiteId(Long siteId);

    /**
     * 查找所有有工作地点分配的保安
     */
    List<SecurityGuard> findBySiteIsNotNull();

    /**
     * 组合筛选查询 - 支持管理端筛选需求
     * @param name 姓名模糊搜索（可为null）
     * @param siteId 单位ID（可为null）
     * @param employmentStatus 在职状态（可为null）
     * @param role 角色（可为null）
     * @param heightMin 最低身高cm（可为null）
     * @param heightMax 最高身高cm（可为null）
     * @param firefightingMin 消防证最低级别（null=不筛选, 1-5=最低级别）
     * @param firefightingMax 消防证最高级别（null=不限）
     * @param securityGuardMin 保安师证最低级别
     * @param securityGuardMax 保安师证最高级别
     * @param securityCheckMin 安检证最低级别
     * @param securityCheckMax 安检证最高级别
     * @param pageable 分页参数
     */
    @Query("SELECT g FROM SecurityGuard g WHERE " +
           "(?1 IS NULL OR g.name LIKE CONCAT('%', ?1, '%')) AND " +
           "(?2 IS NULL OR g.site.id = ?2) AND " +
           "(?3 IS NULL OR g.employmentStatus = ?3) AND " +
           "(?4 IS NULL OR g.role = ?4) AND " +
           "(?5 IS NULL OR g.height >= ?5) AND " +
           "(?6 IS NULL OR g.height <= ?6) AND " +
           "(?7 IS NULL OR g.firefightingCertLevel >= ?7) AND " +
           "(?8 IS NULL OR g.firefightingCertLevel <= ?8) AND " +
           "(?9 IS NULL OR g.securityGuardCertLevel >= ?9) AND " +
           "(?10 IS NULL OR g.securityGuardCertLevel <= ?10) AND " +
           "(?11 IS NULL OR g.securityCheckCertLevel >= ?11) AND " +
           "(?12 IS NULL OR g.securityCheckCertLevel <= ?12)")
    Page<SecurityGuard> findWithFilters(
        String name,
        Long siteId,
        EmploymentStatus employmentStatus,
        GuardRole role,
        Integer heightMin,
        Integer heightMax,
        Integer firefightingMin,
        Integer firefightingMax,
        Integer securityGuardMin,
        Integer securityGuardMax,
        Integer securityCheckMin,
        Integer securityCheckMax,
        Pageable pageable
    );

    // ==================== Dashboard 统计查询 ====================

    /**
     * 统计队长数量
     */
    @Query("SELECT COUNT(g) FROM SecurityGuard g WHERE g.role = 'TEAM_LEADER'")
    long countTeamLeaders();

    /**
     * 统计队员数量
     */
    @Query("SELECT COUNT(g) FROM SecurityGuard g WHERE g.role = 'TEAM_MEMBER'")
    long countTeamMembers();

    /**
     * 计算平均年龄（只统计有生日的）
     */
    @Query("SELECT AVG(YEAR(CURRENT_DATE) - YEAR(g.birthDate)) FROM SecurityGuard g WHERE g.birthDate IS NOT NULL")
    Double calculateAverageAge();
}