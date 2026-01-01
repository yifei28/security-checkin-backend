package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.AdminResponse;
import com.duhao.security.checkinapp.dto.PaginationInfo;
import com.duhao.security.checkinapp.entity.Admin;
import com.duhao.security.checkinapp.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminController(AdminRepository adminRepository,  PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public ResponseEntity<?> addAdmin(@RequestBody Admin newAdmin) {
        // 检查用户名是否已存在
        Optional<Admin> admin = adminRepository.findByUsername(newAdmin.getUsername());
        if (admin.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("用户名已存在");
        }

        // 加密密码
        newAdmin.setPassword(passwordEncoder.encode(newAdmin.getPassword()));
        adminRepository.save(newAdmin);
        return ResponseEntity.ok("管理员添加成功");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long id) {
        if (!adminRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("管理员不存在");
        }
        adminRepository.deleteById(id);
        return ResponseEntity.ok("管理员已删除");
    }

    @GetMapping
    public ResponseEntity<AdminResponse> getAllAdmin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        try {
            // 创建排序对象
            Sort sort = Sort.by(sortBy);
            if ("desc".equalsIgnoreCase(sortOrder)) {
                sort = sort.descending();
            } else {
                sort = sort.ascending();
            }

            // 创建分页对象 (Spring 页码从0开始，前端从1开始)
            Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

            Page<Admin> adminPage = adminRepository.findAll(pageable);

            List<AdminResponse.AdminData> data = adminPage.getContent().stream()
                    .map(this::convertToAdminData)
                    .collect(Collectors.toList());

            PaginationInfo pagination = PaginationInfo.fromPage(adminPage);

            return ResponseEntity.ok(AdminResponse.success(data, pagination));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AdminResponse(false, null));
        }
    }

    // 转换方法
    private AdminResponse.AdminData convertToAdminData(Admin admin) {
        return new AdminResponse.AdminData(
                "admin_" + admin.getId(),
                admin.getUsername(),
                admin.isSuperAdmin()
        );
    }
}
