package com.duhao.security.checkinapp.service;

import com.duhao.security.checkinapp.config.SpotCheckProperties;
import com.duhao.security.checkinapp.entity.CheckinRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 延迟任务处理器 - 调度层
 * 负责轮询 Redis 延迟队列，将任务分发给 Handler 处理
 */
@Service
public class DelayedTaskProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DelayedTaskProcessor.class);

    private final DelayedTaskService delayedTaskService;
    private final DelayedTaskHandler handler;
    private final SpotCheckProperties properties;

    /**
     * 轮询计数器，用于定期输出队列状态
     */
    private int pollCounter = 0;

    public DelayedTaskProcessor(DelayedTaskService delayedTaskService,
                                DelayedTaskHandler handler,
                                SpotCheckProperties properties) {
        this.delayedTaskService = delayedTaskService;
        this.handler = handler;
        this.properties = properties;
    }

    /**
     * 每秒处理延迟任务
     */
    @Scheduled(fixedRate = 1000)
    public void processDelayedTasks() {
        // 每5分钟输出一次队列状态（300秒）
        pollCounter++;
        if (pollCounter >= 300) {
            pollCounter = 0;
            logQueueStatus();
        }

        processSessionTimeouts();
        processSpotCheckTriggers();
        processSpotCheckTimeouts();
    }

    /**
     * 输出队列状态日志
     */
    private void logQueueStatus() {
        try {
            Long sessionTimeoutSize = delayedTaskService.getQueueSize(DelayedTaskService.KEY_SESSION_TIMEOUT);
            Long spotCheckTriggerSize = delayedTaskService.getQueueSize(DelayedTaskService.KEY_SPOTCHECK_TRIGGER);
            Long spotCheckTimeoutSize = delayedTaskService.getQueueSize(DelayedTaskService.KEY_SPOTCHECK_TIMEOUT);
            logger.info("[调度器心跳] 队列状态: timeout:session={}, spotcheck:trigger={}, spotcheck:timeout={}",
                    sessionTimeoutSize, spotCheckTriggerSize, spotCheckTimeoutSize);
        } catch (Exception e) {
            logger.error("[调度器心跳] 获取队列状态失败", e);
        }
    }

    // ==================== 工作片段超时 ====================

    private void processSessionTimeouts() {
        Set<String> tasks = delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SESSION_TIMEOUT);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (String taskValue : tasks) {
            try {
                handler.onSessionTimeout(taskValue);  // 通过代理调用，@Transactional 生效
            } catch (Exception e) {
                logger.error("处理工作片段超时失败: {}", taskValue, e);
            }
        }
    }

    // ==================== 抽查触发 ====================

    private void processSpotCheckTriggers() {
        Set<String> tasks = delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SPOTCHECK_TRIGGER);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (String taskValue : tasks) {
            try {
                handler.onSpotCheckTrigger(taskValue);  // 通过代理调用，@Transactional 生效
            } catch (Exception e) {
                logger.error("处理抽查触发失败: {}", taskValue, e);
            }
        }
    }

    // ==================== 抽查超时 ====================

    private void processSpotCheckTimeouts() {
        Set<String> tasks = delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SPOTCHECK_TIMEOUT);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (String taskValue : tasks) {
            try {
                handler.onSpotCheckTimeout(taskValue);  // 通过代理调用，@Transactional 生效
            } catch (Exception e) {
                logger.error("处理抽查超时失败: {}", taskValue, e);
            }
        }
    }

    // ==================== 对外接口 ====================

    /**
     * 为新工作片段预约超时和首次抽查
     */
    public void scheduleForNewSession(CheckinRecord session) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 预约超时
        LocalDateTime timeoutAt = session.getStartTime().plusHours(properties.getSessionTimeoutHours());
        delayedTaskService.scheduleSessionTimeout(
                session.getId(),
                session.getVersion(),
                timeoutAt
        );

        // 2. 生成首次抽查时间（使用动态间隔：0次已抽 → 90-150分钟）
        LocalDateTime firstTriggerTime = handler.generateNextSpotCheckTime(now, 0);

        if (firstTriggerTime == null) {
            logger.warn("无法生成首次抽查时间（配置问题）: sessionId={}", session.getId());
            return;
        }

        // 3. 预约首次抽查
        delayedTaskService.scheduleSpotCheckTrigger(
                session.getId(),
                session.getVersion(),
                firstTriggerTime
        );

        logger.info("新工作片段预约: sessionId={}, timeoutAt={}, firstSpotCheck={}",
                session.getId(), timeoutAt, firstTriggerTime);
    }

    /**
     * 取消工作片段的所有预约
     */
    public void cancelSessionSchedules(Long sessionId) {
        delayedTaskService.cancelSessionTimeout(sessionId);
        delayedTaskService.cancelSpotCheckTrigger(sessionId);
        logger.debug("取消工作片段预约: sessionId={}", sessionId);
    }
}
