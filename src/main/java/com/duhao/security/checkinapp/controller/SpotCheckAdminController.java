package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.entity.SpotCheck;
import com.duhao.security.checkinapp.entity.SpotCheckStatus;
import com.duhao.security.checkinapp.entity.SpotCheckTriggerType;
import com.duhao.security.checkinapp.service.SpotCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 抽查管理控制器（管理端）
 * 处理抽查管理操作
 */
@RestController
@RequestMapping(value = "/api/admin/spot-check", produces = "application/json; charset=UTF-8")
public class SpotCheckAdminController {

    private static final Logger logger = LoggerFactory.getLogger(SpotCheckAdminController.class);

    private final SpotCheckService spotCheckService;

    public SpotCheckAdminController(SpotCheckService spotCheckService) {
        this.spotCheckService = spotCheckService;
    }

    /**
     * 手动触发抽查
     * POST /api/admin/spot-check/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerSpotChecks(
            @RequestBody TriggerRequest request) {

        logger.info("手动触发抽查: guardIds={}", request.getGuardIds());

        if (request.getGuardIds() == null || request.getGuardIds().isEmpty()) {
            return ResponseEntity.ok(errorResponse("请指定保安ID列表"));
        }

        List<SpotCheck> createdChecks = spotCheckService.triggerManualSpotChecks(
                request.getGuardIds(), null);

        List<SpotCheckRecord> records = createdChecks.stream()
                .map(this::toSpotCheckRecord)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", String.format("成功触发 %d 个抽查", createdChecks.size()));
        response.put("data", records);

        return ResponseEntity.ok(response);
    }

    /**
     * 查询抽查记录
     * GET /api/admin/spot-check/records
     */
    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getSpotCheckRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long guardId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String triggerType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy)
        );

        SpotCheckStatus statusEnum = status != null ? SpotCheckStatus.valueOf(status) : null;
        SpotCheckTriggerType triggerTypeEnum = triggerType != null ? SpotCheckTriggerType.valueOf(triggerType) : null;

        Page<SpotCheck> spotChecks = spotCheckService.findWithFilters(
                startDate, endDate, statusEnum, guardId, siteId, triggerTypeEnum, pageable);

        List<SpotCheckRecord> records = spotChecks.getContent().stream()
                .map(this::toSpotCheckRecord)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", records);
        response.put("pagination", Map.of(
                "total", spotChecks.getTotalElements(),
                "page", page,
                "pageSize", pageSize,
                "totalPages", spotChecks.getTotalPages()
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 取消抽查
     * DELETE /api/admin/spot-check/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancelSpotCheck(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "管理员取消") String reason) {

        logger.info("取消抽查: spotCheckId={}, reason={}", id, reason);

        boolean success = spotCheckService.cancelSpotCheck(id, reason);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "取消成功" : "取消失败，抽查不存在或状态不允许取消");

        return ResponseEntity.ok(response);
    }

    /**
     * 获取抽查统计
     * GET /api/admin/spot-check/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long guardId,
            @RequestParam(required = false) Long siteId) {

        SpotCheckService.SpotCheckStatistics stats = spotCheckService.getStatistics(
                startDate, endDate, guardId, siteId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
                "totalCount", stats.totalCount(),
                "completedCount", stats.completedCount(),
                "missedCount", stats.missedCount(),
                "pendingCount", stats.pendingCount(),
                "completionRate", stats.completionRate()
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 获取今日抽查计划/记录
     * GET /api/admin/spot-check/today
     */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodaySpotChecks() {
        List<SpotCheck> todayChecks = spotCheckService.getTodaySchedule();

        List<SpotCheckRecord> records = todayChecks.stream()
                .map(this::toSpotCheckRecord)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", records);
        response.put("count", records.size());

        return ResponseEntity.ok(response);
    }

    // ==================== 辅助方法和内部类 ====================

    private SpotCheckRecord toSpotCheckRecord(SpotCheck spotCheck) {
        SpotCheckRecord record = new SpotCheckRecord();
        record.setId(spotCheck.getId());
        record.setCreatedAt(spotCheck.getCreatedAt());
        record.setDeadline(spotCheck.getDeadline());
        record.setCompletedAt(spotCheck.getCompletedAt());
        record.setStatus(spotCheck.getStatus().name());
        record.setStatusName(spotCheck.getStatus().getDisplayName());
        record.setTriggerType(spotCheck.getTriggerType().name());
        record.setRemainingSeconds(spotCheck.getRemainingSeconds());

        if (spotCheck.getGuard() != null) {
            record.setGuardId(spotCheck.getGuard().getId());
            record.setGuardName(spotCheck.getGuard().getName());
        }

        if (spotCheck.getSite() != null) {
            record.setSiteId(spotCheck.getSite().getId());
            record.setSiteName(spotCheck.getSite().getName());
        }

        if (spotCheck.getCheckinRecord() != null) {
            record.setSessionId(spotCheck.getCheckinRecord().getId());
        }

        return record;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    /**
     * 触发请求
     */
    public static class TriggerRequest {
        private List<Long> guardIds;

        public List<Long> getGuardIds() {
            return guardIds;
        }

        public void setGuardIds(List<Long> guardIds) {
            this.guardIds = guardIds;
        }
    }

    /**
     * 抽查记录响应
     */
    public static class SpotCheckRecord {
        private Long id;
        private Long sessionId;
        private Long guardId;
        private String guardName;
        private Long siteId;
        private String siteName;
        private LocalDateTime createdAt;
        private LocalDateTime deadline;
        private LocalDateTime completedAt;
        private String status;
        private String statusName;
        private String triggerType;
        private long remainingSeconds;

        // Getters and Setters

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getGuardId() {
            return guardId;
        }

        public void setGuardId(Long guardId) {
            this.guardId = guardId;
        }

        public String getGuardName() {
            return guardName;
        }

        public void setGuardName(String guardName) {
            this.guardName = guardName;
        }

        public Long getSiteId() {
            return siteId;
        }

        public void setSiteId(Long siteId) {
            this.siteId = siteId;
        }

        public String getSiteName() {
            return siteName;
        }

        public void setSiteName(String siteName) {
            this.siteName = siteName;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getDeadline() {
            return deadline;
        }

        public void setDeadline(LocalDateTime deadline) {
            this.deadline = deadline;
        }

        public LocalDateTime getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusName() {
            return statusName;
        }

        public void setStatusName(String statusName) {
            this.statusName = statusName;
        }

        public String getTriggerType() {
            return triggerType;
        }

        public void setTriggerType(String triggerType) {
            this.triggerType = triggerType;
        }

        public long getRemainingSeconds() {
            return remainingSeconds;
        }

        public void setRemainingSeconds(long remainingSeconds) {
            this.remainingSeconds = remainingSeconds;
        }
    }
}
