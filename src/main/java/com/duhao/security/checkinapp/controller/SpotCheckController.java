package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.SpotCheckCompleteRequest;
import com.duhao.security.checkinapp.dto.SpotCheckResponse;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.SpotCheck;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.service.SpotCheckService;
import com.duhao.security.checkinapp.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 抽查控制器（保安端）
 * 处理抽查查询和完成
 */
@RestController
@RequestMapping(value = "/api/spot-check", produces = "application/json; charset=UTF-8")
public class SpotCheckController {

    private static final Logger logger = LoggerFactory.getLogger(SpotCheckController.class);

    private final SpotCheckService spotCheckService;
    private final SecurityGuardRepository guardRepository;
    private final JwtUtil jwtUtil;

    public SpotCheckController(SpotCheckService spotCheckService,
                               SecurityGuardRepository guardRepository,
                               JwtUtil jwtUtil) {
        this.spotCheckService = spotCheckService;
        this.guardRepository = guardRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 查询待处理抽查
     * GET /api/spot-check/pending
     * 小程序轮询用
     */
    @GetMapping("/pending")
    public ResponseEntity<SpotCheckResponse> getPendingSpotCheck(
            @RequestHeader("Authorization") String authHeader) {

        Long guardId = extractGuardId(authHeader);
        if (guardId == null) {
            return ResponseEntity.ok(SpotCheckResponse.error("无效的认证信息"));
        }

        Optional<SpotCheck> pendingOpt = spotCheckService.getActiveSpotCheckForGuard(guardId);

        if (pendingOpt.isEmpty()) {
            // 没有待处理抽查，返回空数据
            return ResponseEntity.ok(SpotCheckResponse.success("当前无待处理抽查", null));
        }

        SpotCheck spotCheck = pendingOpt.get();
        SpotCheckResponse.SpotCheckData data = buildSpotCheckData(spotCheck);

        return ResponseEntity.ok(SpotCheckResponse.success("有待处理抽查", data));
    }

    /**
     * 完成抽查验证
     * POST /api/spot-check/complete
     */
    @PostMapping("/complete")
    public ResponseEntity<SpotCheckResponse> completeSpotCheck(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SpotCheckCompleteRequest request) {

        Long guardId = extractGuardId(authHeader);
        if (guardId == null) {
            return ResponseEntity.ok(SpotCheckResponse.error("无效的认证信息"));
        }

        logger.info("收到抽查完成请求: guardId={}, spotCheckId={}", guardId, request.getSpotCheckId());

        // 验证抽查是否属于该保安
        Optional<SpotCheck> pendingOpt = spotCheckService.getActiveSpotCheckForGuard(guardId);
        if (pendingOpt.isEmpty() || !pendingOpt.get().getId().equals(request.getSpotCheckId())) {
            return ResponseEntity.ok(SpotCheckResponse.error("抽查记录不存在或不属于当前用户"));
        }

        SpotCheckService.SpotCheckResult result = spotCheckService.completeSpotCheck(
                request.getSpotCheckId(),
                request.getLatitude(),
                request.getLongitude(),
                request.getFaceImageUrl()
        );

        if (!result.success()) {
            return ResponseEntity.ok(SpotCheckResponse.error(result.message()));
        }

        SpotCheckResponse.SpotCheckData data = buildSpotCheckData(result.spotCheck());
        return ResponseEntity.ok(SpotCheckResponse.success(result.message(), data));
    }

    /**
     * 查询我的抽查历史
     * GET /api/spot-check/my-history
     */
    @GetMapping("/my-history")
    public ResponseEntity<Map<String, Object>> getMySpotCheckHistory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        Long guardId = extractGuardId(authHeader);
        if (guardId == null) {
            return ResponseEntity.ok(errorResponse("无效的认证信息"));
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SpotCheck> spotChecks = spotCheckService.getSpotCheckHistory(guardId, pageable);

        List<SpotCheckResponse.SpotCheckData> records = spotChecks.getContent().stream()
                .map(this::buildSpotCheckData)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", records);
        response.put("pagination", Map.of(
                "total", spotChecks.getTotalElements(),
                "page", page,
                "pageSize", pageSize,
                "totalPages", spotChecks.getTotalPages()
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 查询今日抽查统计
     * GET /api/spot-check/today-stats
     */
    @GetMapping("/today-stats")
    public ResponseEntity<Map<String, Object>> getTodayStats(
            @RequestHeader("Authorization") String authHeader) {

        Long guardId = extractGuardId(authHeader);
        if (guardId == null) {
            return ResponseEntity.ok(errorResponse("无效的认证信息"));
        }

        long todayCount = spotCheckService.countTodaySpotChecks(guardId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
                "todaySpotChecks", todayCount
        ));

        return ResponseEntity.ok(response);
    }

    // ==================== 辅助方法 ====================

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

    /**
     * 构建抽查数据响应
     */
    private SpotCheckResponse.SpotCheckData buildSpotCheckData(SpotCheck spotCheck) {
        SpotCheckResponse.SpotCheckData data = new SpotCheckResponse.SpotCheckData();
        data.setId(spotCheck.getId());
        data.setCreatedAt(spotCheck.getCreatedAt());
        data.setDeadline(spotCheck.getDeadline());
        data.setCompletedAt(spotCheck.getCompletedAt());
        data.setStatus(spotCheck.getStatus().getDisplayName());
        data.setRemainingSeconds(spotCheck.getRemainingSeconds());
        return data;
    }

    /**
     * 构建错误响应
     */
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
