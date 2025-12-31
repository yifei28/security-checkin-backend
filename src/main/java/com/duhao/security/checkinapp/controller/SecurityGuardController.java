package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.GuardResponse;
import com.duhao.security.checkinapp.entity.EmploymentStatus;
import com.duhao.security.checkinapp.entity.GuardRole;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.repository.WorkSiteRepository;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/guards")
public class SecurityGuardController {
    private final SecurityGuardRepository guardRepository;
    private final WorkSiteRepository workSiteRepository;
    private final CheckinRepository checkinRepository;

    @Autowired
    public SecurityGuardController(SecurityGuardRepository guardRepository, WorkSiteRepository workSiteRepository, CheckinRepository checkinRepository) {
        this.guardRepository = guardRepository;
        this.workSiteRepository = workSiteRepository;
        this.checkinRepository = checkinRepository;
    }

    @PostMapping
    public ResponseEntity<?> addSecurityGuard(@RequestBody SecurityGuard securityGuard){
        if (securityGuard.getSite() == null || securityGuard.getSite().getId() == null) {
            return ResponseEntity.badRequest().body("单位信息不能为空");
        }
        
        Long siteId = securityGuard.getSite().getId();
        Optional<WorkSite> workSite = workSiteRepository.findById(siteId);
        if (workSite.isEmpty()) {
            return ResponseEntity.badRequest().body("没有找到单位");
        }
        securityGuard.setSite(workSite.get());
        
        // 如果没有指定角色，默认为队员
        if (securityGuard.getRole() == null) {
            securityGuard.setRole(GuardRole.TEAM_MEMBER);
        }
        
        return ResponseEntity.ok(guardRepository.save(securityGuard));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSecurityGuard(@PathVariable Long id, @RequestBody SecurityGuard updated){
        return guardRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setPhoneNumber(updated.getPhoneNumber());

            // 更新生日
            if (updated.getBirthDate() != null) {
                existing.setBirthDate(updated.getBirthDate());
            }

            // 更新身高
            if (updated.getHeight() != null) {
                existing.setHeight(updated.getHeight());
            }

            // 更新角色
            if (updated.getRole() != null) {
                existing.setRole(updated.getRole());
            }

            // 更新身份证号
            if (updated.getIdCardNumber() != null) {
                existing.setIdCardNumber(updated.getIdCardNumber());
            }

            // 更新性别
            if (updated.getGender() != null) {
                existing.setGender(updated.getGender());
            }

            // 更新在职状态
            if (updated.getEmploymentStatus() != null) {
                existing.setEmploymentStatus(updated.getEmploymentStatus());
            }

            // 更新入职日期
            if (updated.getOriginalHireDate() != null) {
                existing.setOriginalHireDate(updated.getOriginalHireDate());
            }
            if (updated.getLatestHireDate() != null) {
                existing.setLatestHireDate(updated.getLatestHireDate());
            }

            // 更新离职日期
            if (updated.getResignDate() != null) {
                existing.setResignDate(updated.getResignDate());
            }

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
        Optional<SecurityGuard> guardOpt = guardRepository.findById(id);
        if (guardOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        SecurityGuard guard = guardOpt.get();
        checkinRepository.deleteAll(checkinRepository.findByGuard(guard));
        guardRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<GuardResponse> getAllGuards() {
        try {
            List<SecurityGuard> guards = guardRepository.findAll();
            
            List<GuardResponse.GuardData> data = guards.stream()
                    .map(this::convertToGuardData)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(GuardResponse.success(data));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GuardResponse(false, null));
        }
    }
    
    // 转换方法
    private GuardResponse.GuardData convertToGuardData(SecurityGuard guard) {
        GuardResponse.GuardData.SiteInfo siteInfo = null;
        if (guard.getSite() != null) {
            siteInfo = new GuardResponse.GuardData.SiteInfo(
                    "site_" + guard.getSite().getId(),
                    guard.getSite().getName()
            );
        }

        // 根据 employmentStatus 判断是否活跃
        boolean isActive = guard.getEmploymentStatus() == null ||
                guard.getEmploymentStatus() == EmploymentStatus.ACTIVE ||
                guard.getEmploymentStatus() == EmploymentStatus.PROBATION;

        return new GuardResponse.GuardData(
                "guard_" + guard.getId(),
                guard.getName(),
                guard.getPhoneNumber(),
                guard.getEmployeeId(),
                siteInfo,
                guard.getRole() != null ? guard.getRole() : GuardRole.TEAM_MEMBER,
                isActive,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                // 新增字段
                guard.getBirthDate(),
                guard.getAge(),
                guard.getHeight(),
                guard.getIdCardNumber(),
                guard.getGender(),
                guard.getEmploymentStatus(),
                guard.getOriginalHireDate(),
                guard.getLatestHireDate(),
                guard.getResignDate()
        );
    }
}
