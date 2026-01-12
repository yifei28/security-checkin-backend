package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.DashboardResponse;
import com.duhao.security.checkinapp.dto.DashboardResponse.*;
import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.WorkStatus;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.repository.WorkSiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/statistics", produces = "application/json; charset=UTF-8")
public class StatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    private final SecurityGuardRepository guardRepository;
    private final WorkSiteRepository siteRepository;
    private final CheckinRepository checkinRepository;

    @Autowired
    public StatisticsController(SecurityGuardRepository guardRepository,
                                WorkSiteRepository siteRepository,
                                CheckinRepository checkinRepository) {
        this.guardRepository = guardRepository;
        this.siteRepository = siteRepository;
        this.checkinRepository = checkinRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        try {
            logger.info("=== Dashboard 统计查询开始 ===");

            // 1. 保安统计
            GuardStats guardStats = getGuardStats();
            logger.info("保安统计: total={}, avgAge={}, leaders={}, members={}",
                    guardStats.getTotal(), guardStats.getAverageAge(),
                    guardStats.getTeamLeaderCount(), guardStats.getTeamMemberCount());

            // 2. 单位统计
            SiteStats siteStats = getSiteStats();
            logger.info("单位统计: total={}", siteStats.getTotal());

            // 3. 签到统计
            CheckinStats checkinStats = getCheckinStats(guardStats.getTotal());
            logger.info("签到统计: today={}, weekly={}, onDuty={}",
                    checkinStats.getToday().getTotal(),
                    checkinStats.getWeekly().getTotal(),
                    checkinStats.getToday().getOnDutyCount());

            // 4. 最近签到记录
            List<LatestCheckin> latestCheckins = getLatestCheckins();
            logger.info("最近签到记录数: {}", latestCheckins.size());

            // 组装响应
            DashboardData data = new DashboardData(guardStats, siteStats, checkinStats, latestCheckins);

            logger.info("=== Dashboard 统计查询完成 ===");
            return ResponseEntity.ok(DashboardResponse.success(data));

        } catch (Exception e) {
            logger.error("Dashboard 统计查询失败", e);
            return ResponseEntity.internalServerError().body(DashboardResponse.fail());
        }
    }

    private GuardStats getGuardStats() {
        long total = guardRepository.count();
        long teamLeaderCount = guardRepository.countTeamLeaders();
        long teamMemberCount = guardRepository.countTeamMembers();

        Double avgAge = guardRepository.calculateAverageAge();
        int averageAge = avgAge != null ? (int) Math.round(avgAge) : 0;

        return new GuardStats((int) total, averageAge, (int) teamLeaderCount, (int) teamMemberCount);
    }

    private SiteStats getSiteStats() {
        long total = siteRepository.count();
        return new SiteStats((int) total);
    }

    private CheckinStats getCheckinStats(int totalGuards) {
        // 今日统计
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        long todayTotal = checkinRepository.countByTimestampBetween(todayStart, todayEnd);
        long todayUniqueGuards = checkinRepository.countDistinctGuardsByTimestampBetween(todayStart, todayEnd);
        int checkinRate = totalGuards > 0 ? (int) Math.round((double) todayUniqueGuards / totalGuards * 100) : 0;

        // 当前在岗人数
        int onDutyCount = checkinRepository.findByStatus(WorkStatus.ACTIVE).size();

        TodayStats todayStats = new TodayStats((int) todayTotal, (int) todayUniqueGuards, checkinRate, onDutyCount);

        // 周统计（过去7天）
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay();
        WeeklyStats weeklyStats = getWeeklyStats(weekStart, todayEnd);

        return new CheckinStats(todayStats, weeklyStats);
    }

    private WeeklyStats getWeeklyStats(LocalDateTime start, LocalDateTime end) {
        List<Object[]> dailyCounts = checkinRepository.countByDateBetween(start, end);

        // 创建日期到数量的映射
        Map<LocalDate, Long> countMap = dailyCounts.stream()
                .collect(Collectors.toMap(
                        row -> {
                            Object dateObj = row[0];
                            if (dateObj instanceof java.sql.Date) {
                                return ((java.sql.Date) dateObj).toLocalDate();
                            } else if (dateObj instanceof LocalDate) {
                                return (LocalDate) dateObj;
                            }
                            return LocalDate.parse(dateObj.toString());
                        },
                        row -> ((Number) row[1]).longValue()
                ));

        // 生成完整的7天趋势（补充没有数据的日期）
        List<DailyCount> trend = new ArrayList<>();
        long weeklyTotal = 0;
        LocalDate date = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!date.isAfter(endDate.minusDays(1))) {
            long count = countMap.getOrDefault(date, 0L);
            trend.add(new DailyCount(date.format(DateTimeFormatter.ISO_LOCAL_DATE), (int) count));
            weeklyTotal += count;
            date = date.plusDays(1);
        }

        return new WeeklyStats((int) weeklyTotal, trend);
    }

    private List<LatestCheckin> getLatestCheckins() {
        List<CheckinRecord> records = checkinRepository.findLatestCheckins(PageRequest.of(0, 5));

        return records.stream()
                .map(record -> new LatestCheckin(
                        record.getId(),
                        record.getGuard() != null ? record.getGuard().getName() : "未知",
                        record.getSite() != null ? record.getSite().getName() : "未知",
                        record.getStartTime() != null ?
                                record.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                        record.getStatus() != null ? record.getStatus().getDisplayName() : "待处理",
                        record.getReason()
                ))
                .collect(Collectors.toList());
    }
}
