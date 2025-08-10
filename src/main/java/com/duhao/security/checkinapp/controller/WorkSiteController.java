package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.SiteResponse;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.repository.WorkSiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final WorkSiteRepository workSiteRepository;
    private final SecurityGuardRepository guardRepository;

    @Autowired
    public WorkSiteController(WorkSiteRepository workSiteRepository, 
                             SecurityGuardRepository guardRepository) {
        this.workSiteRepository = workSiteRepository;
        this.guardRepository = guardRepository;
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
        workSiteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<SiteResponse> getAllWorkSites() {
        try {
            List<WorkSite> sites = workSiteRepository.findAll();
            
            List<SiteResponse.SiteData> data = sites.stream()
                    .map(this::convertToSiteData)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(SiteResponse.success(data));
            
        } catch (Exception e) {
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
        
        return new SiteResponse.SiteData(
                "site_" + site.getId(),
                site.getName(),
                site.getLatitude(),
                site.getLongitude(),
                site.getAllowedRadiusMeters(),
                assignedGuardIds,
                true, // 假设所有站点都是活跃状态
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) // 创建时间暂时用当前时间
        );
    }
}
