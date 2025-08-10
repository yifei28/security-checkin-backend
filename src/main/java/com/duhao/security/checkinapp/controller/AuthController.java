package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.LoginRequest;
import com.duhao.security.checkinapp.dto.WechatLoginRequest;
import com.duhao.security.checkinapp.dto.WechatLoginResponse;
import com.duhao.security.checkinapp.entity.Admin;
import com.duhao.security.checkinapp.repository.AdminRepository;
import com.duhao.security.checkinapp.service.WechatLoginService;
import com.duhao.security.checkinapp.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WechatLoginService wechatLoginService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Admin> optionalAdmin = adminRepository.findByUsername(request.getUsername());
        if (optionalAdmin.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        }
        Admin admin = optionalAdmin.get();
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(optionalAdmin.get().getUsername());
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("superAdmin", admin.isSuperAdmin()); // 重点在这里

        return ResponseEntity.ok(response);
    }

    @PostMapping("/wechat-login")
    public ResponseEntity<WechatLoginResponse> wechatLogin(@RequestBody WechatLoginRequest request) {
        WechatLoginResponse result = wechatLoginService.wechatLogin(request);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    @PostMapping("/wechat-launch")
    public ResponseEntity<WechatLoginResponse> wechatLaunch(@RequestBody WechatLoginRequest request) {
        WechatLoginResponse result = wechatLoginService.wechatLaunch(request);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    @PostMapping("/wechat-refresh-token")
    public ResponseEntity<WechatLoginResponse> wechatRefreshToken(HttpServletRequest request) {
        WechatLoginResponse result = wechatLoginService.wechatTokenRefresh(request);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }
}
