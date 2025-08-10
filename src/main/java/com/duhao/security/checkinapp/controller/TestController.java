package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.CheckinRecordResponse;
import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试控制器 - 仅在test-data profile下可用
 * 用于测试前端兼容性，无需认证
 */
@RestController
@RequestMapping("/api/test")
@Profile("test-data")
public class TestController {
    
    @Autowired
    private CheckinRepository checkinRepository;
    
    @GetMapping("/checkin-records")
    public ResponseEntity<CheckinRecordResponse> getTestCheckinRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        // 创建分页对象
        Sort sort = Sort.by("timestamp").descending();
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        
        // 查询数据
        Page<CheckinRecord> recordsPage = checkinRepository.findAll(pageable);
        
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
        
        return ResponseEntity.ok(CheckinRecordResponse.success(data, pagination));
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
        response.put("localDateTimeWithZ", localDateTime.format(isoFormatter) + "Z");
        response.put("zonedDateTime", zonedDateTime.toString());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    private CheckinRecordResponse.CheckinRecordData convertToRecordData(CheckinRecord record) {
        return new CheckinRecordResponse.CheckinRecordData(
                "checkin_" + record.getId(),
                record.getGuard() != null ? "guard_" + record.getGuard().getId() : null,
                record.getSite() != null ? "site_" + record.getSite().getId() : null,
                record.getTimestamp() != null ? record.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z" : null,
                new CheckinRecordResponse.CheckinRecordData.LocationInfo(record.getLatitude(), record.getLongitude()),
                record.getFaceImageUrl(),
                record.getStatus() != null ? record.getStatus().getValue() : null,
                record.getReason()
        );
    }
}