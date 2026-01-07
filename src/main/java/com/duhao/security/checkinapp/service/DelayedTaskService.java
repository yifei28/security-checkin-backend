package com.duhao.security.checkinapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * 延迟任务服务
 * 使用 Redis ZSET 实现延迟队列
 */
@Service
public class DelayedTaskService {

    private static final Logger logger = LoggerFactory.getLogger(DelayedTaskService.class);

    /**
     * Redis Key: 工作片段超时队列
     * Value 格式: sessionId:version
     */
    public static final String KEY_SESSION_TIMEOUT = "timeout:session";

    /**
     * Redis Key: 抽查触发队列
     * Value 格式: sessionId:version
     */
    public static final String KEY_SPOTCHECK_TRIGGER = "spotcheck:trigger";

    /**
     * Redis Key: 抽查超时队列
     * Value 格式: spotCheckId:version
     */
    public static final String KEY_SPOTCHECK_TIMEOUT = "spotcheck:timeout";

    private final StringRedisTemplate redisTemplate;
    private final ZSetOperations<String, String> zSetOps;

    public DelayedTaskService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.zSetOps = redisTemplate.opsForZSet();
    }

    // ==================== 工作片段超时 ====================

    /**
     * 预约工作片段超时检查
     *
     * @param sessionId 工作片段ID
     * @param version   当前版本号
     * @param timeoutAt 超时时间
     */
    public void scheduleSessionTimeout(Long sessionId, Long version, LocalDateTime timeoutAt) {
        String value = formatValue(sessionId, version);
        double score = toTimestamp(timeoutAt);
        zSetOps.add(KEY_SESSION_TIMEOUT, value, score);
        logger.debug("预约工作片段超时: sessionId={}, version={}, timeoutAt={}",
                sessionId, version, timeoutAt);
    }

    /**
     * 取消工作片段超时检查
     *
     * @param sessionId 工作片段ID
     */
    public void cancelSessionTimeout(Long sessionId) {
        removeByPrefix(KEY_SESSION_TIMEOUT, sessionId + ":");
        logger.debug("取消工作片段超时: sessionId={}", sessionId);
    }

    // ==================== 抽查触发 ====================

    /**
     * 预约抽查触发
     *
     * @param sessionId 工作片段ID
     * @param version   当前版本号
     * @param triggerAt 触发时间
     */
    public void scheduleSpotCheckTrigger(Long sessionId, Long version, LocalDateTime triggerAt) {
        String value = formatValue(sessionId, version);
        double score = toTimestamp(triggerAt);
        zSetOps.add(KEY_SPOTCHECK_TRIGGER, value, score);
        logger.debug("预约抽查触发: sessionId={}, version={}, triggerAt={}",
                sessionId, version, triggerAt);
    }

    /**
     * 取消抽查触发
     *
     * @param sessionId 工作片段ID
     */
    public void cancelSpotCheckTrigger(Long sessionId) {
        removeByPrefix(KEY_SPOTCHECK_TRIGGER, sessionId + ":");
        logger.debug("取消抽查触发: sessionId={}", sessionId);
    }

    // ==================== 抽查超时 ====================

    /**
     * 预约抽查超时检查
     *
     * @param spotCheckId 抽查ID
     * @param version     当前版本号
     * @param timeoutAt   超时时间
     */
    public void scheduleSpotCheckTimeout(Long spotCheckId, Long version, LocalDateTime timeoutAt) {
        String value = formatValue(spotCheckId, version);
        double score = toTimestamp(timeoutAt);
        zSetOps.add(KEY_SPOTCHECK_TIMEOUT, value, score);
        logger.debug("预约抽查超时: spotCheckId={}, version={}, timeoutAt={}",
                spotCheckId, version, timeoutAt);
    }

    /**
     * 取消抽查超时检查
     *
     * @param spotCheckId 抽查ID
     */
    public void cancelSpotCheckTimeout(Long spotCheckId) {
        removeByPrefix(KEY_SPOTCHECK_TIMEOUT, spotCheckId + ":");
        logger.debug("取消抽查超时: spotCheckId={}", spotCheckId);
    }

    // ==================== 队列处理 ====================

    /**
     * 获取到期的任务
     *
     * @param key 队列Key
     * @return 到期的任务列表 (格式: id:version)
     */
    public Set<String> pollDueTasks(String key) {
        double now = System.currentTimeMillis();
        Set<String> tasks = zSetOps.rangeByScore(key, 0, now);

        if (tasks != null && !tasks.isEmpty()) {
            // 移除已获取的任务
            for (String task : tasks) {
                zSetOps.remove(key, task);
            }
            logger.debug("获取到期任务: key={}, count={}", key, tasks.size());
        }

        return tasks;
    }

    /**
     * 解析任务值
     *
     * @param value 格式: id:version
     * @return [id, version]
     */
    public long[] parseValue(String value) {
        String[] parts = value.split(":");
        return new long[]{
                Long.parseLong(parts[0]),
                Long.parseLong(parts[1])
        };
    }

    // ==================== 辅助方法 ====================

    private String formatValue(Long id, Long version) {
        return id + ":" + (version != null ? version : 0);
    }

    private double toTimestamp(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void removeByPrefix(String key, String prefix) {
        Set<String> members = zSetOps.range(key, 0, -1);
        if (members != null) {
            for (String member : members) {
                if (member.startsWith(prefix)) {
                    zSetOps.remove(key, member);
                }
            }
        }
    }

    /**
     * 获取队列中的任务数量
     */
    public Long getQueueSize(String key) {
        return zSetOps.zCard(key);
    }

    /**
     * 清空队列（仅用于测试）
     */
    public void clearQueue(String key) {
        redisTemplate.delete(key);
    }
}
