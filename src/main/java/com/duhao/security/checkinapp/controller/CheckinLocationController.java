package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.CheckinLocationRequest;
import com.duhao.security.checkinapp.entity.CheckinLocation;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.repository.CheckinLocationRepository;
import com.duhao.security.checkinapp.repository.WorkSiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 签到地点管理控制器
 * 每个单位可以有多个签到地点
 */
@RestController
@RequestMapping(value = "/api/sites/{siteId}/locations", produces = "application/json; charset=UTF-8")
public class CheckinLocationController {

    private static final Logger logger = LoggerFactory.getLogger(CheckinLocationController.class);

    private final CheckinLocationRepository locationRepository;
    private final WorkSiteRepository siteRepository;

    public CheckinLocationController(CheckinLocationRepository locationRepository, WorkSiteRepository siteRepository) {
        this.locationRepository = locationRepository;
        this.siteRepository = siteRepository;
    }

    /**
     * 获取单位的所有签到地点
     * GET /api/sites/{siteId}/locations
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLocations(@PathVariable Long siteId) {
        logger.info("查询单位签到地点: siteId={}", siteId);

        if (!siteRepository.existsById(siteId)) {
            return notFound("单位不存在");
        }

        List<CheckinLocation> locations = locationRepository.findBySiteId(siteId);
        List<LocationResponse> data = locations.stream()
                .map(this::toResponse)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("count", data.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 添加签到地点
     * POST /api/sites/{siteId}/locations
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addLocation(
            @PathVariable Long siteId,
            @RequestBody CheckinLocationRequest request) {
        logger.info("添加签到地点: siteId={}, name={}", siteId, request.getName());

        // 验证单位是否存在
        WorkSite site = siteRepository.findById(siteId).orElse(null);
        if (site == null) {
            return notFound("单位不存在");
        }

        // 验证必填字段
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return badRequest("地点名称不能为空");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            return badRequest("经纬度不能为空");
        }

        // 创建签到地点
        CheckinLocation location = new CheckinLocation(
                request.getName().trim(),
                request.getLatitude(),
                request.getLongitude(),
                request.getAllowedRadius() != null ? request.getAllowedRadius() : 100.0,
                site
        );

        CheckinLocation saved = locationRepository.save(location);
        logger.info("签到地点添加成功: id={}", saved.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", toResponse(saved));
        response.put("message", "签到地点添加成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 修改签到地点
     * PUT /api/sites/{siteId}/locations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateLocation(
            @PathVariable Long siteId,
            @PathVariable Long id,
            @RequestBody CheckinLocationRequest request) {
        logger.info("修改签到地点: siteId={}, id={}", siteId, id);

        // 验证单位是否存在
        if (!siteRepository.existsById(siteId)) {
            return notFound("单位不存在");
        }

        // 验证签到地点是否存在且属于该单位
        CheckinLocation location = locationRepository.findById(id).orElse(null);
        if (location == null || !location.getSite().getId().equals(siteId)) {
            return notFound("签到地点不存在");
        }

        // 更新字段
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            location.setName(request.getName().trim());
        }
        if (request.getLatitude() != null) {
            location.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            location.setLongitude(request.getLongitude());
        }
        if (request.getAllowedRadius() != null) {
            location.setAllowedRadius(request.getAllowedRadius());
        }

        CheckinLocation saved = locationRepository.save(location);
        logger.info("签到地点修改成功: id={}", saved.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", toResponse(saved));
        response.put("message", "签到地点修改成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 删除签到地点
     * DELETE /api/sites/{siteId}/locations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteLocation(
            @PathVariable Long siteId,
            @PathVariable Long id) {
        logger.info("删除签到地点: siteId={}, id={}", siteId, id);

        // 验证单位是否存在
        if (!siteRepository.existsById(siteId)) {
            return notFound("单位不存在");
        }

        // 验证签到地点是否存在且属于该单位
        if (!locationRepository.existsByIdAndSiteId(id, siteId)) {
            return notFound("签到地点不存在");
        }

        locationRepository.deleteById(id);
        logger.info("签到地点删除成功: id={}", id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "签到地点删除成功");

        return ResponseEntity.ok(response);
    }

    // ==================== 辅助方法 ====================

    private LocationResponse toResponse(CheckinLocation location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAllowedRadius()
        );
    }

    private ResponseEntity<Map<String, Object>> notFound(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }

    // ==================== 响应DTO ====================

    public record LocationResponse(
            Long id,
            String name,
            Double latitude,
            Double longitude,
            Double allowedRadius
    ) {}
}
