package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.CheckinRecord;
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

    // ==================== 新工作片段模型查询 ====================

    /**
     * 根据ID查询抽查，同时加载关联的 checkinRecord 和 guard（避免懒加载问题）
     */
    @Query("SELECT s FROM SpotCheck s " +
           "LEFT JOIN FETCH s.checkinRecord c " +
           "LEFT JOIN FETCH c.guard " +
           "WHERE s.id = ?1")
    Optional<SpotCheck> findByIdWithGuard(Long id);

    /**
     * 查询工作片段的所有抽查
     */
    List<SpotCheck> findByCheckinRecord(CheckinRecord checkinRecord);

    /**
     * 根据工作片段ID查询所有抽查（按创建时间倒序）
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.checkinRecord.id = ?1 ORDER BY s.createdAt DESC")
    List<SpotCheck> findByCheckinRecordId(Long checkinRecordId);

    /**
     * 查询工作片段的所有待处理抽查
     */
    List<SpotCheck> findByCheckinRecordAndStatus(CheckinRecord checkinRecord, SpotCheckStatus status);

    /**
     * 根据工作片段ID查询待处理抽查
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.checkinRecord.id = ?1 AND s.status = ?2")
    List<SpotCheck> findByCheckinRecordIdAndStatus(Long checkinRecordId, SpotCheckStatus status);

    /**
     * 查询保安当前工作片段的待处理抽查（通过 checkinRecord -> guard）
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.checkinRecord.guard.id = ?1 AND s.status = 'PENDING'")
    Optional<SpotCheck> findPendingByGuardId(Long guardId);

    /**
     * 统计工作片段的抽查数量
     */
    @Query("SELECT COUNT(s) FROM SpotCheck s WHERE s.checkinRecord.id = ?1")
    long countByCheckinRecordId(Long checkinRecordId);

    /**
     * 统计工作片段通过的抽查数量
     */
    @Query("SELECT COUNT(s) FROM SpotCheck s WHERE s.checkinRecord.id = ?1 AND s.status = 'PASSED'")
    long countPassedByCheckinRecordId(Long checkinRecordId);

    // ==================== 管理员筛选查询 ====================

    /**
     * 管理员筛选查询（通过 checkinRecord 访问 guard 和 site）
     */
    @Query("SELECT s FROM SpotCheck s WHERE " +
           "(?1 IS NULL OR s.createdAt >= ?1) AND " +
           "(?2 IS NULL OR s.createdAt <= ?2) AND " +
           "(?3 IS NULL OR s.status = ?3) AND " +
           "(?4 IS NULL OR s.checkinRecord.guard.id = ?4) AND " +
           "(?5 IS NULL OR s.checkinRecord.site.id = ?5) AND " +
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
           "(?4 IS NULL OR s.checkinRecord.guard.id = ?4) AND " +
           "(?5 IS NULL OR s.checkinRecord.site.id = ?5)")
    long countByStatusWithFilters(
        LocalDateTime startDate,
        LocalDateTime endDate,
        SpotCheckStatus status,
        Long guardId,
        Long siteId
    );

    // ==================== 保安历史查询 ====================

    /**
     * 查询保安的抽查历史
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.checkinRecord.guard.id = ?1 ORDER BY s.createdAt DESC")
    Page<SpotCheck> findByGuardId(Long guardId, Pageable pageable);

    /**
     * 统计保安当日抽查次数
     */
    @Query("SELECT COUNT(s) FROM SpotCheck s WHERE " +
           "s.checkinRecord.guard.id = ?1 AND s.createdAt >= ?2 AND s.createdAt < ?3")
    long countByGuardAndDate(Long guardId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    /**
     * 查询保安当日的所有抽查
     */
    @Query("SELECT s FROM SpotCheck s WHERE " +
           "s.checkinRecord.guard.id = ?1 AND s.createdAt >= ?2 AND s.createdAt < ?3 " +
           "ORDER BY s.createdAt DESC")
    List<SpotCheck> findByGuardAndDate(Long guardId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    // ==================== 超时处理 ====================

    /**
     * 查询已超时但仍为待处理状态的抽查
     */
    @Query("SELECT s FROM SpotCheck s WHERE s.status = 'PENDING' AND s.deadline < ?1")
    List<SpotCheck> findExpiredPendingChecks(LocalDateTime now);
}
