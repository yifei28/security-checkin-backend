package com.duhao.security.checkinapp.util;

/**
 * 距离计算工具类
 */
public class DistanceCalculator {

    /**
     * 使用Haversine公式计算两点间距离
     * @param lat1 点1纬度
     * @param lng1 点1经度
     * @param lat2 点2纬度
     * @param lng2 点2经度
     * @return 距离（米）
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000; // 地球半径（米）
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2);
                
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * 检查位置是否在允许范围内
     * @param checkinLat 签到纬度
     * @param checkinLng 签到经度
     * @param siteLat 站点纬度
     * @param siteLng 站点经度
     * @param allowedRadius 允许半径（米）
     * @return 是否在范围内
     */
    public static boolean isWithinRange(double checkinLat, double checkinLng, 
                                       double siteLat, double siteLng, 
                                       double allowedRadius) {
        double distance = calculateDistance(checkinLat, checkinLng, siteLat, siteLng);
        return distance <= allowedRadius;
    }
}