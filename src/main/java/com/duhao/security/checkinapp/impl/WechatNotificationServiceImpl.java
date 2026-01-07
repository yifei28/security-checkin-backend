package com.duhao.security.checkinapp.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import com.duhao.security.checkinapp.config.WechatNotificationProperties;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.SpotCheck;
import com.duhao.security.checkinapp.service.WechatNotificationService;
import me.chanjar.weixin.common.error.WxErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 微信订阅消息通知服务实现
 * 使用 WxJava SDK 发送小程序订阅消息
 * 支持多模板轮换发送（解决单模板订阅次数限制）
 */
@Service
public class WechatNotificationServiceImpl implements WechatNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WechatNotificationServiceImpl.class);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WxMaService wxMaService;
    private final WechatNotificationProperties properties;

    /**
     * 模板轮换计数器（线程安全）
     * 用于在多个模板之间循环选择
     */
    private final AtomicInteger templateCounter = new AtomicInteger(0);

    public WechatNotificationServiceImpl(WxMaService wxMaService,
                                          WechatNotificationProperties properties) {
        this.wxMaService = wxMaService;
        this.properties = properties;
    }

    /**
     * 获取下一个要使用的模板索引和ID（轮换策略）
     * @return [模板索引, 模板ID]，如果没有配置返回 null
     */
    private Object[] getNextTemplate() {
        List<String> templateIds = properties.getAvailableTemplateIds();
        if (templateIds.isEmpty()) {
            return null;
        }
        // 轮换选择模板
        int index = templateCounter.getAndIncrement() % templateIds.size();
        // 防止计数器溢出
        if (templateCounter.get() > 1000000) {
            templateCounter.set(0);
        }
        return new Object[]{index, templateIds.get(index)};
    }

    @Override
    @Async
    public void sendSpotCheckNotification(SpotCheck spotCheck) {
        if (!properties.isEnabled()) {
            logger.debug("微信通知功能已禁用");
            return;
        }

        Object[] template = getNextTemplate();
        if (template == null) {
            logger.warn("抽查通知模板ID未配置，跳过发送");
            return;
        }
        int templateIndex = (int) template[0];
        String templateId = (String) template[1];

        SecurityGuard guard = spotCheck.getGuard();
        if (guard == null || guard.getOpenId() == null || guard.getOpenId().isBlank()) {
            logger.warn("无法发送通知：保安或OpenID不存在，spotCheckId={}", spotCheck.getId());
            return;
        }

        try {
            // 构建订阅消息
            WxMaSubscribeMessage message = WxMaSubscribeMessage.builder()
                    .toUser(guard.getOpenId())
                    .templateId(templateId)
                    .page(properties.getSpotCheckPage() + "?spotCheckId=" + spotCheck.getId())
                    .miniprogramState(properties.getMiniprogramState())
                    .lang(properties.getLang())
                    .data(buildSpotCheckData(spotCheck, templateIndex))
                    .build();

            // 发送订阅消息
            wxMaService.getMsgService().sendSubscribeMsg(message);

            logger.info("抽查通知发送成功: spotCheckId={}, guardName={}, templateId={}, openId={}",
                    spotCheck.getId(),
                    guard.getName(),
                    templateId.substring(0, Math.min(10, templateId.length())) + "...",
                    guard.getOpenId().substring(0, Math.min(8, guard.getOpenId().length())) + "...");

        } catch (WxErrorException e) {
            // 常见错误码：
            // 43101 - 用户拒绝接收消息
            // 47003 - 模板ID不存在
            // 41030 - page路径不正确
            int errorCode = e.getError() != null ? e.getError().getErrorCode() : -1;
            String errorMsg = e.getError() != null ? e.getError().getErrorMsg() : e.getMessage();

            if (errorCode == 43101) {
                logger.info("用户未订阅消息，跳过通知: spotCheckId={}, guardName={}",
                        spotCheck.getId(), guard.getName());
            } else {
                logger.error("发送抽查通知失败: spotCheckId={}, errorCode={}, errorMsg={}",
                        spotCheck.getId(), errorCode, errorMsg);
            }

        } catch (Exception e) {
            logger.error("发送抽查通知异常: spotCheckId={}", spotCheck.getId(), e);
        }
    }

    /**
     * 构建抽查通知数据（根据模板索引选择字段）
     *
     * 模板1 (index 0): phrase1, date2, thing6 (打卡类型/打卡时间/打卡内容)
     * 模板2 (index 1): thing1, thing2, time5, thing16 (活动名称/签到方式/时间/温馨提示)
     * 模板3 (index 2): time1, thing2, thing5, thing3 (检查日期/检查人员/检查情况/检查类型)
     *
     * @param spotCheck 抽查记录
     * @param templateIndex 模板索引（0, 1, 2）
     */
    private List<WxMaSubscribeMessage.MsgData> buildSpotCheckData(SpotCheck spotCheck, int templateIndex) {
        List<WxMaSubscribeMessage.MsgData> dataList = new ArrayList<>();

        String siteName = spotCheck.getSite() != null ? spotCheck.getSite().getName() : "当前岗位";
        String guardName = spotCheck.getGuard() != null ? spotCheck.getGuard().getName() : "保安";
        String deadlineStr = spotCheck.getDeadline() != null
                ? spotCheck.getDeadline().format(TIME_FORMATTER)
                : "15分钟内";

        if (templateIndex == 0) {
            // 模板1：打卡类型/打卡时间/打卡内容
            dataList.add(new WxMaSubscribeMessage.MsgData("phrase1", "随机抽查"));
            dataList.add(new WxMaSubscribeMessage.MsgData("date2", deadlineStr));
            dataList.add(new WxMaSubscribeMessage.MsgData("thing6", "请在15分钟内完成身份验证"));
        } else if (templateIndex == 1) {
            // 模板2：活动名称/签到方式/时间/温馨提示
            dataList.add(new WxMaSubscribeMessage.MsgData("thing1", "随机抽查-" + siteName));
            dataList.add(new WxMaSubscribeMessage.MsgData("thing2", "人脸识别"));
            dataList.add(new WxMaSubscribeMessage.MsgData("time5", deadlineStr));
            dataList.add(new WxMaSubscribeMessage.MsgData("thing16", "请在15分钟内完成验证，辛苦了"));
        } else {
            // 模板3：检查日期/检查人员/检查情况/检查类型
            dataList.add(new WxMaSubscribeMessage.MsgData("time1", deadlineStr));
            dataList.add(new WxMaSubscribeMessage.MsgData("thing2", guardName));
            dataList.add(new WxMaSubscribeMessage.MsgData("thing5", "请在15分钟内完成身份验证"));
            dataList.add(new WxMaSubscribeMessage.MsgData("thing3", "随机抽查"));
        }

        return dataList;
    }
}
