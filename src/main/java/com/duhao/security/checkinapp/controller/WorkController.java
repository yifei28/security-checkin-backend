package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.WorkEndRequest;
import com.duhao.security.checkinapp.dto.WorkHistoryResponse;
import com.duhao.security.checkinapp.dto.WorkResponse;
import com.duhao.security.checkinapp.dto.WorkStartRequest;
import com.duhao.security.checkinapp.dto.WorkStatusResponse;
import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.service.WorkService;
import com.duhao.security.checkinapp.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作控制器（保安端）
 * 处理上岗/下岗/状态查询
 */
@RestController
@RequestMapping(value = "/api/work", produces = "application/json; charset=UTF-8")
public class WorkController {

    private static final Logger logger = LoggerFactory.getLogger(WorkController.class);

    private final WorkService workService;
    private final SecurityGuardRepository guardRepository;
    private final CheckinRepository checkinRepository;
    private final JwtUtil jwtUtil;

    public WorkController(WorkService workService,
                          SecurityGuardRepository guardRepository,
                          CheckinRepository checkinRepository,
                          JwtUtil jwtUtil) {
        this.workService = workService;
        this.guardRepository = guardRepository;
        this.checkinRepository = checkinRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 上岗
     * POST /api/work/start
     */
    @PostMapping("/start")
    public ResponseEntity<WorkResponse> startWork(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody WorkStartRequest request) {

        Long guardId = extractGuardId(authHeader);
        if (guardId == null) {
            return ResponseEntity.ok(WorkResponse.error("无效的认证信息"));
        }

        logger.info("收到上岗请求: guardId={}", guardId);
        WorkResponse response = workService.startWork(guardId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 下岗
     * POST /api/work/end
     */
    @PostMapping("/end")
    public ResponseEntity<WorkResponse> endWork(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody WorkEndRequest request) {

        Long guardId = extractGuardId(authHeader);
        if (guardId == null) {
            return ResponseEntity.ok(WorkResponse.error("无效的认证信息"));
        }

        logger.info("收到下岗请求: guardId={}", guardId);
        WorkResponse response = workService.endWork(guardId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前工作状态
     * GET /api/work/status
     */
    @GetMapping("/status")
    public ResponseEntity<WorkStatusResponse> getWorkStatus(
            @RequestHeader("Authorization") String authHeader) {

        Long guardId = extractGuardId(authHeader);
        if (guardId == null) {
            return ResponseEntity.ok(WorkStatusResponse.error("无效的认证信息"));
        }

        WorkStatusResponse response = workService.getWorkStatus(guardId);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取我的工作历史
     * GET /api/work/my-history
     */
    @GetMapping("/my-history")
    public ResponseEntity<WorkHistoryResponse> getMyHistory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        Long guardId = extractGuardId(authHeader);
        if (guardId == null) {
            return ResponseEntity.ok(WorkHistoryResponse.error("无效的认证信息"));
        }

        SecurityGuard guard = guardRepository.findById(guardId).orElse(null);
        if (guard == null) {
            return ResponseEntity.ok(WorkHistoryResponse.error("保安不存在"));
        }

        // 分页查询，按开始时间降序
        PageRequest pageRequest = PageRequest.of(
                page - 1,  // Spring Data 分页从0开始
                pageSize,
                Sort.by(Sort.Direction.DESC, "startTime")
        );

        Page<CheckinRecord> recordPage = checkinRepository.findByGuard(guard, pageRequest);

        List<WorkHistoryResponse.WorkHistoryItem> items = recordPage.getContent().stream()
                .map(WorkHistoryResponse.WorkHistoryItem::fromEntity)
                .toList();

        return ResponseEntity.ok(WorkHistoryResponse.success(
                items,
                recordPage.getTotalElements(),
                page,
                pageSize
        ));
    }

    /**
     * 从JWT中提取保安ID
     */
    private Long extractGuardId(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }

            String token = authHeader.substring(7);
            String subject = jwtUtil.extractUsername(token);

            // 微信token格式: "openid:xxx"
            if (subject == null || !subject.startsWith("openid:")) {
                return null;
            }

            String openId = subject.substring(7); // 去掉 "openid:" 前缀
            SecurityGuard guard = guardRepository.findByOpenId(openId);
            return guard != null ? guard.getId() : null;

        } catch (Exception e) {
            logger.error("提取保安ID失败", e);
            return null;
        }
    }
}
