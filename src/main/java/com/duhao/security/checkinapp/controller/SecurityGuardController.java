package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.GuardResponse;
import com.duhao.security.checkinapp.entity.EmploymentStatus;
import com.duhao.security.checkinapp.entity.GuardRole;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.repository.WorkSiteRepository;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.duhao.security.checkinapp.dto.PaginationInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;

@RestController
@RequestMapping("/api/guards")
public class SecurityGuardController {
    private static final Logger logger = LoggerFactory.getLogger(SecurityGuardController.class);

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
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "单位信息不能为空"));
        }

        Long siteId = securityGuard.getSite().getId();
        Optional<WorkSite> workSite = workSiteRepository.findById(siteId);
        if (workSite.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "没有找到单位"));
        }
        securityGuard.setSite(workSite.get());

        // 如果没有指定角色，默认为队员
        if (securityGuard.getRole() == null) {
            securityGuard.setRole(GuardRole.TEAM_MEMBER);
        }

        try {
            SecurityGuard saved = guardRepository.save(securityGuard);
            return ResponseEntity.ok(Map.of("success", true, "data", saved));
        } catch (DataIntegrityViolationException e) {
            String message = "保存失败";
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("id_card_number")) {
                    message = "身份证号已存在";
                } else if (errorMsg.contains("phone_number")) {
                    message = "手机号已存在";
                } else if (errorMsg.contains("open_id")) {
                    message = "微信OpenID已存在";
                }
            }
            logger.warn("添加保安失败: {}", message);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", message));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSecurityGuard(@PathVariable Long id, @RequestBody SecurityGuard updated){
        Optional<SecurityGuard> existingOpt = guardRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SecurityGuard existing = existingOpt.get();
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

        // 更新证书级别（允许设置为 null 表示清除证书）
        existing.setFirefightingCertLevel(updated.getFirefightingCertLevel());
        existing.setSecurityGuardCertLevel(updated.getSecurityGuardCertLevel());
        existing.setSecurityCheckCertLevel(updated.getSecurityCheckCertLevel());

        if (updated.getSite() != null && updated.getSite().getId() != null) {
            Optional<WorkSite> siteOpt = workSiteRepository.findById(updated.getSite().getId());
            if (siteOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "单位不存在"));
            }
            existing.setSite(siteOpt.get());
        } else {
            existing.setSite(null); // 如果前端没传单位，清空单位
        }

        try {
            SecurityGuard saved = guardRepository.save(existing);
            return ResponseEntity.ok(Map.of("success", true, "data", saved));
        } catch (DataIntegrityViolationException e) {
            String message = "保存失败";
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("id_card_number")) {
                    message = "身份证号已存在";
                } else if (errorMsg.contains("phone_number")) {
                    message = "手机号已存在";
                } else if (errorMsg.contains("open_id")) {
                    message = "微信OpenID已存在";
                }
            }
            logger.warn("更新保安失败: {}", message);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", message));
        }
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
    public ResponseEntity<GuardResponse> getAllGuards(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            // 筛选参数
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String siteId,
            @RequestParam(required = false) String employmentStatus,
            @RequestParam(required = false) String role,
            // 身高范围筛选
            @RequestParam(required = false) Integer heightMin,
            @RequestParam(required = false) Integer heightMax,
            // 证书范围筛选参数
            @RequestParam(required = false) Integer firefightingCertMin,
            @RequestParam(required = false) Integer firefightingCertMax,
            @RequestParam(required = false) Integer securityGuardCertMin,
            @RequestParam(required = false) Integer securityGuardCertMax,
            @RequestParam(required = false) Integer securityCheckCertMin,
            @RequestParam(required = false) Integer securityCheckCertMax) {
        try {
            logger.info("=== 保安列表查询开始 ===");
            logger.info("分页参数: page={}, pageSize={}, sortBy={}, sortOrder={}", page, pageSize, sortBy, sortOrder);
            logger.info("筛选参数: name={}, siteId={}, employmentStatus={}, role={}", name, siteId, employmentStatus, role);
            logger.info("身高筛选: {}-{}cm", heightMin, heightMax);
            logger.info("证书筛选: 消防证={}-{}, 保安师证={}-{}, 安检证={}-{}",
                    firefightingCertMin, firefightingCertMax,
                    securityGuardCertMin, securityGuardCertMax,
                    securityCheckCertMin, securityCheckCertMax);

            // 创建排序对象
            Sort sort = Sort.by(sortBy);
            if ("desc".equalsIgnoreCase(sortOrder)) {
                sort = sort.descending();
            } else {
                sort = sort.ascending();
            }

            // 创建分页对象 (Spring 页码从0开始，前端从1开始)
            Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

            // 解析筛选参数
            Long siteIdLong = parseId(siteId, "site_");
            EmploymentStatus statusEnum = parseEmploymentStatus(employmentStatus);
            GuardRole roleEnum = parseRole(role);

            logger.info("解析后参数: siteIdLong={}, statusEnum={}, roleEnum={}", siteIdLong, statusEnum, roleEnum);

            // 使用筛选查询
            Page<SecurityGuard> guardsPage = guardRepository.findWithFilters(
                    name, siteIdLong, statusEnum, roleEnum,
                    heightMin, heightMax,
                    firefightingCertMin, firefightingCertMax,
                    securityGuardCertMin, securityGuardCertMax,
                    securityCheckCertMin, securityCheckCertMax,
                    pageable);

            List<GuardResponse.GuardData> data = guardsPage.getContent().stream()
                    .map(this::convertToGuardData)
                    .collect(Collectors.toList());

            PaginationInfo pagination = PaginationInfo.fromPage(guardsPage);

            logger.info("查询结果: total={}, 当前页数据量={}", pagination.getTotal(), data.size());
            return ResponseEntity.ok(GuardResponse.success(data, pagination));

        } catch (Exception e) {
            logger.error("保安列表查询失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GuardResponse(false, null));
        }
    }

    // 解析ID参数
    private Long parseId(String idStr, String prefix) {
        if (idStr == null || idStr.trim().isEmpty() || "all".equalsIgnoreCase(idStr)) {
            return null;
        }
        try {
            String cleanId = idStr.startsWith(prefix) ? idStr.substring(prefix.length()) : idStr;
            return Long.parseLong(cleanId);
        } catch (NumberFormatException e) {
            logger.warn("无法解析ID: {} (前缀: {})", idStr, prefix);
            return null;
        }
    }

    // 解析在职状态
    private EmploymentStatus parseEmploymentStatus(String status) {
        if (status == null || status.trim().isEmpty() || "all".equalsIgnoreCase(status)) {
            return null;
        }
        try {
            return EmploymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("无法解析在职状态: {}", status);
            return null;
        }
    }

    // 解析角色
    private GuardRole parseRole(String role) {
        if (role == null || role.trim().isEmpty() || "all".equalsIgnoreCase(role)) {
            return null;
        }
        try {
            return GuardRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("无法解析角色: {}", role);
            return null;
        }
    }
    
    // 转换方法
    private GuardResponse.GuardData convertToGuardData(SecurityGuard guard) {
        GuardResponse.GuardData.SiteInfo siteInfo = null;
        if (guard.getSite() != null) {
            siteInfo = new GuardResponse.GuardData.SiteInfo(
                    guard.getSite().getId(),
                    guard.getSite().getName()
            );
        }

        // 根据 employmentStatus 判断是否活跃
        boolean isActive = guard.getEmploymentStatus() == null ||
                guard.getEmploymentStatus() == EmploymentStatus.ACTIVE ||
                guard.getEmploymentStatus() == EmploymentStatus.PROBATION;

        return new GuardResponse.GuardData(
                guard.getId(),
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
                guard.getResignDate(),
                // 证书字段
                guard.getFirefightingCertLevel(),
                guard.getSecurityGuardCertLevel(),
                guard.getSecurityCheckCertLevel()
        );
    }
}
