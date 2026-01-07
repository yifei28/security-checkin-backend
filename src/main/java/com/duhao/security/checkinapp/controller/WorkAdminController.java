package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.SpotCheck;
import com.duhao.security.checkinapp.entity.WorkStatus;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SpotCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作记录管理控制器（管理端）
 * 查看工作片段和抽查汇总
 */
@RestController
@RequestMapping(value = "/api/admin/work", produces = "application/json; charset=UTF-8")
public class WorkAdminController {

    private static final Logger logger = LoggerFactory.getLogger(WorkAdminController.class);

    private final CheckinRepository checkinRepository;
    private final SpotCheckRepository spotCheckRepository;

    public WorkAdminController(CheckinRepository checkinRepository, SpotCheckRepository spotCheckRepository) {
        this.checkinRepository = checkinRepository;
        this.spotCheckRepository = spotCheckRepository;
    }

    /**
     * 查询工作记录
     * GET /api/admin/work/records
     *
     * 支持两种日期格式：
     * - YYYY-MM-DD（如 2026-01-07）
     * - YYYY-MM-DDTHH:mm:ss（如 2026-01-07T00:00:00）
     */
    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getWorkRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long guardId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        // 解析日期：支持 YYYY-MM-DD 和 YYYY-MM-DDTHH:mm:ss 两种格式
        LocalDateTime startDateTime = parseDate(startDate, true);
        LocalDateTime endDateTime = parseDate(endDate, false);

        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy)
        );

        WorkStatus statusEnum = status != null ? WorkStatus.valueOf(status) : null;

        Page<CheckinRecord> records = checkinRepository.findWithFilters(
                startDateTime, endDateTime, statusEnum, guardId, siteId, pageable);

        List<WorkSessionRecord> sessionRecords = records.getContent().stream()
                .map(this::toWorkSessionRecord)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", sessionRecords);
        response.put("pagination", Map.of(
                "total", records.getTotalElements(),
                "page", page,
                "pageSize", pageSize,
                "totalPages", records.getTotalPages()
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 查询当前在岗保安
     * GET /api/admin/work/active
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveGuards() {
        List<CheckinRecord> activeRecords = checkinRepository.findByStatus(WorkStatus.ACTIVE);

        List<WorkSessionRecord> sessionRecords = activeRecords.stream()
                .map(this::toWorkSessionRecord)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", sessionRecords);
        response.put("count", sessionRecords.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取工作记录详情
     * GET /api/admin/work/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getWorkDetail(@PathVariable Long id) {
        return checkinRepository.findById(id)
                .map(record -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", toWorkSessionRecord(record));
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "工作记录不存在");
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * 获取工作记录的抽查列表
     * GET /api/admin/work/{id}/spot-checks
     */
    @GetMapping("/{id}/spot-checks")
    public ResponseEntity<Map<String, Object>> getWorkSpotChecks(@PathVariable Long id) {
        // 先检查工作记录是否存在
        if (!checkinRepository.existsById(id)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "工作记录不存在");
            return ResponseEntity.ok(response);
        }

        List<SpotCheck> spotChecks = spotCheckRepository.findByCheckinRecordId(id);
        List<SpotCheckRecord> records = spotChecks.stream()
                .map(this::toSpotCheckRecord)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", records);
        response.put("count", records.size());

        return ResponseEntity.ok(response);
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析日期字符串，支持两种格式：
     * - YYYY-MM-DD (如 2026-01-07)
     * - YYYY-MM-DDTHH:mm:ss (如 2026-01-07T00:00:00)
     *
     * @param dateStr 日期字符串
     * @param isStart 是否为开始日期（true=00:00:00，false=次日00:00:00）
     * @return LocalDateTime 或 null
     */
    private LocalDateTime parseDate(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            if (dateStr.contains("T")) {
                // ISO 格式：2026-01-07T00:00:00
                // 对于结束日期，使用传入的时间或次日开始
                LocalDateTime parsed = LocalDateTime.parse(dateStr);
                if (!isStart) {
                    // 结束日期：如果传入23:59:59，转换为次日00:00:00以包含整天
                    return parsed.toLocalDate().plusDays(1).atStartOfDay();
                }
                return parsed;
            } else {
                // 简单格式：2026-01-07
                LocalDate date = LocalDate.parse(dateStr);
                if (isStart) {
                    return date.atStartOfDay();
                } else {
                    return date.plusDays(1).atStartOfDay();
                }
            }
        } catch (Exception e) {
            logger.warn("日期解析失败: {}", dateStr);
            return null;
        }
    }

    private SpotCheckRecord toSpotCheckRecord(SpotCheck spotCheck) {
        SpotCheckRecord record = new SpotCheckRecord();
        record.setId(spotCheck.getId());
        record.setCreatedAt(spotCheck.getCreatedAt());
        record.setDeadline(spotCheck.getDeadline());
        record.setCompletedAt(spotCheck.getCompletedAt());
        record.setStatus(spotCheck.getStatus().name());
        record.setStatusName(spotCheck.getStatus().getDisplayName());
        record.setTriggerType(spotCheck.getTriggerType().name());
        record.setTriggerTypeName(spotCheck.getTriggerType().getDisplayName());
        record.setLatitude(spotCheck.getLatitude());
        record.setLongitude(spotCheck.getLongitude());
        record.setFaceImageUrl(spotCheck.getFaceImageUrl());
        return record;
    }

    private WorkSessionRecord toWorkSessionRecord(CheckinRecord record) {
        WorkSessionRecord session = new WorkSessionRecord();
        session.setId(record.getId());
        session.setStartTime(record.getStartTime());
        session.setEndTime(record.getEndTime());
        session.setStatus(record.getStatus().name());
        session.setStatusName(record.getStatus().getDisplayName());
        session.setDurationMinutes(record.getDurationMinutes());
        session.setSpotCheckTotal(record.getSpotCheckTotal());
        session.setSpotCheckPassed(record.getSpotCheckPassed());

        // 位置信息
        session.setStartLatitude(record.getStartLatitude());
        session.setStartLongitude(record.getStartLongitude());
        session.setEndLatitude(record.getEndLatitude());
        session.setEndLongitude(record.getEndLongitude());

        if (record.getGuard() != null) {
            session.setGuardId(record.getGuard().getId());
            session.setGuardName(record.getGuard().getName());
        }

        if (record.getSite() != null) {
            session.setSiteId(record.getSite().getId());
            session.setSiteName(record.getSite().getName());
        }

        // 计算在岗时长
        if (record.getStatus() == WorkStatus.ACTIVE) {
            session.setActiveMinutes(record.getActiveMinutes());
        }

        return session;
    }

    /**
     * 工作片段记录
     */
    public static class WorkSessionRecord {
        private Long id;
        private Long guardId;
        private String guardName;
        private Long siteId;
        private String siteName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private String statusName;
        private Long durationMinutes;
        private Long activeMinutes;
        private Integer spotCheckTotal;
        private Integer spotCheckPassed;
        private Double startLatitude;
        private Double startLongitude;
        private Double endLatitude;
        private Double endLongitude;

        // Getters and Setters

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
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

        public Long getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Long durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public Long getActiveMinutes() {
            return activeMinutes;
        }

        public void setActiveMinutes(Long activeMinutes) {
            this.activeMinutes = activeMinutes;
        }

        public Integer getSpotCheckTotal() {
            return spotCheckTotal;
        }

        public void setSpotCheckTotal(Integer spotCheckTotal) {
            this.spotCheckTotal = spotCheckTotal;
        }

        public Integer getSpotCheckPassed() {
            return spotCheckPassed;
        }

        public void setSpotCheckPassed(Integer spotCheckPassed) {
            this.spotCheckPassed = spotCheckPassed;
        }

        public Double getStartLatitude() {
            return startLatitude;
        }

        public void setStartLatitude(Double startLatitude) {
            this.startLatitude = startLatitude;
        }

        public Double getStartLongitude() {
            return startLongitude;
        }

        public void setStartLongitude(Double startLongitude) {
            this.startLongitude = startLongitude;
        }

        public Double getEndLatitude() {
            return endLatitude;
        }

        public void setEndLatitude(Double endLatitude) {
            this.endLatitude = endLatitude;
        }

        public Double getEndLongitude() {
            return endLongitude;
        }

        public void setEndLongitude(Double endLongitude) {
            this.endLongitude = endLongitude;
        }
    }

    /**
     * 抽查记录
     */
    public static class SpotCheckRecord {
        private Long id;
        private LocalDateTime createdAt;
        private LocalDateTime deadline;
        private LocalDateTime completedAt;
        private String status;
        private String statusName;
        private String triggerType;
        private String triggerTypeName;
        private Double latitude;
        private Double longitude;
        private String faceImageUrl;

        // Getters and Setters

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public String getTriggerTypeName() {
            return triggerTypeName;
        }

        public void setTriggerTypeName(String triggerTypeName) {
            this.triggerTypeName = triggerTypeName;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public String getFaceImageUrl() {
            return faceImageUrl;
        }

        public void setFaceImageUrl(String faceImageUrl) {
            this.faceImageUrl = faceImageUrl;
        }
    }
}
