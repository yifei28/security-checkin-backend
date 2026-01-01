package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.SpotCheck;
import com.duhao.security.checkinapp.entity.SpotCheckStatus;
import com.duhao.security.checkinapp.entity.SpotCheckTriggerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpotCheckRepository extends JpaRepository<SpotCheck, Long> {

    /**
     * 查询保安的待处理抽查
     */
    Optional<SpotCheck> findByGuardAndStatus(SecurityGuard guard, SpotCheckStatus status);

    /**
     * 根据保安ID查询待处理抽查
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.guard.id = ?1 AND s.status = ?2")
    Optional<SpotCheck> findByGuardIdAndStatus(Long guardId, SpotCheckStatus status);

    /**
     * 查询已超时但仍为待处理状态的抽查
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.status = 'PENDING' AND s.deadline < ?1")
    List<SpotCheck> findExpiredPendingChecks(LocalDateTime now);

    /**
     * 统计保安当日抽查次数
     */
    @Query("SELECT COUNT(s) FROM SpotCheck s WHERE s.guard.id = ?1 AND s.createdAt >= ?2 AND s.createdAt < ?3")
    long countByGuardAndDate(Long guardId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    /**
     * 查询保安当日的所有抽查
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.guard.id = ?1 AND s.createdAt >= ?2 AND s.createdAt < ?3 ORDER BY s.createdAt DESC")
    List<SpotCheck> findByGuardAndDate(Long guardId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    /**
     * 查询待发送通知的抽查
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.status = 'PENDING' AND s.notificationSent = false AND s.scheduledTime <= ?1")
    List<SpotCheck> findPendingNotifications(LocalDateTime now);

    /**
     * 查询计划在指定时间触发的抽查
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.status = 'PENDING' AND s.scheduledTime <= ?1 AND s.scheduledTime > ?2")
    List<SpotCheck> findScheduledChecks(LocalDateTime now, LocalDateTime previousCheck);

    /**
     * 管理员筛选查询
     */
    @Query("SELECT s FROM SpotCheck s WHERE " +
           "(?1 IS NULL OR s.createdAt >= ?1) AND " +
           "(?2 IS NULL OR s.createdAt <= ?2) AND " +
           "(?3 IS NULL OR s.status = ?3) AND " +
           "(?4 IS NULL OR s.guard.id = ?4) AND " +
           "(?5 IS NULL OR s.site.id = ?5) AND " +
           "(?6 IS NULL OR s.triggerType = ?6)")
    Page<SpotCheck> findWithFilters(
        LocalDateTime startDate,
        LocalDateTime endDate,
        SpotCheckStatus status,
        Long guardId,
        Long siteId,
        SpotCheckTriggerType triggerType,
        Pageable pageable
    );

    /**
     * 统计查询 - 按状态统计
     */
    @Query("SELECT COUNT(s) FROM SpotCheck s WHERE " +
           "(?1 IS NULL OR s.createdAt >= ?1) AND " +
           "(?2 IS NULL OR s.createdAt <= ?2) AND " +
           "s.status = ?3 AND " +
           "(?4 IS NULL OR s.guard.id = ?4) AND " +
           "(?5 IS NULL OR s.site.id = ?5)")
    long countByStatusWithFilters(
        LocalDateTime startDate,
        LocalDateTime endDate,
        SpotCheckStatus status,
        Long guardId,
        Long siteId
    );

    /**
     * 查询保安的抽查历史
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.guard.id = ?1 ORDER BY s.createdAt DESC")
    Page<SpotCheck> findByGuardId(Long guardId, Pageable pageable);

    /**
     * 查询今日计划的所有抽查
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.scheduledTime >= ?1 AND s.scheduledTime < ?2 ORDER BY s.scheduledTime ASC")
    List<SpotCheck> findTodaySchedule(LocalDateTime startOfDay, LocalDateTime endOfDay);

    /**
     * 根据保安查询
     */
    List<SpotCheck> findByGuard(SecurityGuard guard);
}
