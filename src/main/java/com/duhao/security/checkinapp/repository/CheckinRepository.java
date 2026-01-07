package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.entity.WorkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckinRepository extends JpaRepository<CheckinRecord, Long> {

    // ==================== 新工作片段模型查询 ====================

    /**
     * 根据ID查询工作片段，同时加载 guard 和 site（避免懒加载问题）
     */
    @Query("SELECT c FROM CheckinRecord c LEFT JOIN FETCH c.guard LEFT JOIN FETCH c.site WHERE c.id = ?1")
    Optional<CheckinRecord> findByIdWithGuardAndSite(Long id);

    /**
     * 查询保安当前在岗的工作片段（status = ACTIVE）
     */
    Optional<CheckinRecord> findByGuardAndStatus(SecurityGuard guard, WorkStatus status);

    /**
     * 根据保安和时间范围查询工作片段
     */
    List<CheckinRecord> findByGuardAndStartTimeBetween(
            SecurityGuard guard,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 查询某个站点的所有在岗保安
     */
    List<CheckinRecord> findBySiteAndStatus(WorkSite site, WorkStatus status);

    /**
     * 查询所有在岗的工作片段
     */
    List<CheckinRecord> findByStatus(WorkStatus status);

    // ==================== 兼容旧查询（已弃用） ====================

    /**
     * @deprecated 使用 findByGuardAndStartTimeBetween 代替
     */
    @Deprecated
    default List<CheckinRecord> findByGuardAndSiteAndTimestampBetween(
            SecurityGuard guard,
            WorkSite site,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return findByGuardAndStartTimeBetween(guard, startTime, endTime);
    }

    // 分页查询
    Page<CheckinRecord> findAll(Pageable pageable);

    // 根据保安查询
    List<CheckinRecord> findByGuard(SecurityGuard guard);

    // 根据保安分页查询
    Page<CheckinRecord> findByGuard(SecurityGuard guard, Pageable pageable);

    // 根据站点查询
    List<CheckinRecord> findBySite(WorkSite site);

    // 时间范围查询
    List<CheckinRecord> findByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    // 组合筛选查询 - 支持管理员端筛选需求
    @Query("SELECT c FROM CheckinRecord c WHERE " +
           "(?1 IS NULL OR c.startTime >= ?1) AND " +
           "(?2 IS NULL OR c.startTime <= ?2) AND " +
           "(?3 IS NULL OR c.status = ?3) AND " +
           "(?4 IS NULL OR c.guard.id = ?4) AND " +
           "(?5 IS NULL OR c.site.id = ?5)")
    Page<CheckinRecord> findWithFilters(
        LocalDateTime startDate,
        LocalDateTime endDate,
        WorkStatus status,
        Long guardId,
        Long siteId,
        Pageable pageable
    );

    // 统计查询 - 用于管理端统计信息
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE " +
           "(?1 IS NULL OR c.startTime >= ?1) AND " +
           "(?2 IS NULL OR c.startTime <= ?2) AND " +
           "(?3 IS NULL OR c.status = ?3) AND " +
           "(?4 IS NULL OR c.guard.id = ?4) AND " +
           "(?5 IS NULL OR c.site.id = ?5)")
    long countWithFilters(
        LocalDateTime startDate,
        LocalDateTime endDate,
        WorkStatus status,
        Long guardId,
        Long siteId
    );

    // 按状态统计记录
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE " +
           "c.status = ?1 AND " +
           "(?2 IS NULL OR c.startTime >= ?2) AND " +
           "(?3 IS NULL OR c.startTime <= ?3) AND " +
           "(?4 IS NULL OR c.guard.id = ?4) AND " +
           "(?5 IS NULL OR c.site.id = ?5)")
    long countByStatusWithFilters(
        WorkStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long guardId,
        Long siteId
    );

    // ==================== Dashboard 统计查询 ====================

    /**
     * 统计时间范围内的签到次数
     */
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE c.startTime >= ?1 AND c.startTime < ?2")
    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 统计时间范围内的签到人数（去重）
     */
    @Query("SELECT COUNT(DISTINCT c.guard.id) FROM CheckinRecord c WHERE c.startTime >= ?1 AND c.startTime < ?2")
    long countDistinctGuardsByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 统计总成功签到数（包括 COMPLETED 状态）
     */
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE c.status = 'COMPLETED'")
    long countTotalSuccess();

    /**
     * 统计总失败签到数（TIMEOUT 状态）
     */
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE c.status = 'TIMEOUT'")
    long countTotalFailed();

    /**
     * 获取最近N条签到记录
     */
    @Query("SELECT c FROM CheckinRecord c ORDER BY c.startTime DESC")
    List<CheckinRecord> findLatestCheckins(Pageable pageable);

    /**
     * 按日期统计签到数（用于周趋势）
     */
    @Query("SELECT FUNCTION('DATE', c.startTime) as date, COUNT(c) as count FROM CheckinRecord c " +
           "WHERE c.startTime >= ?1 AND c.startTime < ?2 " +
           "GROUP BY FUNCTION('DATE', c.startTime) " +
           "ORDER BY date ASC")
    List<Object[]> countByDateBetween(LocalDateTime start, LocalDateTime end);

    // ==================== 超时检查 ====================

    /**
     * 查询所有超过指定时间的在岗记录（用于超时检查）
     */
    @Query("SELECT c FROM CheckinRecord c WHERE c.status = 'ACTIVE' AND c.startTime < ?1")
    List<CheckinRecord> findActiveSessionsStartedBefore(LocalDateTime threshold);

    // ==================== 单位统计查询 ====================

    /**
     * 统计单位在时间范围内的签到次数
     */
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE c.site.id = ?1 AND c.startTime >= ?2 AND c.startTime < ?3")
    long countBySiteAndTimestampBetween(Long siteId, LocalDateTime start, LocalDateTime end);

    /**
     * 统计单位在时间范围内的签到人数（去重）
     */
    @Query("SELECT COUNT(DISTINCT c.guard.id) FROM CheckinRecord c WHERE c.site.id = ?1 AND c.startTime >= ?2 AND c.startTime < ?3")
    long countDistinctGuardsBySiteAndTimestampBetween(Long siteId, LocalDateTime start, LocalDateTime end);

    /**
     * 统计【分配到该单位的保安】在时间范围内的签到人数（去重）
     * 只统计 guard.site.id = siteId 的保安
     */
    @Query("SELECT COUNT(DISTINCT c.guard.id) FROM CheckinRecord c " +
           "WHERE c.site.id = ?1 AND c.guard.site.id = ?1 AND c.startTime >= ?2 AND c.startTime < ?3")
    long countDistinctAssignedGuardsBySiteAndTimestampBetween(Long siteId, LocalDateTime start, LocalDateTime end);

    /**
     * 统计单位当前在岗人数
     */
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE c.site.id = ?1 AND c.status = ?2")
    long countBySiteAndStatus(Long siteId, WorkStatus status);

    /**
     * 统计保安在时间范围内的签到次数
     */
    @Query("SELECT COUNT(c) FROM CheckinRecord c WHERE c.guard.id = ?1 AND c.startTime >= ?2 AND c.startTime < ?3")
    long countByGuardAndTimestampBetween(Long guardId, LocalDateTime start, LocalDateTime end);
}
