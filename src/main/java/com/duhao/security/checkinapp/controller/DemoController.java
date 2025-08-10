package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.CheckinRecordResponse;
import com.duhao.security.checkinapp.dto.CheckinRequest;
import com.duhao.security.checkinapp.dto.CheckinResult;
import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.controller.CheckinController;
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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 演示控制器 - 展示小程序API数据格式差异
 */
@RestController
@RequestMapping("/demo")
public class DemoController {
    
    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);
    
    @Autowired
    private CheckinRepository checkinRepository;
    
    @Autowired 
    private SecurityGuardRepository guardRepository;
    
    @Autowired
    private CheckinController checkinController;
    
    @GetMapping("/mini-program-format")
    public ResponseEntity<CheckinRecordResponse> getMiniProgramFormat(
            @RequestParam(defaultValue = "20250809-0000017-tiKUHu") String employeeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int pageSize) {
        
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
                    .map(record -> new CheckinRecordResponse.CheckinRecordData(
                            "checkin_" + record.getId(),
                            record.getGuard() != null ? record.getGuard().getName() : null,  // 返回保安姓名
                            record.getSite() != null ? record.getSite().getName() : null,    // 返回站点名称
                            record.getTimestamp() != null ? record.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                            new CheckinRecordResponse.CheckinRecordData.LocationInfo(record.getLatitude(), record.getLongitude()),
                            record.getFaceImageUrl(),
                            record.getStatus() != null ? record.getStatus().getValue() : "PENDING",
                            record.getReason()
                    ))
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
    
    @GetMapping("/admin-format") 
    public ResponseEntity<CheckinRecordResponse> getAdminFormat(
            @RequestParam(defaultValue = "20250809-0000017-tiKUHu") String employeeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int pageSize) {
        
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
            
            // 转换为管理端响应格式（返回ID）
            List<CheckinRecordResponse.CheckinRecordData> data = recordsPage.getContent().stream()
                    .map(record -> new CheckinRecordResponse.CheckinRecordData(
                            "checkin_" + record.getId(),
                            record.getGuard() != null ? "guard_" + record.getGuard().getId() : null,  // 返回保安ID
                            record.getSite() != null ? "site_" + record.getSite().getId() : null,     // 返回站点ID
                            record.getTimestamp() != null ? record.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                            new CheckinRecordResponse.CheckinRecordData.LocationInfo(record.getLatitude(), record.getLongitude()),
                            record.getFaceImageUrl(),
                            record.getStatus() != null ? record.getStatus().getValue() : "PENDING",
                            record.getReason()
                    ))
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
    
    @GetMapping("/timezone")
    public ResponseEntity<java.util.Map<String, Object>> testTimezone() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        // 获取当前时间的不同表示
        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.now();
        java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.now();
        
        // 格式化时间
        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        response.put("systemDefaultTimeZone", java.util.TimeZone.getDefault().getID());
        response.put("localDateTime", localDateTime.format(isoFormatter));
        response.put("localDateTimeWithoutZ", localDateTime.format(isoFormatter));
        response.put("zonedDateTime", zonedDateTime.toString());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    // 测试筛选功能的端点（无需认证）
    @GetMapping("/test-filters")
    public ResponseEntity<CheckinRecordResponse> testFilters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status) {
        
        try {
            // 直接调用CheckinController的筛选逻辑
            return checkinController.getAllCheckins(page, pageSize, "timestamp", "desc", 
                startDate, endDate, status, null, null);
            
        } catch (Exception e) {
            logger.error("测试筛选功能时发生错误", e);
            return ResponseEntity.status(500)
                    .body(new CheckinRecordResponse(false, null, null));
        }
    }
    
    @PostMapping("/test-logger")
    public ResponseEntity<java.util.Map<String, String>> testLogger(@RequestBody CheckinRequest request) {
        logger.info("=== 测试签到日志功能开始（Demo版本）===");
        logger.info("员工ID: {}", request.getEmployeeId());
        logger.info("位置信息: 纬度={}, 经度={}", request.getLatitude(), request.getLongitude());
        logger.info("人脸图片URL: {}", request.getFaceImageUrl());
        
        // 直接调用CheckinController的validateCheckin方法来测试日志
        try {
            logger.info("准备调用验证方法...");
            // 这里我们无法直接调用私有方法，但我们可以测试日志系统
            logger.info("验证日志系统正常工作");
            logger.info("=== 测试签到日志功能结束（Demo版本）===");
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("status", "logged");
            response.put("message", "请查看控制台输出验证日志功能");
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("测试日志时发生错误", e);
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}