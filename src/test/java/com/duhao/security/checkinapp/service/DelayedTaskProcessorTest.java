package com.duhao.security.checkinapp.service;

import com.duhao.security.checkinapp.config.SpotCheckProperties;
import com.duhao.security.checkinapp.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DelayedTaskProcessor 单元测试
 * 测试调度器是否正确委托给 Handler 处理
 */
@ExtendWith(MockitoExtension.class)
class DelayedTaskProcessorTest {

    @Mock
    private DelayedTaskService delayedTaskService;

    @Mock
    private DelayedTaskHandler handler;

    @Mock
    private SpotCheckProperties properties;

    @InjectMocks
    private DelayedTaskProcessor processor;

    private SecurityGuard testGuard;
    private WorkSite testSite;
    private CheckinRecord activeSession;

    @BeforeEach
    void setUp() {
        // 创建测试工作地点
        testSite = new WorkSite();
        setField(testSite, "id", 1L);
        testSite.setName("测试站点");
        testSite.setLatitude(31.2304);
        testSite.setLongitude(121.4737);

        // 创建测试保安
        testGuard = new SecurityGuard();
        setField(testGuard, "id", 1L);
        testGuard.setName("张三");
        testGuard.setSite(testSite);

        // 创建活跃的工作片段
        activeSession = new CheckinRecord(testGuard, testSite, 31.2304, 121.4737, null);
        setField(activeSession, "id", 100L);
        setField(activeSession, "version", 0L);
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            // Try superclass
            try {
                var field = obj.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    // ==================== 调度委托测试 ====================

    @Test
    @DisplayName("processDelayedTasks - 委托工作片段超时处理")
    void processDelayedTasks_DelegatesSessionTimeout() {
        // Given
        Set<String> tasks = new HashSet<>();
        tasks.add("100:0");
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SESSION_TIMEOUT))
                .thenReturn(tasks);
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SPOTCHECK_TRIGGER))
                .thenReturn(null);
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SPOTCHECK_TIMEOUT))
                .thenReturn(null);

        // When
        processor.processDelayedTasks();

        // Then
        verify(handler).onSessionTimeout("100:0");
    }

    @Test
    @DisplayName("processDelayedTasks - 委托抽查触发处理")
    void processDelayedTasks_DelegatesSpotCheckTrigger() {
        // Given
        Set<String> tasks = new HashSet<>();
        tasks.add("100:0");
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SESSION_TIMEOUT))
                .thenReturn(null);
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SPOTCHECK_TRIGGER))
                .thenReturn(tasks);
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SPOTCHECK_TIMEOUT))
                .thenReturn(null);

        // When
        processor.processDelayedTasks();

        // Then
        verify(handler).onSpotCheckTrigger("100:0");
    }

    @Test
    @DisplayName("processDelayedTasks - 委托抽查超时处理")
    void processDelayedTasks_DelegatesSpotCheckTimeout() {
        // Given
        Set<String> tasks = new HashSet<>();
        tasks.add("200:0");
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SESSION_TIMEOUT))
                .thenReturn(null);
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SPOTCHECK_TRIGGER))
                .thenReturn(null);
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SPOTCHECK_TIMEOUT))
                .thenReturn(tasks);

        // When
        processor.processDelayedTasks();

        // Then
        verify(handler).onSpotCheckTimeout("200:0");
    }

    @Test
    @DisplayName("processDelayedTasks - 空队列不调用handler")
    void processDelayedTasks_EmptyQueues() {
        // Given
        when(delayedTaskService.pollDueTasks(any())).thenReturn(null);

        // When
        processor.processDelayedTasks();

        // Then
        verifyNoInteractions(handler);
    }

    @Test
    @DisplayName("processDelayedTasks - handler异常不影响其他任务")
    void processDelayedTasks_HandlerExceptionDoesNotAffectOthers() {
        // Given
        Set<String> tasks = new HashSet<>();
        tasks.add("100:0");
        tasks.add("101:0");
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SESSION_TIMEOUT))
                .thenReturn(tasks);
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SPOTCHECK_TRIGGER))
                .thenReturn(null);
        when(delayedTaskService.pollDueTasks(DelayedTaskService.KEY_SPOTCHECK_TIMEOUT))
                .thenReturn(null);
        doThrow(new RuntimeException("Test exception")).when(handler).onSessionTimeout("100:0");

        // When
        processor.processDelayedTasks();

        // Then - 两个任务都应该被尝试处理
        verify(handler, times(2)).onSessionTimeout(any());
    }

    // ==================== 新工作片段预约测试 ====================

    @Test
    @DisplayName("为新工作片段预约超时和抽查")
    void scheduleForNewSession() {
        // Given
        when(properties.getSessionTimeoutHours()).thenReturn(16);
        // 模拟 handler 生成首次抽查时间（阶段1: 90-150分钟）
        when(handler.generateNextSpotCheckTime(any(LocalDateTime.class), eq(0)))
                .thenReturn(LocalDateTime.now().plusMinutes(120));

        // When
        processor.scheduleForNewSession(activeSession);

        // Then
        verify(delayedTaskService).scheduleSessionTimeout(eq(100L), eq(0L), any(LocalDateTime.class));
        verify(delayedTaskService).scheduleSpotCheckTrigger(eq(100L), eq(0L), any(LocalDateTime.class));
        verify(handler).generateNextSpotCheckTime(any(LocalDateTime.class), eq(0));
    }

    @Test
    @DisplayName("为新工作片段预约 - 无法生成抽查时间时只预约超时")
    void scheduleForNewSession_NoSpotCheck() {
        // Given
        when(properties.getSessionTimeoutHours()).thenReturn(16);
        // 模拟 handler 返回 null（配置问题）
        when(handler.generateNextSpotCheckTime(any(LocalDateTime.class), eq(0)))
                .thenReturn(null);

        // When
        processor.scheduleForNewSession(activeSession);

        // Then - 只预约超时，不预约抽查
        verify(delayedTaskService).scheduleSessionTimeout(eq(100L), eq(0L), any(LocalDateTime.class));
        verify(delayedTaskService, never()).scheduleSpotCheckTrigger(any(), any(), any());
    }

    // ==================== 取消预约测试 ====================

    @Test
    @DisplayName("取消工作片段的所有预约")
    void cancelSessionSchedules() {
        // When
        processor.cancelSessionSchedules(100L);

        // Then
        verify(delayedTaskService).cancelSessionTimeout(100L);
        verify(delayedTaskService).cancelSpotCheckTrigger(100L);
    }
}
