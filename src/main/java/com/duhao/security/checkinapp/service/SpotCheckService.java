package com.duhao.security.checkinapp.service;

import com.duhao.security.checkinapp.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 抽查服务接口
 */
public interface SpotCheckService {

    /**
     * 创建抽查任务
     * @param guard 被抽查的保安
     * @param triggerType 触发类型
     * @param admin 手动触发时的管理员（自动触发时为null）
     * @return 创建的抽查任务
     */
    SpotCheck createSpotCheck(SecurityGuard guard, SpotCheckTriggerType triggerType, Admin admin);

    /**
     * 创建预约抽查任务（用于定时调度）
     * @param guard 被抽查的保安
     * @param scheduledTime 计划执行时间
     * @return 创建的抽查任务
     */
    SpotCheck createScheduledSpotCheck(SecurityGuard guard, LocalDateTime scheduledTime);

    /**
     * 完成抽查验证
     * @param spotCheckId 抽查ID
     * @param latitude 验证位置纬度
     * @param longitude 验证位置经度
     * @param faceImageUrl 人脸照片URL
     * @return 验证结果
     */
    SpotCheckResult completeSpotCheck(Long spotCheckId, Double latitude, Double longitude, String faceImageUrl);

    /**
     * 获取保安当前待处理的抽查
     * @param guardId 保安ID
     * @return 待处理抽查（如果存在）
     */
    Optional<SpotCheck> getActiveSpotCheckForGuard(Long guardId);

    /**
     * 处理超时的抽查（标记为缺勤）
     * @return 处理的抽查数量
     */
    int processExpiredSpotChecks();

    /**
     * 管理员手动触发抽查
     * @param guardIds 保安ID列表
     * @param admin 触发的管理员
     * @return 创建的抽查列表
     */
    List<SpotCheck> triggerManualSpotChecks(List<Long> guardIds, Admin admin);

    /**
     * 取消抽查
     * @param spotCheckId 抽查ID
     * @param reason 取消原因
     * @return 是否成功
     */
    boolean cancelSpotCheck(Long spotCheckId, String reason);

    /**
     * 获取保安的抽查历史
     * @param guardId 保安ID
     * @param pageable 分页参数
     * @return 抽查历史分页
     */
    Page<SpotCheck> getSpotCheckHistory(Long guardId, Pageable pageable);

    /**
     * 管理员筛选查询抽查记录
     */
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
     * 获取今日抽查计划
     * @return 今日的抽查列表
     */
    List<SpotCheck> getTodaySchedule();

    /**
     * 获取抽查统计
     */
    SpotCheckStatistics getStatistics(LocalDateTime startDate, LocalDateTime endDate, Long guardId, Long siteId);

    /**
     * 统计保安当日抽查次数
     */
    long countTodaySpotChecks(Long guardId);

    /**
     * 抽查验证结果
     */
    record SpotCheckResult(boolean success, String message, SpotCheck spotCheck) {
        public static SpotCheckResult ok(SpotCheck spotCheck) {
            return new SpotCheckResult(true, "验证成功", spotCheck);
        }

        public static SpotCheckResult fail(String message) {
            return new SpotCheckResult(false, message, null);
        }
    }

    /**
     * 抽查统计数据
     */
    record SpotCheckStatistics(
        long totalCount,
        long completedCount,
        long missedCount,
        long failedCount,
        long pendingCount,
        long cancelledCount,
        int completionRate
    ) {}
}
