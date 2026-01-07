package com.duhao.security.checkinapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 微信订阅消息通知配置
 */
@Configuration
@ConfigurationProperties(prefix = "wx.notification")
public class WechatNotificationProperties {

    /**
     * 是否启用通知功能
     */
    private boolean enabled = true;

    /**
     * 抽查通知模板ID列表（支持多个模板轮换使用）
     * 格式：逗号分隔的模板ID列表
     * 例如：templateId1,templateId2,templateId3
     */
    private List<String> spotCheckTemplateIds = new ArrayList<>();

    /**
     * 兼容旧配置：单个抽查通知模板ID
     * @deprecated 请使用 spotCheckTemplateIds
     */
    private String spotCheckTemplateId;

    /**
     * 小程序跳转页面路径
     * 点击通知后跳转的页面
     */
    private String spotCheckPage = "pages/spot-check/index";

    /**
     * 小程序环境版本
     * developer: 开发版
     * trial: 体验版
     * formal: 正式版（默认）
     */
    private String miniprogramState = "formal";

    /**
     * 语言
     */
    private String lang = "zh_CN";

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getSpotCheckTemplateIds() {
        return spotCheckTemplateIds;
    }

    public void setSpotCheckTemplateIds(List<String> spotCheckTemplateIds) {
        this.spotCheckTemplateIds = spotCheckTemplateIds;
    }

    /**
     * 获取所有可用的模板ID列表
     * 优先使用 spotCheckTemplateIds，如果为空则回退到 spotCheckTemplateId
     */
    public List<String> getAvailableTemplateIds() {
        if (spotCheckTemplateIds != null && !spotCheckTemplateIds.isEmpty()) {
            return spotCheckTemplateIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .toList();
        }
        // 兼容旧配置
        if (spotCheckTemplateId != null && !spotCheckTemplateId.isBlank()) {
            return List.of(spotCheckTemplateId);
        }
        return List.of();
    }

    /**
     * @deprecated 请使用 getAvailableTemplateIds()
     */
    public String getSpotCheckTemplateId() {
        return spotCheckTemplateId;
    }

    /**
     * @deprecated 请使用 setSpotCheckTemplateIds()
     */
    public void setSpotCheckTemplateId(String spotCheckTemplateId) {
        this.spotCheckTemplateId = spotCheckTemplateId;
    }

    public String getSpotCheckPage() {
        return spotCheckPage;
    }

    public void setSpotCheckPage(String spotCheckPage) {
        this.spotCheckPage = spotCheckPage;
    }

    public String getMiniprogramState() {
        return miniprogramState;
    }

    public void setMiniprogramState(String miniprogramState) {
        this.miniprogramState = miniprogramState;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
