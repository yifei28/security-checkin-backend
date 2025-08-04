package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.entity.Admin;
import com.duhao.security.checkinapp.repository.AdminRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
    public List<Admin> getAllAdmin() {
        return adminRepository.findAll();
    }
}
