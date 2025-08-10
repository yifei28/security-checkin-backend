package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.entity.CheckinStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckinRepository extends JpaRepository<CheckinRecord, Long> {
    
    // 根据保安和站点以及时间范围查询
    List<CheckinRecord> findByGuardAndSiteAndTimestampBetween(
            SecurityGuard guard, 
            WorkSite site, 
            LocalDateTime startTime, 
            LocalDateTime endTime
    );
    
    // 分页查询
    Page<CheckinRecord> findAll(Pageable pageable);
    
    // 根据保安查询
    List<CheckinRecord> findByGuard(SecurityGuard guard);
    
    // 根据保安分页查询
    Page<CheckinRecord> findByGuard(SecurityGuard guard, Pageable pageable);
    
    // 根据站点查询
    List<CheckinRecord> findBySite(WorkSite site);
    
    // 根据状态查询
    List<CheckinRecord> findByStatus(CheckinStatus status);
    
    // 时间范围查询
    List<CheckinRecord> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    // 组合筛选查询 - 支持管理员端筛选需求
    @Query("SELECT c FROM CheckinRecord c WHERE " +
           "(?1 IS NULL OR c.timestamp >= ?1) AND " +
           "(?2 IS NULL OR c.timestamp <= ?2) AND " +
           "(?3 IS NULL OR c.status = ?3) AND " +
           "(?4 IS NULL OR c.guard.id = ?4) AND " +
           "(?5 IS NULL OR c.site.id = ?5)")
    Page<CheckinRecord> findWithFilters(
        LocalDateTime startDate,
        LocalDateTime endDate,
        CheckinStatus status,
        Long guardId,
        Long siteId,
        Pageable pageable
    );
    
    // 统计查询 - 用于管理端统计信息
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE " +
           "(?1 IS NULL OR c.timestamp >= ?1) AND " +
           "(?2 IS NULL OR c.timestamp <= ?2) AND " +
           "(?3 IS NULL OR c.status = ?3) AND " +
           "(?4 IS NULL OR c.guard.id = ?4) AND " +
           "(?5 IS NULL OR c.site.id = ?5)")
    long countWithFilters(
        LocalDateTime startDate,
        LocalDateTime endDate,
        CheckinStatus status,
        Long guardId,
        Long siteId
    );
    
    // 按状态统计记录
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE " +
           "c.status = com.duhao.security.checkinapp.entity.CheckinStatus.SUCCESS AND " +
           "(?1 IS NULL OR c.timestamp >= ?1) AND " +
           "(?2 IS NULL OR c.timestamp <= ?2) AND " +
           "(?3 IS NULL OR c.guard.id = ?3) AND " +
           "(?4 IS NULL OR c.site.id = ?4)")
    long countSuccessWithFilters(
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long guardId,
        Long siteId
    );
    
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE " +
           "c.status = com.duhao.security.checkinapp.entity.CheckinStatus.FAILED AND " +
           "(?1 IS NULL OR c.timestamp >= ?1) AND " +
           "(?2 IS NULL OR c.timestamp <= ?2) AND " +
           "(?3 IS NULL OR c.guard.id = ?3) AND " +
           "(?4 IS NULL OR c.site.id = ?4)")
    long countFailedWithFilters(
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long guardId,
        Long siteId
    );
    
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE " +
           "c.status = com.duhao.security.checkinapp.entity.CheckinStatus.PENDING AND " +
           "(?1 IS NULL OR c.timestamp >= ?1) AND " +
           "(?2 IS NULL OR c.timestamp <= ?2) AND " +
           "(?3 IS NULL OR c.guard.id = ?3) AND " +
           "(?4 IS NULL OR c.site.id = ?4)")
    long countPendingWithFilters(
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long guardId,
        Long siteId
    );
}
