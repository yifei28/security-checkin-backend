package com.duhao.security.checkinapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Dashboard 统计数据响应
 */
public class DashboardResponse {

    private boolean success;
    private DashboardData data;

    public DashboardResponse(boolean success, DashboardData data) {
        this.success = success;
        this.data = data;
    }

    public static DashboardResponse success(DashboardData data) {
        return new DashboardResponse(true, data);
    }

    public static DashboardResponse fail() {
        return new DashboardResponse(false, null);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public DashboardData getData() { return data; }

    /**
     * Dashboard 数据
     */
    public static class DashboardData {
        private GuardStats guards;
        private SiteStats sites;
        private CheckinStats checkins;
        private List<LatestCheckin> latestCheckins;

        public DashboardData(GuardStats guards, SiteStats sites, CheckinStats checkins, List<LatestCheckin> latestCheckins) {
            this.guards = guards;
            this.sites = sites;
            this.checkins = checkins;
            this.latestCheckins = latestCheckins;
        }

        public GuardStats getGuards() { return guards; }
        public SiteStats getSites() { return sites; }
        public CheckinStats getCheckins() { return checkins; }
        public List<LatestCheckin> getLatestCheckins() { return latestCheckins; }
    }

    /**
     * 保安统计
     */
    public static class GuardStats {
        private int total;
        private int averageAge;
        private int teamLeaderCount;
        private int teamMemberCount;

        public GuardStats(int total, int averageAge, int teamLeaderCount, int teamMemberCount) {
            this.total = total;
            this.averageAge = averageAge;
            this.teamLeaderCount = teamLeaderCount;
            this.teamMemberCount = teamMemberCount;
        }

        public int getTotal() { return total; }
        public int getAverageAge() { return averageAge; }
        public int getTeamLeaderCount() { return teamLeaderCount; }
        public int getTeamMemberCount() { return teamMemberCount; }
    }

    /**
     * 单位统计
     */
    public static class SiteStats {
        private int total;

        public SiteStats(int total) {
            this.total = total;
        }

        public int getTotal() { return total; }
    }

    /**
     * 签到统计
     */
    public static class CheckinStats {
        private TodayStats today;
        private WeeklyStats weekly;
        private OverallStats overall;

        public CheckinStats(TodayStats today, WeeklyStats weekly, OverallStats overall) {
            this.today = today;
            this.weekly = weekly;
            this.overall = overall;
        }

        public TodayStats getToday() { return today; }
        public WeeklyStats getWeekly() { return weekly; }
        public OverallStats getOverall() { return overall; }
    }

    /**
     * 今日签到统计
     */
    public static class TodayStats {
        private int total;
        private int uniqueGuards;
        private int checkinRate;

        public TodayStats(int total, int uniqueGuards, int checkinRate) {
            this.total = total;
            this.uniqueGuards = uniqueGuards;
            this.checkinRate = checkinRate;
        }

        public int getTotal() { return total; }
        public int getUniqueGuards() { return uniqueGuards; }
        public int getCheckinRate() { return checkinRate; }
    }

    /**
     * 周统计
     */
    public static class WeeklyStats {
        private int total;
        private List<DailyCount> trend;

        public WeeklyStats(int total, List<DailyCount> trend) {
            this.total = total;
            this.trend = trend;
        }

        public int getTotal() { return total; }
        public List<DailyCount> getTrend() { return trend; }
    }

    /**
     * 每日统计
     */
    public static class DailyCount {
        private String date;
        private int count;

        public DailyCount(String date, int count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() { return date; }
        public int getCount() { return count; }
    }

    /**
     * 总体统计
     */
    public static class OverallStats {
        private long successCount;
        private long failedCount;
        private int successRate;

        public OverallStats(long successCount, long failedCount, int successRate) {
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.successRate = successRate;
        }

        public long getSuccessCount() { return successCount; }
        public long getFailedCount() { return failedCount; }
        public int getSuccessRate() { return successRate; }
    }

    /**
     * 最近签到记录
     */
    public static class LatestCheckin {
        private String id;
        private String guardName;
        private String siteName;
        private String timestamp;
        private String status;
        private String reason;

        public LatestCheckin(String id, String guardName, String siteName, String timestamp, String status, String reason) {
            this.id = id;
            this.guardName = guardName;
            this.siteName = siteName;
            this.timestamp = timestamp;
            this.status = status;
            this.reason = reason;
        }

        public String getId() { return id; }
        public String getGuardName() { return guardName; }
        public String getSiteName() { return siteName; }
        public String getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        public String getReason() { return reason; }
    }
}
