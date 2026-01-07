package com.duhao.security.checkinapp.service;

import com.duhao.security.checkinapp.entity.SpotCheck;

/**
 * 微信订阅消息通知服务
 * 用于发送抽查通知给保安
 */
public interface WechatNotificationService {

    /**
     * 发送抽查通知（异步）
     * 当系统触发抽查时，向保安发送订阅消息
     *
     * @param spotCheck 抽查记录
     */
    void sendSpotCheckNotification(SpotCheck spotCheck);
}
