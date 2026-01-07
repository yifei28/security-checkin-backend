package com.duhao.security.checkinapp.service;

import cn.binarywang.wx.miniapp.api.WxMaMsgService;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import com.duhao.security.checkinapp.config.WechatNotificationProperties;
import com.duhao.security.checkinapp.entity.*;
import com.duhao.security.checkinapp.impl.WechatNotificationServiceImpl;
import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WechatNotificationService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class WechatNotificationServiceTest {

    @Mock
    private WxMaService wxMaService;

    @Mock
    private WxMaMsgService wxMaMsgService;

    @Mock
    private WechatNotificationProperties properties;

    @InjectMocks
    private WechatNotificationServiceImpl notificationService;

    private SecurityGuard testGuard;
    private WorkSite testSite;
    private CheckinRecord activeSession;
    private SpotCheck testSpotCheck;

    @BeforeEach
    void setUp() {
        // 配置 wxMaService.getMsgService() 返回 mock
        lenient().when(wxMaService.getMsgService()).thenReturn(wxMaMsgService);

        // 创建测试工作地点
        testSite = new WorkSite();
        setField(testSite, "id", 1L);
        testSite.setName("测试站点");

        // 创建测试保安
        testGuard = new SecurityGuard();
        setField(testGuard, "id", 1L);
        testGuard.setName("张三");
        testGuard.setOpenId("test-openid-12345");
        testGuard.setSite(testSite);

        // 创建活跃的工作片段
        activeSession = new CheckinRecord(testGuard, testSite, 31.2304, 121.4737, null);
        setField(activeSession, "id", 100L);

        // 创建测试抽查
        testSpotCheck = new SpotCheck(activeSession, SpotCheckTriggerType.AUTOMATIC);
        setField(testSpotCheck, "id", 200L);
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            // ignore
        }
    }

    // ==================== 发送抽查通知测试 ====================

    @Test
    @DisplayName("发送抽查通知成功")
    void sendSpotCheckNotification_Success() throws WxErrorException {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getAvailableTemplateIds()).thenReturn(List.of("test-template-id"));
        when(properties.getSpotCheckPage()).thenReturn("pages/spot-check/index");
        when(properties.getMiniprogramState()).thenReturn("developer");
        when(properties.getLang()).thenReturn("zh_CN");
        doNothing().when(wxMaMsgService).sendSubscribeMsg(any(WxMaSubscribeMessage.class));

        // When
        notificationService.sendSpotCheckNotification(testSpotCheck);

        // Then - 验证发送的消息内容
        ArgumentCaptor<WxMaSubscribeMessage> captor = ArgumentCaptor.forClass(WxMaSubscribeMessage.class);
        verify(wxMaMsgService).sendSubscribeMsg(captor.capture());

        WxMaSubscribeMessage message = captor.getValue();
        assertEquals("test-openid-12345", message.getToUser());
        assertEquals("test-template-id", message.getTemplateId());
        assertTrue(message.getPage().contains("spotCheckId=200"));
        assertEquals("developer", message.getMiniprogramState());
    }

    @Test
    @DisplayName("发送抽查通知失败 - 功能已禁用")
    void sendSpotCheckNotification_Disabled() throws WxErrorException {
        // Given
        when(properties.isEnabled()).thenReturn(false);

        // When
        notificationService.sendSpotCheckNotification(testSpotCheck);

        // Then
        verify(wxMaMsgService, never()).sendSubscribeMsg(any());
    }

    @Test
    @DisplayName("发送抽查通知失败 - 模板ID未配置")
    void sendSpotCheckNotification_NoTemplateId() throws WxErrorException {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getAvailableTemplateIds()).thenReturn(List.of());

        // When
        notificationService.sendSpotCheckNotification(testSpotCheck);

        // Then
        verify(wxMaMsgService, never()).sendSubscribeMsg(any());
    }

    @Test
    @DisplayName("发送抽查通知失败 - 保安无OpenID")
    void sendSpotCheckNotification_NoOpenId() throws WxErrorException {
        // Given
        testGuard.setOpenId(null);
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getAvailableTemplateIds()).thenReturn(List.of("test-template-id"));

        // When
        notificationService.sendSpotCheckNotification(testSpotCheck);

        // Then
        verify(wxMaMsgService, never()).sendSubscribeMsg(any());
    }

    @Test
    @DisplayName("发送抽查通知失败 - 用户未订阅 (43101)")
    void sendSpotCheckNotification_UserNotSubscribed() throws WxErrorException {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getAvailableTemplateIds()).thenReturn(List.of("test-template-id"));
        when(properties.getSpotCheckPage()).thenReturn("pages/spot-check/index");
        when(properties.getMiniprogramState()).thenReturn("developer");
        when(properties.getLang()).thenReturn("zh_CN");

        WxError error = new WxError();
        error.setErrorCode(43101);
        error.setErrorMsg("user refuse to accept the msg");
        doThrow(new WxErrorException(error)).when(wxMaMsgService).sendSubscribeMsg(any());

        // When - 用户未订阅不应该抛出异常
        assertDoesNotThrow(() -> notificationService.sendSpotCheckNotification(testSpotCheck));
    }

    @Test
    @DisplayName("发送抽查通知失败 - 其他微信错误")
    void sendSpotCheckNotification_WxError() throws WxErrorException {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getAvailableTemplateIds()).thenReturn(List.of("test-template-id"));
        when(properties.getSpotCheckPage()).thenReturn("pages/spot-check/index");
        when(properties.getMiniprogramState()).thenReturn("developer");
        when(properties.getLang()).thenReturn("zh_CN");

        WxError error = new WxError();
        error.setErrorCode(47003);
        error.setErrorMsg("template not exist");
        doThrow(new WxErrorException(error)).when(wxMaMsgService).sendSubscribeMsg(any());

        // When - 其他错误也不应该抛出异常（异步方法内部处理）
        assertDoesNotThrow(() -> notificationService.sendSpotCheckNotification(testSpotCheck));
    }

    @Test
    @DisplayName("模板轮换 - 多模板按顺序使用")
    void sendSpotCheckNotification_TemplateRotation() throws WxErrorException {
        // Given - 配置3个模板
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getAvailableTemplateIds()).thenReturn(List.of("template-1", "template-2", "template-3"));
        when(properties.getSpotCheckPage()).thenReturn("pages/spot-check/index");
        when(properties.getMiniprogramState()).thenReturn("developer");
        when(properties.getLang()).thenReturn("zh_CN");
        doNothing().when(wxMaMsgService).sendSubscribeMsg(any(WxMaSubscribeMessage.class));

        // When - 连续发送3次通知
        notificationService.sendSpotCheckNotification(testSpotCheck);
        notificationService.sendSpotCheckNotification(testSpotCheck);
        notificationService.sendSpotCheckNotification(testSpotCheck);

        // Then - 验证使用了不同的模板
        ArgumentCaptor<WxMaSubscribeMessage> captor = ArgumentCaptor.forClass(WxMaSubscribeMessage.class);
        verify(wxMaMsgService, times(3)).sendSubscribeMsg(captor.capture());

        List<WxMaSubscribeMessage> messages = captor.getAllValues();
        assertEquals("template-1", messages.get(0).getTemplateId());
        assertEquals("template-2", messages.get(1).getTemplateId());
        assertEquals("template-3", messages.get(2).getTemplateId());
    }
}
