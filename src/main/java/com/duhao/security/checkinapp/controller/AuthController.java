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
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

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
        logger.info("收到微信登录请求 - loginCode: {}, phoneCode: {}", 
                   request.getLoginCode() != null ? request.getLoginCode().substring(0, Math.min(6, request.getLoginCode().length())) + "..." : "null",
                   request.getPhoneCode() != null ? request.getPhoneCode().substring(0, Math.min(6, request.getPhoneCode().length())) + "..." : "null");
        
        WechatLoginResponse result = wechatLoginService.wechatLogin(request);
        
        if (result.isSuccess()) {
            logger.info("微信登录成功 - openid: {}", 
                       result.getUserInfo() != null ? 
                       result.getUserInfo().getOpenid().substring(0, Math.min(8, result.getUserInfo().getOpenid().length())) + "..." : "unknown");
            return ResponseEntity.ok(result);
        } else {
            logger.warn("微信登录失败 - code: {}, message: {}", result.getErrorCode(), result.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    @PostMapping("/wechat-launch")
    public ResponseEntity<WechatLoginResponse> wechatLaunch(@RequestBody WechatLoginRequest request) {
        logger.info("收到微信启动请求 - loginCode: {}", 
                   request.getLoginCode() != null ? request.getLoginCode().substring(0, Math.min(6, request.getLoginCode().length())) + "..." : "null");
        
        WechatLoginResponse result = wechatLoginService.wechatLaunch(request);
        
        if (result.isSuccess()) {
            logger.info("微信启动成功 - openid: {}", 
                       result.getUserInfo() != null ? 
                       result.getUserInfo().getOpenid().substring(0, Math.min(8, result.getUserInfo().getOpenid().length())) + "..." : "unknown");
            return ResponseEntity.ok(result);
        } else {
            logger.warn("微信启动失败 - code: {}, message: {}", result.getErrorCode(), result.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    @PostMapping("/wechat-refresh-token")
    public ResponseEntity<WechatLoginResponse> wechatRefreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        logger.info("收到微信Token刷新请求 - Authorization: {}", 
                   authHeader != null ? "Bearer " + authHeader.substring(7, Math.min(13, authHeader.length())) + "..." : "null");
        
        WechatLoginResponse result = wechatLoginService.wechatTokenRefresh(request);
        
        if (result.isSuccess()) {
            logger.info("微信Token刷新成功 - openid: {}", 
                       result.getUserInfo() != null ? 
                       result.getUserInfo().getOpenid().substring(0, Math.min(8, result.getUserInfo().getOpenid().length())) + "..." : "unknown");
            return ResponseEntity.ok(result);
        } else {
            logger.warn("微信Token刷新失败 - code: {}, message: {}", result.getErrorCode(), result.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

}
