package com.duhao.security.checkinapp.service;

import com.duhao.security.checkinapp.config.SpotCheckProperties;
import com.duhao.security.checkinapp.dto.WorkEndRequest;
import com.duhao.security.checkinapp.dto.WorkResponse;
import com.duhao.security.checkinapp.dto.WorkStartRequest;
import com.duhao.security.checkinapp.dto.WorkStatusResponse;
import com.duhao.security.checkinapp.entity.CheckinLocation;
import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.SpotCheck;
import com.duhao.security.checkinapp.entity.SpotCheckStatus;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.entity.WorkStatus;
import com.duhao.security.checkinapp.repository.CheckinLocationRepository;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.repository.SpotCheckRepository;
import com.duhao.security.checkinapp.util.DistanceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 工作服务
 * 处理上岗/下岗业务逻辑
 */
@Service
public class WorkService {

    private static final Logger logger = LoggerFactory.getLogger(WorkService.class);

    private final CheckinRepository checkinRepository;
    private final SecurityGuardRepository guardRepository;
    private final SpotCheckRepository spotCheckRepository;
    private final CheckinLocationRepository locationRepository;
    private final DelayedTaskProcessor delayedTaskProcessor;
    private final DelayedTaskService delayedTaskService;
    private final SpotCheckProperties properties;

    public WorkService(CheckinRepository checkinRepository,
                       SecurityGuardRepository guardRepository,
                       SpotCheckRepository spotCheckRepository,
                       CheckinLocationRepository locationRepository,
                       DelayedTaskProcessor delayedTaskProcessor,
                       DelayedTaskService delayedTaskService,
                       SpotCheckProperties properties) {
        this.checkinRepository = checkinRepository;
        this.guardRepository = guardRepository;
        this.spotCheckRepository = spotCheckRepository;
        this.locationRepository = locationRepository;
        this.delayedTaskProcessor = delayedTaskProcessor;
        this.delayedTaskService = delayedTaskService;
        this.properties = properties;
    }

    /**
     * 上岗
     */
    @Transactional
    public WorkResponse startWork(Long guardId, WorkStartRequest request) {
        logger.info("上岗请求: guardId={}, lat={}, lng={}", guardId, request.getLatitude(), request.getLongitude());

        // 1. 查询保安
        SecurityGuard guard = guardRepository.findById(guardId).orElse(null);
        if (guard == null) {
            return WorkResponse.error("保安不存在");
        }

        // 2. 检查是否已有 ACTIVE 记录
        Optional<CheckinRecord> existingActive = checkinRepository.findByGuardAndStatus(guard, WorkStatus.ACTIVE);
        if (existingActive.isPresent()) {
            return WorkResponse.error("您已在岗中，请先下岗");
        }

        // 3. 检查是否分配了工作地点
        if (guard.getSite() == null) {
            return WorkResponse.error("您未分配工作地点，请联系管理员");
        }

        // 4. 验证位置（支持多签到地点）
        String locationError = validateLocation(guard.getSite(), request.getLatitude(), request.getLongitude());
        if (locationError != null) {
            return WorkResponse.error(locationError);
        }

        // 5. 创建工作片段
        CheckinRecord session = new CheckinRecord(
                guard,
                guard.getSite(),
                request.getLatitude(),
                request.getLongitude(),
                request.getFaceImageUrl()
        );
        session = checkinRepository.save(session);

        // 6. 预约超时和首次抽查
        delayedTaskProcessor.scheduleForNewSession(session);

        logger.info("上岗成功: guardId={}, sessionId={}, siteName={}",
                guardId, session.getId(), guard.getSite().getName());

        // 7. 构建响应
        WorkResponse.WorkData data = new WorkResponse.WorkData();
        data.setSessionId(session.getId());
        data.setStartTime(session.getStartTime());
        data.setStatus(session.getStatus().getDisplayName());
        data.setSiteName(guard.getSite().getName());

        return WorkResponse.success("上岗成功", data);
    }

    /**
     * 下岗
     */
    @Transactional
    public WorkResponse endWork(Long guardId, WorkEndRequest request) {
        logger.info("下岗请求: guardId={}, lat={}, lng={}", guardId, request.getLatitude(), request.getLongitude());

        // 1. 查询保安
        SecurityGuard guard = guardRepository.findById(guardId).orElse(null);
        if (guard == null) {
            return WorkResponse.error("保安不存在");
        }

        // 2. 查找 ACTIVE 记录
        Optional<CheckinRecord> activeOpt = checkinRepository.findByGuardAndStatus(guard, WorkStatus.ACTIVE);
        if (activeOpt.isEmpty()) {
            return WorkResponse.error("您未在岗，无法下岗");
        }

        CheckinRecord session = activeOpt.get();

        // 3. 检查工作时长（至少1小时）
        long activeMinutes = session.getActiveMinutes();
        int minWorkMinutes = properties.getMinWorkHours() * 60;
        if (activeMinutes < minWorkMinutes) {
            return WorkResponse.error(String.format("工作时长不足（当前：%d分钟，最少：%d分钟）",
                    activeMinutes, minWorkMinutes));
        }

        // 4. 验证位置（支持多签到地点）
        String locationError = validateLocation(session.getSite(), request.getLatitude(), request.getLongitude());
        if (locationError != null) {
            return WorkResponse.error(locationError);
        }

        // 5. 执行下岗
        session.clockOut(request.getLatitude(), request.getLongitude(), request.getFaceImageUrl());

        // 6. 取消预约
        delayedTaskProcessor.cancelSessionSchedules(session.getId());

        // 7. 所有 PENDING 抽查 → PASSED（下岗时人还在，视为通过）
        List<SpotCheck> pendingChecks = spotCheckRepository.findByCheckinRecordIdAndStatus(
                session.getId(), SpotCheckStatus.PENDING);
        for (SpotCheck check : pendingChecks) {
            check.markPassed(request.getLatitude(), request.getLongitude(), request.getFaceImageUrl());
            spotCheckRepository.save(check);
            // 从 Redis 移除超时任务
            delayedTaskService.cancelSpotCheckTimeout(check.getId());
            logger.info("下岗时自动通过抽查: spotCheckId={}", check.getId());
        }

        // 8. 更新抽查统计（在处理完 PENDING 后统计）
        long total = spotCheckRepository.countByCheckinRecordId(session.getId());
        long passed = spotCheckRepository.countPassedByCheckinRecordId(session.getId());
        session.updateSpotCheckStats((int) total, (int) passed);

        checkinRepository.save(session);

        logger.info("下岗成功: guardId={}, sessionId={}, duration={}分钟, spotChecks={}/{}",
                guardId, session.getId(), session.getDurationMinutes(), passed, total);

        // 9. 构建响应
        WorkResponse.WorkData data = new WorkResponse.WorkData();
        data.setSessionId(session.getId());
        data.setStartTime(session.getStartTime());
        data.setEndTime(session.getEndTime());
        data.setStatus(session.getStatus().getDisplayName());
        data.setDurationMinutes(session.getDurationMinutes());
        data.setSiteName(session.getSite().getName());

        return WorkResponse.success("下岗成功", data);
    }

    /**
     * 获取当前工作状态
     */
    public WorkStatusResponse getWorkStatus(Long guardId) {
        // 1. 查询保安
        SecurityGuard guard = guardRepository.findById(guardId).orElse(null);
        if (guard == null) {
            return WorkStatusResponse.error("保安不存在");
        }

        // 2. 查找 ACTIVE 记录
        Optional<CheckinRecord> activeOpt = checkinRepository.findByGuardAndStatus(guard, WorkStatus.ACTIVE);

        WorkStatusResponse.WorkStatusData data = new WorkStatusResponse.WorkStatusData();

        if (activeOpt.isPresent()) {
            CheckinRecord session = activeOpt.get();
            data.setWorking(true);
            data.setSessionId(session.getId());
            data.setStartTime(session.getStartTime());
            data.setActiveMinutes(session.getActiveMinutes());
            data.setSiteName(session.getSite() != null ? session.getSite().getName() : null);

            // 查询待处理抽查
            Optional<SpotCheck> pendingCheck = spotCheckRepository.findPendingByGuardId(guardId);
            if (pendingCheck.isPresent()) {
                SpotCheck check = pendingCheck.get();
                data.setPendingSpotCheck(new WorkStatusResponse.PendingSpotCheck(
                        check.getId(),
                        check.getDeadline(),
                        check.getRemainingSeconds()
                ));
            }
        } else {
            data.setWorking(false);
            data.setSiteName(guard.getSite() != null ? guard.getSite().getName() : null);
        }

        return WorkStatusResponse.success(data);
    }

    /**
     * 验证位置是否在单位的签到范围内
     * 支持多签到地点：只要在任一签到地点范围内即可
     *
     * @param site 工作单位
     * @param latitude 当前纬度
     * @param longitude 当前经度
     * @return null表示验证通过，否则返回错误信息
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
            return null; // 验证通过
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
                logger.info("位置验证通过: 在签到地点 '{}' 范围内（距离：{}米，允许：{}米）",
                        loc.getName(), String.format("%.0f", distance), String.format("%.0f", loc.getAllowedRadius()));
                return null; // 验证通过
            }

            if (distance < minDistance) {
                minDistance = distance;
                nearestLocation = loc.getName();
            }
        }

        // 不在任何签到地点范围内
        return String.format("位置超出允许范围（距最近地点[%s]：%.0f米）", nearestLocation, minDistance);
    }
}
