package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.CheckinLocationResponse;
import com.duhao.security.checkinapp.dto.PaginationInfo;
import com.duhao.security.checkinapp.dto.SiteResponse;
import com.duhao.security.checkinapp.entity.CheckinLocation;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.entity.WorkStatus;
import com.duhao.security.checkinapp.repository.CheckinLocationRepository;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.repository.WorkSiteRepository;
import com.duhao.security.checkinapp.repository.CheckinRepository;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sites")
public class WorkSiteController {
    private static final Logger logger = LoggerFactory.getLogger(WorkSiteController.class);

    private final WorkSiteRepository workSiteRepository;
    private final SecurityGuardRepository guardRepository;
    private final CheckinRepository checkinRepository;
    private final CheckinLocationRepository locationRepository;

    @Autowired
    public WorkSiteController(WorkSiteRepository workSiteRepository,
                             SecurityGuardRepository guardRepository,
                             CheckinRepository checkinRepository,
                             CheckinLocationRepository locationRepository) {
        this.workSiteRepository = workSiteRepository;
        this.guardRepository = guardRepository;
        this.checkinRepository = checkinRepository;
        this.locationRepository = locationRepository;
    }

    @PostMapping
    public ResponseEntity<?> addWorkSite(@RequestBody WorkSite workSite){
        return ResponseEntity.ok(workSiteRepository.save(workSite));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkSite> updateSite(@PathVariable Long id, @RequestBody WorkSite updatedSite) {
        return workSiteRepository.findById(id).map(existing -> {
            existing.setName(updatedSite.getName());
            existing.setLatitude(updatedSite.getLatitude());
            existing.setLongitude(updatedSite.getLongitude());
            existing.setAllowedRadiusMeters(updatedSite.getAllowedRadiusMeters());
            return ResponseEntity.ok(workSiteRepository.save(existing));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSite(@PathVariable Long id) {
        // 检查WorkSite是否存在
        return workSiteRepository.findById(id).map(workSite -> {
            // 先删除该站点的所有签到记录
            checkinRepository.deleteAll(checkinRepository.findBySite(workSite));
            
            // 将分配给该站点的保安的site字段设为null
            guardRepository.findBySite(workSite).forEach(guard -> {
                guard.setSite(null);
                guardRepository.save(guard);
            });
            
            // 最后删除站点
            workSiteRepository.deleteById(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<SiteResponse> getAllWorkSites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            // 筛选参数
            @RequestParam(required = false) String name) {
        try {
            logger.info("=== 单位列表查询开始 ===");
            logger.info("分页参数: page={}, pageSize={}, sortBy={}, sortOrder={}", page, pageSize, sortBy, sortOrder);
            logger.info("筛选参数: name={}", name);

            // 创建排序对象
            Sort sort = Sort.by(sortBy);
            if ("desc".equalsIgnoreCase(sortOrder)) {
                sort = sort.descending();
            } else {
                sort = sort.ascending();
            }

            // 创建分页对象 (Spring 页码从0开始，前端从1开始)
            Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

            // 使用筛选查询
            Page<WorkSite> sitesPage = workSiteRepository.findWithFilters(name, pageable);

            List<SiteResponse.SiteData> data = sitesPage.getContent().stream()
                    .map(this::convertToSiteData)
                    .collect(Collectors.toList());

            PaginationInfo pagination = PaginationInfo.fromPage(sitesPage);

            logger.info("查询结果: total={}, 当前页数据量={}", pagination.getTotal(), data.size());
            return ResponseEntity.ok(SiteResponse.success(data, pagination));

        } catch (Exception e) {
            logger.error("单位列表查询失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SiteResponse(false, null));
        }
    }
    
    // 转换方法
    private SiteResponse.SiteData convertToSiteData(WorkSite site) {
        // 获取分配给该站点的保安ID列表
        List<String> assignedGuardIds = guardRepository.findBySite(site).stream()
                .map(guard -> "guard_" + guard.getId())
                .collect(Collectors.toList());

        // 获取统计数据
        int locationCount = (int) locationRepository.countBySiteId(site.getId());
        int guardCount = (int) guardRepository.countBySiteId(site.getId());
        int onDutyNow = (int) checkinRepository.countBySiteAndStatus(site.getId(), WorkStatus.ACTIVE);

        return new SiteResponse.SiteData(
                site.getId(),
                site.getName(),
                site.getLatitude(),
                site.getLongitude(),
                site.getAllowedRadiusMeters(),
                assignedGuardIds,
                true, // 假设所有站点都是活跃状态
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), // 创建时间暂时用当前时间
                locationCount,
                guardCount,
                onDutyNow
        );
    }

    /**
     * 获取单位详情（包含签到地点列表）
     * GET /api/sites/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSiteDetail(@PathVariable Long id) {
        logger.info("查询单位详情: id={}", id);

        return workSiteRepository.findById(id).map(site -> {
            // 获取签到地点列表
            List<CheckinLocation> locations = locationRepository.findBySiteId(id);
            List<CheckinLocationResponse> locationResponses = locations.stream()
                    .map(loc -> new CheckinLocationResponse(
                            loc.getId(),
                            loc.getName(),
                            loc.getLatitude(),
                            loc.getLongitude(),
                            loc.getAllowedRadius()
                    ))
                    .collect(Collectors.toList());

            // 获取统计数据
            int guardCount = (int) guardRepository.countBySiteId(id);
            int onDutyNow = (int) checkinRepository.countBySiteAndStatus(id, WorkStatus.ACTIVE);

            // 构建详情响应
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("data", java.util.Map.of(
                    "id", site.getId(),
                    "name", site.getName(),
                    "latitude", site.getLatitude(),
                    "longitude", site.getLongitude(),
                    "allowedRadiusMeters", site.getAllowedRadiusMeters(),
                    "locations", locationResponses,
                    "guardCount", guardCount,
                    "onDutyNow", onDutyNow
            ));

            logger.info("单位详情查询完成: id={}, locationCount={}, guardCount={}, onDutyNow={}",
                    id, locationResponses.size(), guardCount, onDutyNow);

            return ResponseEntity.ok(response);
        }).orElseGet(() -> {
            java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "单位不存在");
            return ResponseEntity.ok(errorResponse);
        });
    }
}
