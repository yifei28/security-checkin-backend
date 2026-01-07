package com.duhao.security.checkinapp.impl;

import com.duhao.security.checkinapp.config.SpotCheckProperties;
import com.duhao.security.checkinapp.entity.*;
import com.duhao.security.checkinapp.repository.CheckinLocationRepository;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.repository.SpotCheckRepository;
import com.duhao.security.checkinapp.service.DelayedTaskService;
import com.duhao.security.checkinapp.service.SpotCheckService;
import com.duhao.security.checkinapp.service.WechatNotificationService;
import com.duhao.security.checkinapp.util.DistanceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 抽查服务实现
 */
@Service
public class SpotCheckServiceImpl implements SpotCheckService {

    private static final Logger logger = LoggerFactory.getLogger(SpotCheckServiceImpl.class);

    private final SpotCheckRepository spotCheckRepository;
    private final CheckinRepository checkinRepository;
    private final SecurityGuardRepository guardRepository;
    private final CheckinLocationRepository locationRepository;
    private final DelayedTaskService delayedTaskService;
    private final WechatNotificationService notificationService;
    private final SpotCheckProperties properties;

    public SpotCheckServiceImpl(SpotCheckRepository spotCheckRepository,
                                 CheckinRepository checkinRepository,
                                 SecurityGuardRepository guardRepository,
                                 CheckinLocationRepository locationRepository,
                                 DelayedTaskService delayedTaskService,
                                 WechatNotificationService notificationService,
                                 SpotCheckProperties properties) {
        this.spotCheckRepository = spotCheckRepository;
        this.checkinRepository = checkinRepository;
        this.guardRepository = guardRepository;
        this.locationRepository = locationRepository;
        this.delayedTaskService = delayedTaskService;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public SpotCheck createSpotCheck(SecurityGuard guard, SpotCheckTriggerType triggerType, Admin admin) {
        // 查找保安当前的 ACTIVE 工作片段
        Optional<CheckinRecord> activeSession = checkinRepository.findByGuardAndStatus(guard, WorkStatus.ACTIVE);
        if (activeSession.isEmpty()) {
            logger.warn("保安没有进行中的工作片段，无法创建抽查: guardId={}", guard.getId());
            return null;
        }

        CheckinRecord session = activeSession.get();
        SpotCheck spotCheck = new SpotCheck(session, triggerType);
        spotCheck = spotCheckRepository.save(spotCheck);

        // 预约抽查超时
        delayedTaskService.scheduleSpotCheckTimeout(
                spotCheck.getId(),
                spotCheck.getVersion(),
                spotCheck.getDeadline()
        );

        // 发送微信订阅消息通知
        notificationService.sendSpotCheckNotification(spotCheck);

        logger.info("创建抽查: spotCheckId={}, guardId={}, guardName={}, triggerType={}",
                spotCheck.getId(), guard.getId(), guard.getName(), triggerType);

        return spotCheck;
    }

    @Override
    @Transactional
    public SpotCheck createScheduledSpotCheck(SecurityGuard guard, LocalDateTime scheduledTime) {
        // 预约抽查实际由 DelayedTaskProcessor 处理
        // 这里仅作为兼容接口
        return createSpotCheck(guard, SpotCheckTriggerType.AUTOMATIC, null);
    }

    @Override
    @Transactional
    public SpotCheckResult completeSpotCheck(Long spotCheckId, Double latitude, Double longitude, String faceImageUrl) {
        logger.info("完成抽查请求: spotCheckId={}, lat={}, lng={}", spotCheckId, latitude, longitude);

        // 1. 查询抽查
        SpotCheck spotCheck = spotCheckRepository.findById(spotCheckId).orElse(null);
        if (spotCheck == null) {
            return SpotCheckResult.fail("抽查记录不存在");
        }

        // 2. 检查状态
        if (spotCheck.getStatus() != SpotCheckStatus.PENDING) {
            return SpotCheckResult.fail("抽查已处理，当前状态: " + spotCheck.getStatus().getDisplayName());
        }

        // 3. 检查是否超时
        if (spotCheck.isExpired()) {
            spotCheck.markMissed();
            spotCheckRepository.save(spotCheck);
            return SpotCheckResult.fail("抽查已超时");
        }

        // 4. 验证位置（支持多签到地点）
        WorkSite site = spotCheck.getSite();
        if (site == null) {
            return SpotCheckResult.fail("无法获取工作地点信息");
        }

        String locationError = validateLocation(site, latitude, longitude);
        if (locationError != null) {
            spotCheck.setFailReason(locationError);
            spotCheckRepository.save(spotCheck);
            return SpotCheckResult.fail(locationError);
        }

        // 5. 标记为通过
        spotCheck.markPassed(latitude, longitude, faceImageUrl);
        spotCheck = spotCheckRepository.save(spotCheck);

        logger.info("抽查通过: spotCheckId={}, guardName={}",
                spotCheckId, spotCheck.getGuard() != null ? spotCheck.getGuard().getName() : "unknown");

        return SpotCheckResult.ok(spotCheck);
    }

    @Override
    public Optional<SpotCheck> getActiveSpotCheckForGuard(Long guardId) {
        return spotCheckRepository.findPendingByGuardId(guardId);
    }

    @Override
    @Transactional
    public int processExpiredSpotChecks() {
        // 这个功能由 DelayedTaskProcessor 处理
        // 这里作为兜底方案，扫描超时未处理的抽查
        List<SpotCheck> expiredChecks = spotCheckRepository.findExpiredPendingChecks(LocalDateTime.now());
        int count = 0;

        for (SpotCheck check : expiredChecks) {
            check.markMissed();
            spotCheckRepository.save(check);
            count++;
            logger.info("处理超时抽查: spotCheckId={}", check.getId());
        }

        return count;
    }

    @Override
    @Transactional
    public List<SpotCheck> triggerManualSpotChecks(List<Long> guardIds, Admin admin) {
        List<SpotCheck> createdChecks = new ArrayList<>();

        for (Long guardId : guardIds) {
            SecurityGuard guard = guardRepository.findById(guardId).orElse(null);
            if (guard == null) {
                logger.warn("保安不存在: guardId={}", guardId);
                continue;
            }

            // 检查是否有待处理的抽查
            Optional<SpotCheck> pending = spotCheckRepository.findPendingByGuardId(guardId);
            if (pending.isPresent()) {
                logger.warn("保安有未完成的抽查，跳过: guardId={}", guardId);
                continue;
            }

            SpotCheck check = createSpotCheck(guard, SpotCheckTriggerType.MANUAL, admin);
            if (check != null) {
                createdChecks.add(check);
            }
        }

        logger.info("手动触发抽查完成: 请求数量={}, 成功数量={}", guardIds.size(), createdChecks.size());
        return createdChecks;
    }

    @Override
    @Transactional
    public boolean cancelSpotCheck(Long spotCheckId, String reason) {
        SpotCheck spotCheck = spotCheckRepository.findById(spotCheckId).orElse(null);
        if (spotCheck == null) {
            return false;
        }

        if (spotCheck.getStatus() != SpotCheckStatus.PENDING) {
            logger.warn("无法取消非待处理状态的抽查: spotCheckId={}, status={}",
                    spotCheckId, spotCheck.getStatus());
            return false;
        }

        // 在新架构中，只有 PENDING, PASSED, MISSED 三种状态
        // 取消操作改为标记 MISSED 并记录原因
        spotCheck.markMissed();
        spotCheck.setFailReason("管理员取消: " + reason);
        spotCheckRepository.save(spotCheck);

        // 取消超时预约
        delayedTaskService.cancelSpotCheckTimeout(spotCheckId);

        logger.info("取消抽查: spotCheckId={}, reason={}", spotCheckId, reason);
        return true;
    }

    @Override
    public Page<SpotCheck> getSpotCheckHistory(Long guardId, Pageable pageable) {
        return spotCheckRepository.findByGuardId(guardId, pageable);
    }

    @Override
    public Page<SpotCheck> findWithFilters(LocalDateTime startDate, LocalDateTime endDate,
                                           SpotCheckStatus status, Long guardId, Long siteId,
                                           SpotCheckTriggerType triggerType, Pageable pageable) {
        return spotCheckRepository.findWithFilters(startDate, endDate, status, guardId, siteId, triggerType, pageable);
    }

    @Override
    public List<SpotCheck> getTodaySchedule() {
        // 在新架构中，抽查是按需触发的，不是预先计划的
        // 返回今天所有的抽查记录
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // 查询所有保安今天的抽查
        return spotCheckRepository.findAll().stream()
                .filter(s -> !s.getCreatedAt().isBefore(startOfDay) && !s.getCreatedAt().isAfter(endOfDay))
                .toList();
    }

    @Override
    public SpotCheckStatistics getStatistics(LocalDateTime startDate, LocalDateTime endDate,
                                             Long guardId, Long siteId) {
        long passedCount = spotCheckRepository.countByStatusWithFilters(
                startDate, endDate, SpotCheckStatus.PASSED, guardId, siteId);
        long missedCount = spotCheckRepository.countByStatusWithFilters(
                startDate, endDate, SpotCheckStatus.MISSED, guardId, siteId);
        long pendingCount = spotCheckRepository.countByStatusWithFilters(
                startDate, endDate, SpotCheckStatus.PENDING, guardId, siteId);

        long totalCount = passedCount + missedCount + pendingCount;

        // 计算完成率（不包含待处理的）
        long completedCount = passedCount;
        long processedCount = passedCount + missedCount;
        int completionRate = processedCount > 0 ? (int) (completedCount * 100 / processedCount) : 0;

        // 新架构只有 PENDING, PASSED, MISSED 三种状态
        // failedCount 和 cancelledCount 在新架构中都算作 missed
        return new SpotCheckStatistics(
                totalCount,
                passedCount,      // completedCount = 通过数
                missedCount,      // missedCount
                0,                // failedCount (新架构中没有)
                pendingCount,
                0,                // cancelledCount (新架构中没有)
                completionRate
        );
    }

    @Override
    public long countTodaySpotChecks(Long guardId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        return spotCheckRepository.countByGuardAndDate(guardId, startOfDay, endOfDay);
    }

    /**
     * 验证位置是否在单位的签到范围内
     * 支持多签到地点：只要在任一签到地点范围内即可
     */
    private String validateLocation(WorkSite site, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return "位置信息缺失，请检查定位";
        }

        // 获取单位的所有签到地点
        List<CheckinLocation> locations = locationRepository.findBySiteId(site.getId());

        if (locations.isEmpty()) {
            // 没有签到地点时，使用单位原有坐标（向后兼容）
            double distance = DistanceCalculator.calculateDistance(
                    latitude, longitude,
                    site.getLatitude(), site.getLongitude()
            );
            if (distance > site.getAllowedRadiusMeters()) {
                return String.format("位置超出允许范围（距离：%.0f米，允许：%.0f米）",
                        distance, site.getAllowedRadiusMeters());
            }
            return null;
        }

        // 检查是否在任一签到地点范围内
        double minDistance = Double.MAX_VALUE;
        String nearestLocation = null;

        for (CheckinLocation loc : locations) {
            double distance = DistanceCalculator.calculateDistance(
                    latitude, longitude,
                    loc.getLatitude(), loc.getLongitude()
            );

            if (distance <= loc.getAllowedRadius()) {
                logger.info("抽查位置验证通过: 在签到地点 '{}' 范围内", loc.getName());
                return null;
            }

            if (distance < minDistance) {
                minDistance = distance;
                nearestLocation = loc.getName();
            }
        }

        return String.format("位置超出允许范围（距最近地点[%s]：%.0f米）", nearestLocation, minDistance);
    }
}
