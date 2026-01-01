package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.WorkSite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkSiteRepository extends JpaRepository<WorkSite, Long> {

    /**
     * 组合筛选查询 - 支持管理端筛选需求
     * @param name 名称模糊搜索（可为null）
     * @param pageable 分页参数
     */
    @Query("SELECT s FROM WorkSite s WHERE " +
           "(?1 IS NULL OR s.name LIKE CONCAT('%', ?1, '%'))")
    Page<WorkSite> findWithFilters(
        String name,
        Pageable pageable
    );
}