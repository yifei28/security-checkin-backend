package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.OnDutyGuardInfo;
import com.duhao.security.checkinapp.dto.PaginationInfo;
import com.duhao.security.checkinapp.dto.SiteGuardResponse;
import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.entity.WorkStatus;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.repository.WorkSiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 单位统计控制器
 * 提供单位的统计数据和保安列表
 */
@RestController
@RequestMapping(value = "/api/sites/{siteId}", produces = "application/json; charset=UTF-8")
public class SiteStatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(SiteStatisticsController.class);

    private final WorkSiteRepository siteRepository;
    private final SecurityGuardRepository guardRepository;
    private final CheckinRepository checkinRepository;

    public SiteStatisticsController(WorkSiteRepository siteRepository,
                                     SecurityGuardRepository guardRepository,
                                     CheckinRepository checkinRepository) {
        this.siteRepository = siteRepository;
        this.guardRepository = guardRepository;
        this.checkinRepository = checkinRepository;
    }

    /**
     * 获取单位统计数据（前端地图显示用）
     * GET /api/sites/{siteId}/statistics
     *
     * 支持两种格式：
     * - /api/sites/5/statistics (数字ID)
     * - /api/sites/site_5/statistics (带前缀的ID)
     *
     * 响应格式（匹配前端期望）：
     * {
     *   "success": true,
     *   "data": {
     *     "onDutyCount": 3,
     *     "totalGuards": 5,
     *     "checkinRate": 60,
     *     "onDutyGuards": [...]
     *   }
     * }
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@PathVariable String siteId) {
        logger.info("查询单位统计: siteId={}", siteId);

        // 解析 siteId：支持 "5" 或 "site_5" 格式
        Long parsedSiteId = parseSiteId(siteId);
        if (parsedSiteId == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "无效的单位ID格式");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // 1. 验证单位存在
        WorkSite site = siteRepository.findById(parsedSiteId).orElse(null);
        if (site == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "单位不存在");
            return ResponseEntity.ok(errorResponse);
        }

        // 2. 保安总数
        int totalGuards = (int) guardRepository.countBySiteId(parsedSiteId);

        // 3. 今日统计
        // 签到率 = 分配到该站点且今日打卡的保安数 / 分配到该站点的保安总数
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        int todayAssignedGuardsCheckedIn = (int) checkinRepository.countDistinctAssignedGuardsBySiteAndTimestampBetween(parsedSiteId, todayStart, todayEnd);
        int checkinRate = totalGuards > 0 ? (int) Math.round((double) todayAssignedGuardsCheckedIn / totalGuards * 100) : 0;

        // 4. 查询当前在岗人员（包含位置信息）
        List<CheckinRecord> activeRecords = checkinRepository.findBySiteAndStatus(site, WorkStatus.ACTIVE);
        int onDutyCount = activeRecords.size();

        List<OnDutyGuardInfo> onDutyGuards = activeRecords.stream()
                .map(record -> new OnDutyGuardInfo(
                        record.getGuard().getId(),
                        record.getGuard().getName(),
                        record.getStartLatitude(),
                        record.getStartLongitude(),
                        record.getStartTime()
                ))
                .toList();

        // 5. 组装响应（扁平结构，匹配前端期望）
        Map<String, Object> data = new HashMap<>();
        data.put("onDutyCount", onDutyCount);
        data.put("totalGuards", totalGuards);
        data.put("checkinRate", checkinRate);
        data.put("onDutyGuards", onDutyGuards);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        logger.info("单位统计查询完成: siteId={}, totalGuards={}, onDutyCount={}, checkinRate={}%",
                parsedSiteId, totalGuards, onDutyCount, checkinRate);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取单位保安列表
     * GET /api/sites/{siteId}/guards
     *
     * 支持两种格式：
     * - /api/sites/5/guards (数字ID)
     * - /api/sites/site_5/guards (带前缀的ID)
     */
    @GetMapping("/guards")
    public ResponseEntity<Map<String, Object>> getGuards(
            @PathVariable String siteId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {

        logger.info("查询单位保安列表: siteId={}, page={}, pageSize={}", siteId, page, pageSize);

        // 解析 siteId：支持 "5" 或 "site_5" 格式
        Long parsedSiteId = parseSiteId(siteId);
        if (parsedSiteId == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "无效的单位ID格式");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // 1. 验证单位存在
        if (!siteRepository.existsById(parsedSiteId)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "单位不存在");
            return ResponseEntity.ok(response);
        }

        // 2. 分页查询保安
        Sort sort = Sort.by(sortOrder.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        Page<SecurityGuard> guardsPage = guardRepository.findBySiteId(parsedSiteId, pageable);

        // 3. 今日时间范围
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        // 4. 转换为响应DTO
        List<SiteGuardResponse> guardResponses = guardsPage.getContent().stream()
                .map(guard -> {
                    // 查询是否在岗
                    Optional<CheckinRecord> activeRecord = checkinRepository.findByGuardAndStatus(guard, WorkStatus.ACTIVE);
                    boolean isOnDuty = activeRecord.isPresent();
                    Long currentCheckinId = activeRecord.map(CheckinRecord::getId).orElse(null);

                    // 查询今日签到次数
                    int todayCount = (int) checkinRepository.countByGuardAndTimestampBetween(
                            guard.getId(), todayStart, todayEnd);

                    return new SiteGuardResponse(
                            guard.getId(),
                            guard.getName(),
                            guard.getEmployeeId(),
                            guard.getRole() != null ? guard.getRole().name() : null,
                            guard.getRole() != null ? guard.getRole().getDisplayName() : null,
                            SiteGuardResponse.maskPhoneNumber(guard.getPhoneNumber()),
                            isOnDuty,
                            currentCheckinId,
                            todayCount
                    );
                })
                .toList();

        // 5. 分页信息
        PaginationInfo pagination = PaginationInfo.fromPage(guardsPage);

        // 6. 组装响应
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", guardResponses);
        response.put("pagination", pagination);

        logger.info("单位保安列表查询完成: siteId={}, count={}", siteId, guardResponses.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 解析 siteId：支持 "5" 或 "site_5" 格式
     * @param siteId 原始ID字符串
     * @return 解析后的Long ID，无效则返回null
     */
    private Long parseSiteId(String siteId) {
        if (siteId == null || siteId.isBlank()) {
            return null;
        }
        try {
            // 如果有 "site_" 前缀，去掉前缀
            String numericPart = siteId.startsWith("site_") ? siteId.substring(5) : siteId;
            return Long.parseLong(numericPart);
        } catch (NumberFormatException e) {
            logger.warn("无法解析 siteId: {}", siteId);
            return null;
        }
    }
}
