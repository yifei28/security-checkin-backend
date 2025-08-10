package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.CheckinResult;
import com.duhao.security.checkinapp.dto.CheckinRequest;
import com.duhao.security.checkinapp.dto.CheckinRecordResponse;
import com.duhao.security.checkinapp.dto.CheckinStatistics;
import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.CheckinStatus;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.util.DistanceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping(value="/api", produces = "application/json; charset=UTF-8")
public class CheckinController {

    private static final Logger logger = LoggerFactory.getLogger(CheckinController.class);
    
    private final CheckinRepository checkinRepository;
    private final SecurityGuardRepository guardRepository;

    @Autowired
    public CheckinController(CheckinRepository repository, 
                            SecurityGuardRepository guardRepository) {
        this.checkinRepository = repository;
        this.guardRepository = guardRepository;
    }

    @PostMapping(path="/checkin/validate")
    public ResponseEntity<CheckinResult> prepareCheckin(@RequestBody CheckinRequest request) {
        CheckinResult checkinResult = validateCheckin(request);
        if (!checkinResult.isSuccess()){
            return ResponseEntity.badRequest().body(checkinResult);
        }
        return ResponseEntity.ok().body(checkinResult);
    }

    @PostMapping(path="/checkin")
    public ResponseEntity<CheckinResult> checkin(@RequestBody CheckinRequest request) {
        LocalDateTime checkinTime = LocalDateTime.now();
        
        logger.info("=== 签到请求开始 ===");
        logger.info("员工ID: {}", request.getEmployeeId());
        logger.info("请求时间: {}", checkinTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        logger.info("位置信息: 纬度={}, 经度={}", request.getLatitude(), request.getLongitude());
        logger.info("人脸图片URL: {}", request.getFaceImageUrl());
        
        CheckinResult checkinResult = validateCheckin(request);
        if (!checkinResult.isSuccess()) {
            logger.warn("签到验证失败: {}", checkinResult.getMessage());
            
            // 创建失败的签到记录
            SecurityGuard guard = guardRepository.findByEmployeeId(request.getEmployeeId());
            if (guard != null) {
                WorkSite site = guard.getSite();
                if (site != null) {
                    CheckinRecord failedRecord = new CheckinRecord(guard, site, 
                        request.getLatitude(), request.getLongitude(),
                        checkinTime, request.getFaceImageUrl(),
                        CheckinStatus.FAILED, checkinResult.getMessage());
                    CheckinRecord savedRecord = checkinRepository.save(failedRecord);
                    
                    logger.info("失败签到记录已保存:");
                    logCheckinRecord(savedRecord);
                }
            }
            return ResponseEntity.badRequest().body(checkinResult);
        }
        
        // 成功签到 - 创建成功记录
        SecurityGuard guard = guardRepository.findByEmployeeId(request.getEmployeeId());
        WorkSite site = guard.getSite();
        CheckinRecord successRecord = new CheckinRecord(guard, site,
            request.getLatitude(), request.getLongitude(),
            checkinTime, request.getFaceImageUrl(),
            CheckinStatus.SUCCESS, null);
        CheckinRecord savedRecord = checkinRepository.save(successRecord);
        
        logger.info("签到成功！记录已保存:");
        logCheckinRecord(savedRecord);
        logger.info("=== 签到请求结束 ===");
        
        return ResponseEntity.ok().body(checkinResult);
    }

    private CheckinResult validateCheckin(CheckinRequest request){
        logger.info("--- 开始验证签到条件 ---");
        
        // 1. 根据employeeId查询保安信息
        SecurityGuard guard = guardRepository.findByEmployeeId(request.getEmployeeId());
        if (guard == null) {
            logger.error("验证失败: 员工ID {} 不存在", request.getEmployeeId());
            return CheckinResult.fail("没有找到保安信息，请联系管理员");
        }
        logger.info("✓ 保安信息验证通过: {} ({})", guard.getName(), guard.getEmployeeId());

        // 2. 检查是否分配单位
        WorkSite site = guard.getSite();
        if (site == null) {
            logger.error("验证失败: 保安 {} 未分配工作单位", guard.getName());
            return CheckinResult.fail("尚未分配工作单位，无法签到");
        }
        logger.info("✓ 工作单位验证通过: {}", site.getName());

        // 3. 检查是否在单位附近
        if (request.getLatitude() == null || request.getLongitude() == null) {
            logger.error("验证失败: 位置信息缺失");
            return CheckinResult.fail("位置不准确，请检查定位");
        }

        // 使用新的距离计算方法
        double distance = DistanceCalculator.calculateDistance(
                request.getLatitude(), request.getLongitude(),
                site.getLatitude(), site.getLongitude()
        );
        logger.info("位置距离计算: 当前位置({}, {}) 到工作地点({}, {}) 距离 {}米", 
            request.getLatitude(), request.getLongitude(),
            site.getLatitude(), site.getLongitude(), String.format("%.1f", distance));
        logger.info("允许签到范围: {}米", site.getAllowedRadiusMeters());
        
        if (distance > site.getAllowedRadiusMeters()) {
            logger.error("验证失败: 超出签到范围 (实际距离: {}米, 允许范围: {}米)", 
                String.format("%.0f", distance), site.getAllowedRadiusMeters());
            return CheckinResult.fail(String.format("签到位置超出允许范围（实际距离：%.0f米）", distance));
        }
        logger.info("✓ 位置距离验证通过");

        // 4. 检查是否在正确的时间
        LocalDateTime now = LocalDateTime.now();
        String period = getCurrentPeriod(now);
        logger.info("当前时间: {} ({})", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), 
            period != null ? period : "非签到时间");
        
        if (period == null) {
            logger.error("验证失败: 当前时间 {} 不在签到时间段内", now.toLocalTime());
            return CheckinResult.fail("当前时间不在签到时间段内");
        }
        logger.info("✓ 签到时间验证通过: {} 时段", period);

        // 5. 检查是否已经签过到（基于guard和site以及日期）
        LocalDate today = LocalDate.now();
        List<CheckinRecord> todayRecords = checkinRepository.findByGuardAndSiteAndTimestampBetween(
                guard, site,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
        logger.info("今日已有签到记录数: {}", todayRecords.size());
        
//        if (!todayRecords.isEmpty()) {
//            // 检查是否有成功的签到记录
//            boolean hasSuccessfulCheckin = todayRecords.stream()
//                    .anyMatch(r -> r.getStatus() == CheckinStatus.SUCCESS);
//            if (hasSuccessfulCheckin) {
//                logger.error("验证失败: 今日已有成功签到记录");
//                return CheckinResult.fail("您今天已经成功签到了");
//            }
//        }

        logger.info("✓ 所有验证条件通过");
        return CheckinResult.ok();
    }


    private boolean isBetweenInclusive(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && !time.isAfter(end);
    }

    private String getCurrentPeriod(LocalDateTime checkinTime) {
        LocalTime time = checkinTime.toLocalTime();
        if (isBetweenInclusive(time, LocalTime.of(8, 0), LocalTime.of(12, 0))) {
            return "上午";
        } else if (isBetweenInclusive(time, LocalTime.of(13, 0), LocalTime.of(23, 0))) {
            return "下午";
        }
        return null;
    }

    /**
     * 记录签到记录的详细信息到日志
     */
    private void logCheckinRecord(CheckinRecord record) {
        logger.info("├─ 记录ID: {}", record.getId());
        logger.info("├─ 保安姓名: {} (员工号: {})", 
            record.getGuard() != null ? record.getGuard().getName() : "未知", 
            record.getGuard() != null ? record.getGuard().getEmployeeId() : "未知");
        logger.info("├─ 工作地点: {}", 
            record.getSite() != null ? record.getSite().getName() : "未分配");
        logger.info("├─ 签到时间: {}", 
            record.getTimestamp() != null ? record.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "未知");
        logger.info("├─ 签到状态: {}", 
            record.getStatus() != null ? record.getStatus().getValue() : "未知");
        logger.info("├─ 位置坐标: ({}, {})", record.getLatitude(), record.getLongitude());
        logger.info("├─ 人脸图片: {}", 
            record.getFaceImageUrl() != null ? record.getFaceImageUrl() : "无");
        logger.info("└─ 失败原因: {}", 
            record.getReason() != null ? record.getReason() : "无");
    }

    @GetMapping(path="/checkin")
    public ResponseEntity<CheckinRecordResponse> getAllCheckins(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String guardId,
            @RequestParam(required = false) String siteId) {
        
        try {
            logger.info("=== 管理端签到记录查询开始 ===");
            logger.info("分页参数: page={}, pageSize={}", page, pageSize);
            logger.info("排序参数: sortBy={}, sortOrder={}", sortBy, sortOrder);
            logger.info("筛选参数: startDate={}, endDate={}, status={}, guardId={}, siteId={}", 
                startDate, endDate, status, guardId, siteId);
            
            // 创建排序对象
            Sort sort = Sort.by(sortBy);
            if ("desc".equalsIgnoreCase(sortOrder)) {
                sort = sort.descending();
            } else {
                sort = sort.ascending();
            }
            
            // 创建分页对象
            Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
            
            // 解析筛选参数
            LocalDateTime startDateTime = parseDateTime(startDate);
            LocalDateTime endDateTime = parseDateTime(endDate);
            CheckinStatus statusEnum = parseStatus(status);
            Long guardIdLong = parseId(guardId, "guard_");
            Long siteIdLong = parseId(siteId, "site_");
            
            logger.info("解析后参数: startDateTime={}, endDateTime={}, statusEnum={}, guardIdLong={}, siteIdLong={}", 
                startDateTime, endDateTime, statusEnum, guardIdLong, siteIdLong);
            
            // 使用筛选查询
            Page<CheckinRecord> recordsPage = checkinRepository.findWithFilters(
                startDateTime, endDateTime, statusEnum, guardIdLong, siteIdLong, pageable);
            
            // 转换为响应格式
            List<CheckinRecordResponse.CheckinRecordData> data = recordsPage.getContent().stream()
                    .map(this::convertToRecordData)
                    .collect(Collectors.toList());
            
            // 分页信息
            CheckinRecordResponse.PaginationInfo pagination = new CheckinRecordResponse.PaginationInfo(
                    recordsPage.getTotalElements(),
                    page,
                    pageSize,
                    recordsPage.getTotalPages()
            );
            
            // 计算统计信息
            CheckinStatistics statistics = calculateStatistics(startDateTime, endDateTime, guardIdLong, siteIdLong);
            
            return ResponseEntity.ok(CheckinRecordResponse.success(data, pagination, statistics));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CheckinRecordResponse(false, null, null));
        }
    }

    @GetMapping(path="/checkin/my-records")
    public ResponseEntity<CheckinRecordResponse> getMyCheckins(
            @RequestParam String employeeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        
        try {
            // 查询保安信息
            SecurityGuard guard = guardRepository.findByEmployeeId(employeeId);
            if (guard == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new CheckinRecordResponse(false, null, null));
            }
            
            // 创建排序对象
            Sort sort = Sort.by(sortBy);
            if ("desc".equalsIgnoreCase(sortOrder)) {
                sort = sort.descending();
            } else {
                sort = sort.ascending();
            }
            
            // 创建分页对象
            Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
            
            // 查询该保安的签到记录
            Page<CheckinRecord> recordsPage = checkinRepository.findByGuard(guard, pageable);
            
            // 转换为小程序专用响应格式（包含名称而不是ID）
            List<CheckinRecordResponse.CheckinRecordData> data = recordsPage.getContent().stream()
                    .map(this::convertToMiniProgramRecordData)
                    .collect(Collectors.toList());
            
            // 分页信息
            CheckinRecordResponse.PaginationInfo pagination = new CheckinRecordResponse.PaginationInfo(
                    recordsPage.getTotalElements(),
                    page,
                    pageSize,
                    recordsPage.getTotalPages()
            );
            
            return ResponseEntity.ok(CheckinRecordResponse.success(data, pagination));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CheckinRecordResponse(false, null, null));
        }
    }


    // 转换方法 - 管理端使用（返回ID）
    private CheckinRecordResponse.CheckinRecordData convertToRecordData(CheckinRecord record) {
        return new CheckinRecordResponse.CheckinRecordData(
                "checkin_" + record.getId(),
                record.getGuard() != null ? "guard_" + record.getGuard().getId() : null,
                record.getSite() != null ? "site_" + record.getSite().getId() : null,
                record.getTimestamp() != null ? record.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                new CheckinRecordResponse.CheckinRecordData.LocationInfo(record.getLatitude(), record.getLongitude()),
                record.getFaceImageUrl(),
                record.getStatus() != null ? record.getStatus().getValue() : CheckinStatus.PENDING.getValue(),
                record.getReason()
        );
    }

    // 小程序专用转换方法 - 返回名称而不是ID
    private CheckinRecordResponse.CheckinRecordData convertToMiniProgramRecordData(CheckinRecord record) {
        return new CheckinRecordResponse.CheckinRecordData(
                "checkin_" + record.getId(),
                record.getGuard() != null ? record.getGuard().getName() : null,  // 返回保安姓名
                record.getSite() != null ? record.getSite().getName() : null,    // 返回站点名称
                record.getTimestamp() != null ? record.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                new CheckinRecordResponse.CheckinRecordData.LocationInfo(record.getLatitude(), record.getLongitude()),
                record.getFaceImageUrl(),
                record.getStatus() != null ? record.getStatus().getValue() : CheckinStatus.PENDING.getValue(),
                record.getReason()
        );
    }

    // 测试日志功能的端点（无需认证）
    @PostMapping("/test-logger")
    public ResponseEntity<Map<String, String>> testLogger(@RequestBody CheckinRequest request) {
        logger.info("=== 测试签到日志功能开始 ===");
        logger.info("员工ID: {}", request.getEmployeeId());
        logger.info("位置信息: 纬度={}, 经度={}", request.getLatitude(), request.getLongitude());
        
        // 模拟验证过程
        CheckinResult result = validateCheckin(request);
        logger.info("验证结果: {}", result.isSuccess() ? "成功" : "失败 - " + result.getMessage());
        logger.info("=== 测试签到日志功能结束 ===");
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "logged");
        response.put("message", "查看控制台日志输出");
        return ResponseEntity.ok(response);
    }

    // 临时测试端点 - 用于验证小程序API返回格式（无需认证）
    @GetMapping("/test-mini-program")
    public ResponseEntity<CheckinRecordResponse> testMiniProgramAPI(
            @RequestParam(defaultValue = "20250809-0000017-tiKUHu") String employeeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int pageSize) {
        
        try {
            // 查询保安信息
            SecurityGuard guard = guardRepository.findByEmployeeId(employeeId);
            if (guard == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new CheckinRecordResponse(false, null, null));
            }
            
            // 创建分页对象
            Sort sort = Sort.by("timestamp").descending();
            Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
            
            // 查询该保安的签到记录
            Page<CheckinRecord> recordsPage = checkinRepository.findByGuard(guard, pageable);
            
            // 转换为小程序专用响应格式（包含名称而不是ID）
            List<CheckinRecordResponse.CheckinRecordData> data = recordsPage.getContent().stream()
                    .map(this::convertToMiniProgramRecordData)
                    .collect(Collectors.toList());
            
            // 分页信息
            CheckinRecordResponse.PaginationInfo pagination = new CheckinRecordResponse.PaginationInfo(
                    recordsPage.getTotalElements(),
                    page,
                    pageSize,
                    recordsPage.getTotalPages()
            );
            
            return ResponseEntity.ok(CheckinRecordResponse.success(data, pagination));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CheckinRecordResponse(false, null, null));
        }
    }
    
    // 筛选参数解析辅助方法
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            // 支持多种日期时间格式
            if (dateTimeStr.contains("T")) {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                // 如果只有日期，默认为当天开始时间
                return LocalDate.parse(dateTimeStr).atStartOfDay();
            }
        } catch (DateTimeParseException e) {
            logger.warn("无法解析日期时间: {}", dateTimeStr, e);
            return null;
        }
    }
    
    private CheckinStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty() || "all".equalsIgnoreCase(statusStr)) {
            return null;
        }
        try {
            // 将字符串转换为对应的枚举值
            switch (statusStr.toLowerCase()) {
                case "success":
                    return CheckinStatus.SUCCESS;
                case "failed":
                    return CheckinStatus.FAILED;
                case "pending":
                    return CheckinStatus.PENDING;
                default:
                    logger.warn("未知的状态值: {}", statusStr);
                    return null;
            }
        } catch (Exception e) {
            logger.warn("无法解析状态: {}", statusStr, e);
            return null;
        }
    }
    
    private Long parseId(String idStr, String prefix) {
        if (idStr == null || idStr.trim().isEmpty() || "all".equalsIgnoreCase(idStr)) {
            return null;
        }
        try {
            // 去除前缀 (如 "guard_123" -> "123")
            String cleanId = idStr.startsWith(prefix) ? idStr.substring(prefix.length()) : idStr;
            return Long.parseLong(cleanId);
        } catch (NumberFormatException e) {
            logger.warn("无法解析ID: {} (前缀: {})", idStr, prefix, e);
            return null;
        }
    }
    
    private CheckinStatistics calculateStatistics(LocalDateTime startDateTime, LocalDateTime endDateTime, Long guardId, Long siteId) {
        try {
            logger.info("计算统计信息: startDateTime={}, endDateTime={}, guardId={}, siteId={}", 
                startDateTime, endDateTime, guardId, siteId);
            
            // 计算各种状态的记录数
            long successCount = checkinRepository.countSuccessWithFilters(startDateTime, endDateTime, guardId, siteId);
            long failedCount = checkinRepository.countFailedWithFilters(startDateTime, endDateTime, guardId, siteId);
            long pendingCount = checkinRepository.countPendingWithFilters(startDateTime, endDateTime, guardId, siteId);
            long totalRecords = successCount + failedCount + pendingCount;
            
            logger.info("统计结果: total={}, success={}, failed={}, pending={}", 
                totalRecords, successCount, failedCount, pendingCount);
            
            return new CheckinStatistics(totalRecords, successCount, failedCount, pendingCount);
        } catch (Exception e) {
            logger.error("计算统计信息时出错", e);
            return new CheckinStatistics(0, 0, 0, 0);
        }
    }

}