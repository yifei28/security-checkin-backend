package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.entity.SpotCheckStatus;
import com.duhao.security.checkinapp.entity.WorkStatus;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SpotCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表控制器（管理端）
 * 提供周报/月报和统计分析
 */
@RestController
@RequestMapping(value = "/api/admin/report", produces = "application/json; charset=UTF-8")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final CheckinRepository checkinRepository;
    private final SpotCheckRepository spotCheckRepository;

    public ReportController(CheckinRepository checkinRepository,
                           SpotCheckRepository spotCheckRepository) {
        this.checkinRepository = checkinRepository;
        this.spotCheckRepository = spotCheckRepository;
    }

    /**
     * 获取周报
     * GET /api/admin/report/weekly
     */
    @GetMapping("/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) Long guardId,
            @RequestParam(required = false) Long siteId) {

        // 默认本周
        if (weekStart == null) {
            weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        LocalDateTime startTime = weekStart.atStartOfDay();
        LocalDateTime endTime = weekStart.plusWeeks(1).atStartOfDay();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", buildReport(startTime, endTime, guardId, siteId));
        response.put("period", Map.of(
                "type", "weekly",
                "start", weekStart,
                "end", weekStart.plusDays(6)
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 获取月报
     * GET /api/admin/report/monthly
     */
    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long guardId,
            @RequestParam(required = false) Long siteId) {

        // 默认本月
        LocalDate now = LocalDate.now();
        if (year == null) year = now.getYear();
        if (month == null) month = now.getMonthValue();

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDateTime startTime = monthStart.atStartOfDay();
        LocalDateTime endTime = monthStart.plusMonths(1).atStartOfDay();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", buildReport(startTime, endTime, guardId, siteId));
        response.put("period", Map.of(
                "type", "monthly",
                "year", year,
                "month", month,
                "start", monthStart,
                "end", monthStart.plusMonths(1).minusDays(1)
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 获取自定义时间范围报告
     * GET /api/admin/report/custom
     */
    @GetMapping("/custom")
    public ResponseEntity<Map<String, Object>> getCustomReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long guardId,
            @RequestParam(required = false) Long siteId) {

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", buildReport(startDate, endDate, guardId, siteId));
        response.put("period", Map.of(
                "type", "custom",
                "start", startDate,
                "end", endDate
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 获取每日趋势
     * GET /api/admin/report/daily-trend
     */
    @GetMapping("/daily-trend")
    public ResponseEntity<Map<String, Object>> getDailyTrend(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) Long guardId,
            @RequestParam(required = false) Long siteId) {

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            // 工作统计
            long workSessions = checkinRepository.countWithFilters(
                    dayStart, dayEnd, null, guardId, siteId);
            long completedSessions = checkinRepository.countByStatusWithFilters(
                    WorkStatus.COMPLETED, dayStart, dayEnd, guardId, siteId);

            // 抽查统计
            long spotCheckTotal = countSpotChecks(dayStart, dayEnd, null, guardId, siteId);
            long spotCheckPassed = countSpotChecks(dayStart, dayEnd, SpotCheckStatus.PASSED, guardId, siteId);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date);
            dayData.put("workSessions", workSessions);
            dayData.put("completedSessions", completedSessions);
            dayData.put("spotCheckTotal", spotCheckTotal);
            dayData.put("spotCheckPassed", spotCheckPassed);
            dayData.put("spotCheckPassRate", spotCheckTotal > 0
                    ? (int) (spotCheckPassed * 100 / spotCheckTotal) : 0);

            trend.add(dayData);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", trend);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取概览统计
     * GET /api/admin/report/overview
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime weekStartTime = weekStart.atStartOfDay();

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartTime = monthStart.atStartOfDay();

        // 当前在岗人数
        long activeGuards = checkinRepository.findByStatus(WorkStatus.ACTIVE).size();

        // 今日统计
        long todaySessions = checkinRepository.countWithFilters(todayStart, todayEnd, null, null, null);
        long todaySpotChecks = countSpotChecks(todayStart, todayEnd, null, null, null);
        long todaySpotCheckPassed = countSpotChecks(todayStart, todayEnd, SpotCheckStatus.PASSED, null, null);

        // 本周统计
        long weekSessions = checkinRepository.countWithFilters(weekStartTime, todayEnd, null, null, null);
        long weekSpotChecks = countSpotChecks(weekStartTime, todayEnd, null, null, null);
        long weekSpotCheckPassed = countSpotChecks(weekStartTime, todayEnd, SpotCheckStatus.PASSED, null, null);

        // 本月统计
        long monthSessions = checkinRepository.countWithFilters(monthStartTime, todayEnd, null, null, null);
        long monthSpotChecks = countSpotChecks(monthStartTime, todayEnd, null, null, null);
        long monthSpotCheckPassed = countSpotChecks(monthStartTime, todayEnd, SpotCheckStatus.PASSED, null, null);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
                "activeGuards", activeGuards,
                "today", Map.of(
                        "sessions", todaySessions,
                        "spotChecks", todaySpotChecks,
                        "spotCheckPassed", todaySpotCheckPassed,
                        "passRate", todaySpotChecks > 0 ? (int) (todaySpotCheckPassed * 100 / todaySpotChecks) : 0
                ),
                "thisWeek", Map.of(
                        "sessions", weekSessions,
                        "spotChecks", weekSpotChecks,
                        "spotCheckPassed", weekSpotCheckPassed,
                        "passRate", weekSpotChecks > 0 ? (int) (weekSpotCheckPassed * 100 / weekSpotChecks) : 0
                ),
                "thisMonth", Map.of(
                        "sessions", monthSessions,
                        "spotChecks", monthSpotChecks,
                        "spotCheckPassed", monthSpotCheckPassed,
                        "passRate", monthSpotChecks > 0 ? (int) (monthSpotCheckPassed * 100 / monthSpotChecks) : 0
                )
        ));

        return ResponseEntity.ok(response);
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> buildReport(LocalDateTime startTime, LocalDateTime endTime,
                                            Long guardId, Long siteId) {
        // 工作统计
        long totalSessions = checkinRepository.countWithFilters(startTime, endTime, null, guardId, siteId);
        long completedSessions = checkinRepository.countByStatusWithFilters(
                WorkStatus.COMPLETED, startTime, endTime, guardId, siteId);
        long timeoutSessions = checkinRepository.countByStatusWithFilters(
                WorkStatus.TIMEOUT, startTime, endTime, guardId, siteId);
        long activeSessions = checkinRepository.countByStatusWithFilters(
                WorkStatus.ACTIVE, startTime, endTime, guardId, siteId);

        // 抽查统计
        long spotCheckTotal = countSpotChecks(startTime, endTime, null, guardId, siteId);
        long spotCheckPassed = countSpotChecks(startTime, endTime, SpotCheckStatus.PASSED, guardId, siteId);
        long spotCheckMissed = countSpotChecks(startTime, endTime, SpotCheckStatus.MISSED, guardId, siteId);
        long spotCheckPending = countSpotChecks(startTime, endTime, SpotCheckStatus.PENDING, guardId, siteId);

        // 计算通过率
        long processedSpotChecks = spotCheckPassed + spotCheckMissed;
        int spotCheckPassRate = processedSpotChecks > 0
                ? (int) (spotCheckPassed * 100 / processedSpotChecks) : 0;

        return Map.of(
                "work", Map.of(
                        "totalSessions", totalSessions,
                        "completedSessions", completedSessions,
                        "timeoutSessions", timeoutSessions,
                        "activeSessions", activeSessions,
                        "completionRate", totalSessions > 0
                                ? (int) (completedSessions * 100 / totalSessions) : 0
                ),
                "spotCheck", Map.of(
                        "total", spotCheckTotal,
                        "passed", spotCheckPassed,
                        "missed", spotCheckMissed,
                        "pending", spotCheckPending,
                        "passRate", spotCheckPassRate
                )
        );
    }

    private long countSpotChecks(LocalDateTime startTime, LocalDateTime endTime,
                                 SpotCheckStatus status, Long guardId, Long siteId) {
        if (status != null) {
            return spotCheckRepository.countByStatusWithFilters(startTime, endTime, status, guardId, siteId);
        }

        // 总数 = 所有状态之和
        long passed = spotCheckRepository.countByStatusWithFilters(
                startTime, endTime, SpotCheckStatus.PASSED, guardId, siteId);
        long missed = spotCheckRepository.countByStatusWithFilters(
                startTime, endTime, SpotCheckStatus.MISSED, guardId, siteId);
        long pending = spotCheckRepository.countByStatusWithFilters(
                startTime, endTime, SpotCheckStatus.PENDING, guardId, siteId);
        return passed + missed + pending;
    }
}
