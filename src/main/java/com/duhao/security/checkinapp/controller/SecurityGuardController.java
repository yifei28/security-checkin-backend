package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.repository.WorkSiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/guards")
public class SecurityGuardController {
    private final SecurityGuardRepository guardRepository;
    private final WorkSiteRepository workSiteRepository;

    @Autowired
    public SecurityGuardController(SecurityGuardRepository guardRepository, WorkSiteRepository workSiteRepository) {
        this.guardRepository = guardRepository;
        this.workSiteRepository = workSiteRepository;
    }

    @PostMapping
    public ResponseEntity<?> addSecurityGuard(@RequestBody SecurityGuard securityGuard){
        Long siteId = securityGuard.getSite().getId();
        Optional<WorkSite> workSite = workSiteRepository.findById(siteId);
        if (workSite.isEmpty()) {
            return ResponseEntity.badRequest().body("没有找到单位");
        }
        securityGuard.setSite(workSite.get());
        return ResponseEntity.ok(guardRepository.save(securityGuard));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSecurityGuard(@PathVariable Long id, @RequestBody SecurityGuard updated){
        return guardRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setEmployeeId(updated.getEmployeeId());
            existing.setPhoneNumber(updated.getPhoneNumber());
            if (updated.getSite() != null && updated.getSite().getId() != null) {
                Optional<WorkSite> siteOpt = workSiteRepository.findById(updated.getSite().getId());
                if (siteOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("单位不存在");
                }
                existing.setSite(siteOpt.get());
            } else {
                existing.setSite(null); // 如果前端没传单位，清空单位
            }
            return ResponseEntity.ok(guardRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuard(@PathVariable Long id) {
        guardRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<SecurityGuard> getAllGuards() {
        return guardRepository.findAll();
    }
}
