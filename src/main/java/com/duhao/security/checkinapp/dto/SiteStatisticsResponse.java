package com.duhao.security.checkinapp.dto;

/**
 * 单位统计响应
 */
public class SiteStatisticsResponse {

    private boolean success;
    private String message;
    private SiteStatisticsData data;

    public SiteStatisticsResponse() {
    }

    public SiteStatisticsResponse(boolean success, String message, SiteStatisticsData data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static SiteStatisticsResponse success(SiteStatisticsData data) {
        return new SiteStatisticsResponse(true, null, data);
    }

    public static SiteStatisticsResponse error(String message) {
        return new SiteStatisticsResponse(false, message, null);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SiteStatisticsData getData() {
        return data;
    }

    public void setData(SiteStatisticsData data) {
        this.data = data;
    }

    /**
     * 单位统计数据
     */
    public static class SiteStatisticsData {
        private Long siteId;
        private String siteName;
        private int guardCount;
        private TodayStats todayStats;
        private WeeklyStats weeklyStats;

        public SiteStatisticsData() {
        }

        public SiteStatisticsData(Long siteId, String siteName, int guardCount,
                                   TodayStats todayStats, WeeklyStats weeklyStats) {
            this.siteId = siteId;
            this.siteName = siteName;
            this.guardCount = guardCount;
            this.todayStats = todayStats;
            this.weeklyStats = weeklyStats;
        }

        public Long getSiteId() {
            return siteId;
        }

        public void setSiteId(Long siteId) {
            this.siteId = siteId;
        }

        public String getSiteName() {
            return siteName;
        }

        public void setSiteName(String siteName) {
            this.siteName = siteName;
        }

        public int getGuardCount() {
            return guardCount;
        }

        public void setGuardCount(int guardCount) {
            this.guardCount = guardCount;
        }

        public TodayStats getTodayStats() {
            return todayStats;
        }

        public void setTodayStats(TodayStats todayStats) {
            this.todayStats = todayStats;
        }

        public WeeklyStats getWeeklyStats() {
            return weeklyStats;
        }

        public void setWeeklyStats(WeeklyStats weeklyStats) {
            this.weeklyStats = weeklyStats;
        }
    }

    /**
     * 今日统计
     */
    public static class TodayStats {
        private int checkinCount;
        private int uniqueGuards;
        private int checkinRate;
        private int onDutyNow;

        public TodayStats() {
        }

        public TodayStats(int checkinCount, int uniqueGuards, int checkinRate, int onDutyNow) {
            this.checkinCount = checkinCount;
            this.uniqueGuards = uniqueGuards;
            this.checkinRate = checkinRate;
            this.onDutyNow = onDutyNow;
        }

        public int getCheckinCount() {
            return checkinCount;
        }

        public void setCheckinCount(int checkinCount) {
            this.checkinCount = checkinCount;
        }

        public int getUniqueGuards() {
            return uniqueGuards;
        }

        public void setUniqueGuards(int uniqueGuards) {
            this.uniqueGuards = uniqueGuards;
        }

        public int getCheckinRate() {
            return checkinRate;
        }

        public void setCheckinRate(int checkinRate) {
            this.checkinRate = checkinRate;
        }

        public int getOnDutyNow() {
            return onDutyNow;
        }

        public void setOnDutyNow(int onDutyNow) {
            this.onDutyNow = onDutyNow;
        }
    }

    /**
     * 周统计
     */
    public static class WeeklyStats {
        private int totalCheckins;
        private double avgDailyCheckins;

        public WeeklyStats() {
        }

        public WeeklyStats(int totalCheckins, double avgDailyCheckins) {
            this.totalCheckins = totalCheckins;
            this.avgDailyCheckins = avgDailyCheckins;
        }

        public int getTotalCheckins() {
            return totalCheckins;
        }

        public void setTotalCheckins(int totalCheckins) {
            this.totalCheckins = totalCheckins;
        }

        public double getAvgDailyCheckins() {
            return avgDailyCheckins;
        }

        public void setAvgDailyCheckins(double avgDailyCheckins) {
            this.avgDailyCheckins = avgDailyCheckins;
        }
    }
}
