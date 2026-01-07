package com.duhao.security.checkinapp.service;

import com.duhao.security.checkinapp.config.SpotCheckProperties;
import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.SpotCheck;
import com.duhao.security.checkinapp.entity.SpotCheckStatus;
import com.duhao.security.checkinapp.entity.SpotCheckTriggerType;
import com.duhao.security.checkinapp.entity.WorkStatus;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SpotCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 延迟任务处理器 - 业务逻辑处理
 * 独立 Service，确保 @Transactional 通过 AOP 代理生效
 */
@Service
public class DelayedTaskHandler {

    private static final Logger logger = LoggerFactory.getLogger(DelayedTaskHandler.class);

    private final DelayedTaskService delayedTaskService;
    private final CheckinRepository checkinRepository;
    private final SpotCheckRepository spotCheckRepository;
    private final SpotCheckProperties properties;
    private final WechatNotificationService notificationService;

    public DelayedTaskHandler(DelayedTaskService delayedTaskService,
                              CheckinRepository checkinRepository,
                              SpotCheckRepository spotCheckRepository,
                              SpotCheckProperties properties,
                              WechatNotificationService notificationService) {
        this.delayedTaskService = delayedTaskService;
        this.checkinRepository = checkinRepository;
        this.spotCheckRepository = spotCheckRepository;
        this.properties = properties;
        this.notificationService = notificationService;
    }

    // ==================== 工作片段超时处理 ====================

    @Transactional
    public void onSessionTimeout(String taskValue) {
        long[] parsed = delayedTaskService.parseValue(taskValue);
        Long sessionId = parsed[0];
        Long expectedVersion = parsed[1];

        CheckinRecord session = checkinRepository.findById(sessionId).orElse(null);

        // 三重校验
        if (session == null) {
            logger.debug("工作片段不存在: sessionId={}", sessionId);
            return;
        }
        if (!session.getVersion().equals(expectedVersion)) {
            logger.debug("版本不匹配: sessionId={}, expected={}, actual={}",
                    sessionId, expectedVersion, session.getVersion());
            return;
        }
        if (session.getStatus() != WorkStatus.ACTIVE) {
            logger.debug("状态已变更: sessionId={}, status={}", sessionId, session.getStatus());
            return;
        }

        // 执行超时处理
        String guardName = session.getGuard() != null ? session.getGuard().getName() : "unknown";
        logger.info("工作片段超时: sessionId={}, guardName={}, startTime={}",
                sessionId, guardName, session.getStartTime());

        session.timeout();

        // 更新抽查统计
        long total = spotCheckRepository.countByCheckinRecordId(sessionId);
        long passed = spotCheckRepository.countPassedByCheckinRecordId(sessionId);
        session.updateSpotCheckStats((int) total, (int) passed);

        checkinRepository.save(session);

        // 所有 PENDING 抽查 → MISSED
        List<SpotCheck> pendingChecks = spotCheckRepository.findByCheckinRecordIdAndStatus(
                sessionId, SpotCheckStatus.PENDING);
        for (SpotCheck check : pendingChecks) {
            check.markMissed();
            spotCheckRepository.save(check);
        }

        logger.info("工作片段超时处理完成: sessionId={}, pendingChecksMissed={}",
                sessionId, pendingChecks.size());
    }

    // ==================== 抽查触发处理 ====================

    @Transactional
    public void onSpotCheckTrigger(String taskValue) {
        long[] parsed = delayedTaskService.parseValue(taskValue);
        Long sessionId = parsed[0];
        Long expectedVersion = parsed[1];

        CheckinRecord session = checkinRepository.findById(sessionId).orElse(null);

        // 三重校验
        if (session == null) {
            logger.debug("工作片段不存在: sessionId={}", sessionId);
            return;
        }
        if (!session.getVersion().equals(expectedVersion)) {
            logger.debug("版本不匹配: sessionId={}, expected={}, actual={}",
                    sessionId, expectedVersion, session.getVersion());
            return;
        }
        if (session.getStatus() != WorkStatus.ACTIVE) {
            logger.debug("状态已变更: sessionId={}, status={}", sessionId, session.getStatus());
            return;
        }

        // 查询本次工作片段已触发的抽查次数
        int completedChecks = (int) spotCheckRepository.countByCheckinRecordId(sessionId);

        // 检查是否已达上限
        if (completedChecks >= properties.getMaxChecksPerSession()) {
            logger.info("已达抽查上限，不再安排: sessionId={}, completedChecks={}",
                    sessionId, completedChecks);
            return;
        }

        // 创建抽查
        SpotCheck spotCheck = new SpotCheck(session, SpotCheckTriggerType.AUTOMATIC);
        spotCheck = spotCheckRepository.save(spotCheck);

        String guardName = session.getGuard() != null ? session.getGuard().getName() : "unknown";
        logger.info("抽查触发: sessionId={}, spotCheckId={}, guardName={}, 第{}次抽查",
                sessionId, spotCheck.getId(), guardName, completedChecks + 1);

        // 发送微信订阅消息通知（异步）
        notificationService.sendSpotCheckNotification(spotCheck);

        // 预约抽查超时
        delayedTaskService.scheduleSpotCheckTimeout(
                spotCheck.getId(),
                spotCheck.getVersion(),
                spotCheck.getDeadline()
        );

        // 检查是否还能安排下一次（刚创建了一个，所以 +1）
        int newCompletedChecks = completedChecks + 1;
        if (newCompletedChecks >= properties.getMaxChecksPerSession()) {
            logger.info("已完成全部抽查，不再预约下次: sessionId={}, totalChecks={}",
                    sessionId, newCompletedChecks);
            return;
        }

        // 生成下次抽查时间（根据已完成次数动态调整间隔）
        LocalDateTime nextTriggerTime = generateNextSpotCheckTime(LocalDateTime.now(), newCompletedChecks);

        // 预约下次抽查
        delayedTaskService.scheduleSpotCheckTrigger(
                sessionId,
                session.getVersion(),
                nextTriggerTime
        );

        logger.debug("预约下次抽查: sessionId={}, nextTriggerTime={}, 将是第{}次",
                sessionId, nextTriggerTime, newCompletedChecks + 1);
    }

    // ==================== 抽查超时处理 ====================

    @Transactional
    public void onSpotCheckTimeout(String taskValue) {
        long[] parsed = delayedTaskService.parseValue(taskValue);
        Long spotCheckId = parsed[0];
        Long expectedVersion = parsed[1];

        SpotCheck spotCheck = spotCheckRepository.findById(spotCheckId).orElse(null);

        // 校验
        if (spotCheck == null) {
            logger.debug("抽查不存在: spotCheckId={}", spotCheckId);
            return;
        }
        if (!spotCheck.getVersion().equals(expectedVersion)) {
            logger.debug("版本不匹配: spotCheckId={}, expected={}, actual={}",
                    spotCheckId, expectedVersion, spotCheck.getVersion());
            return;
        }
        if (spotCheck.getStatus() != SpotCheckStatus.PENDING) {
            logger.debug("状态已变更: spotCheckId={}, status={}", spotCheckId, spotCheck.getStatus());
            return;
        }

        // 标记为超时
        spotCheck.markMissed();
        spotCheckRepository.save(spotCheck);

        String guardName = spotCheck.getGuard() != null ? spotCheck.getGuard().getName() : "unknown";
        logger.info("抽查超时: spotCheckId={}, guardName={}", spotCheckId, guardName);
    }

    // ==================== 辅助方法 ====================

    /**
     * 根据已完成抽查次数，使用均匀分布生成下次抽查时间
     *
     * 动态间隔策略：
     * - 0次已抽 → 间隔 90-150分钟
     * - 1次已抽 → 间隔 120-200分钟
     * - 2次已抽 → 间隔 150-250分钟
     * - 3次已抽 → 不再安排
     *
     * @param from 起始时间
     * @param completedChecks 已完成的抽查次数
     * @return 下次抽查时间，如果已达上限返回 null
     */
    public LocalDateTime generateNextSpotCheckTime(LocalDateTime from, int completedChecks) {
        int[] range = properties.getIntervalRange(completedChecks);
        if (range == null) {
            // 已达上限，不再安排
            return null;
        }

        int minInterval = range[0];
        int maxInterval = range[1];

        // 均匀分布随机间隔
        double interval = minInterval + Math.random() * (maxInterval - minInterval);

        logger.debug("生成下次抽查间隔: completedChecks={}, range=[{}, {}], interval={}分钟",
                completedChecks, minInterval, maxInterval, (long) interval);

        return from.plusMinutes((long) interval);
    }
}
