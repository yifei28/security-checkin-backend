package com.duhao.security.checkinapp.service;

import com.duhao.security.checkinapp.config.SpotCheckProperties;
import com.duhao.security.checkinapp.entity.*;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SpotCheckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DelayedTaskHandler 单元测试
 * 测试业务逻辑处理
 */
@ExtendWith(MockitoExtension.class)
class DelayedTaskHandlerTest {

    @Mock
    private DelayedTaskService delayedTaskService;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private SpotCheckRepository spotCheckRepository;

    @Mock
    private SpotCheckProperties properties;

    @Mock
    private WechatNotificationService notificationService;

    @InjectMocks
    private DelayedTaskHandler handler;

    private SecurityGuard testGuard;
    private WorkSite testSite;
    private CheckinRecord activeSession;
    private SpotCheck testSpotCheck;

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

        // 创建测试抽查
        testSpotCheck = new SpotCheck(activeSession, SpotCheckTriggerType.AUTOMATIC);
        setField(testSpotCheck, "id", 200L);
        setField(testSpotCheck, "version", 0L);
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

    // ==================== 工作片段超时测试 ====================

    @Test
    @DisplayName("工作片段超时处理 - 正常超时")
    void onSessionTimeout_Success() {
        // Given
        String taskValue = "100:0";
        when(delayedTaskService.parseValue(taskValue)).thenReturn(new long[]{100L, 0L});
        when(checkinRepository.findById(100L)).thenReturn(Optional.of(activeSession));
        when(spotCheckRepository.countByCheckinRecordId(100L)).thenReturn(3L);
        when(spotCheckRepository.countPassedByCheckinRecordId(100L)).thenReturn(2L);
        when(spotCheckRepository.findByCheckinRecordIdAndStatus(100L, SpotCheckStatus.PENDING))
                .thenReturn(Collections.singletonList(testSpotCheck));
        when(checkinRepository.save(any(CheckinRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(spotCheckRepository.save(any(SpotCheck.class))).thenAnswer(i -> i.getArgument(0));

        // When
        handler.onSessionTimeout(taskValue);

        // Then
        assertEquals(WorkStatus.TIMEOUT, activeSession.getStatus());
        verify(checkinRepository).save(activeSession);
        verify(spotCheckRepository).save(testSpotCheck);
        assertEquals(SpotCheckStatus.MISSED, testSpotCheck.getStatus());
    }

    @Test
    @DisplayName("工作片段超时处理 - 记录不存在")
    void onSessionTimeout_NotFound() {
        // Given
        String taskValue = "999:0";
        when(delayedTaskService.parseValue(taskValue)).thenReturn(new long[]{999L, 0L});
        when(checkinRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        handler.onSessionTimeout(taskValue);

        // Then
        verify(checkinRepository, never()).save(any());
    }

    @Test
    @DisplayName("工作片段超时处理 - 版本不匹配")
    void onSessionTimeout_VersionMismatch() {
        // Given
        String taskValue = "100:1";  // 期望版本1，但实际是0
        when(delayedTaskService.parseValue(taskValue)).thenReturn(new long[]{100L, 1L});
        when(checkinRepository.findById(100L)).thenReturn(Optional.of(activeSession));

        // When
        handler.onSessionTimeout(taskValue);

        // Then
        verify(checkinRepository, never()).save(any());
    }

    @Test
    @DisplayName("工作片段超时处理 - 状态已变更")
    void onSessionTimeout_StatusChanged() {
        // Given
        String taskValue = "100:0";
        activeSession.clockOut(31.2304, 121.4737, null);  // 已下岗

        when(delayedTaskService.parseValue(taskValue)).thenReturn(new long[]{100L, 0L});
        when(checkinRepository.findById(100L)).thenReturn(Optional.of(activeSession));

        // When
        handler.onSessionTimeout(taskValue);

        // Then
        verify(checkinRepository, never()).save(any());
    }

    // ==================== 抽查触发测试 ====================

    @Test
    @DisplayName("抽查触发处理 - 正常触发")
    void onSpotCheckTrigger_Success() {
        // Given
        String taskValue = "100:0";
        when(delayedTaskService.parseValue(taskValue)).thenReturn(new long[]{100L, 0L});
        when(checkinRepository.findById(100L)).thenReturn(Optional.of(activeSession));
        // 模拟已完成0次抽查
        when(spotCheckRepository.countByCheckinRecordId(100L)).thenReturn(0L);
        when(properties.getMaxChecksPerSession()).thenReturn(3);
        // 动态间隔配置
        when(properties.getIntervalRange(1)).thenReturn(new int[]{120, 200});
        when(spotCheckRepository.save(any(SpotCheck.class))).thenAnswer(i -> {
            SpotCheck check = i.getArgument(0);
            setField(check, "id", 300L);
            setField(check, "version", 0L);
            return check;
        });

        // When
        handler.onSpotCheckTrigger(taskValue);

        // Then
        verify(spotCheckRepository).save(any(SpotCheck.class));
        verify(notificationService).sendSpotCheckNotification(any(SpotCheck.class));
        verify(delayedTaskService).scheduleSpotCheckTimeout(any(), any(), any());
        verify(delayedTaskService).scheduleSpotCheckTrigger(eq(100L), eq(0L), any());
    }

    @Test
    @DisplayName("抽查触发处理 - 已达上限不触发")
    void onSpotCheckTrigger_MaxReached() {
        // Given
        String taskValue = "100:0";
        when(delayedTaskService.parseValue(taskValue)).thenReturn(new long[]{100L, 0L});
        when(checkinRepository.findById(100L)).thenReturn(Optional.of(activeSession));
        // 模拟已完成3次抽查（达到上限）
        when(spotCheckRepository.countByCheckinRecordId(100L)).thenReturn(3L);
        when(properties.getMaxChecksPerSession()).thenReturn(3);

        // When
        handler.onSpotCheckTrigger(taskValue);

        // Then - 不应创建新的抽查
        verify(spotCheckRepository, never()).save(any());
    }

    // ==================== 抽查超时测试 ====================

    @Test
    @DisplayName("抽查超时处理 - 正常超时")
    void onSpotCheckTimeout_Success() {
        // Given
        String taskValue = "200:0";
        when(delayedTaskService.parseValue(taskValue)).thenReturn(new long[]{200L, 0L});
        when(spotCheckRepository.findById(200L)).thenReturn(Optional.of(testSpotCheck));
        when(spotCheckRepository.save(any(SpotCheck.class))).thenAnswer(i -> i.getArgument(0));

        // When
        handler.onSpotCheckTimeout(taskValue);

        // Then
        assertEquals(SpotCheckStatus.MISSED, testSpotCheck.getStatus());
        verify(spotCheckRepository).save(testSpotCheck);
    }

    @Test
    @DisplayName("抽查超时处理 - 已处理")
    void onSpotCheckTimeout_AlreadyProcessed() {
        // Given
        String taskValue = "200:0";
        testSpotCheck.markPassed(31.2304, 121.4737, null);

        when(delayedTaskService.parseValue(taskValue)).thenReturn(new long[]{200L, 0L});
        when(spotCheckRepository.findById(200L)).thenReturn(Optional.of(testSpotCheck));

        // When
        handler.onSpotCheckTimeout(taskValue);

        // Then
        verify(spotCheckRepository, never()).save(any());
    }

    // ==================== 动态间隔时间生成测试 ====================

    @Test
    @DisplayName("生成下次抽查时间 - 阶段1（0次已抽）90-150分钟")
    void generateNextSpotCheckTime_Stage1() {
        // Given
        when(properties.getIntervalRange(0)).thenReturn(new int[]{90, 150});
        LocalDateTime from = LocalDateTime.now();

        // When
        LocalDateTime nextTime = handler.generateNextSpotCheckTime(from, 0);

        // Then
        assertNotNull(nextTime);
        long minutesDiff = java.time.Duration.between(from, nextTime).toMinutes();
        assertTrue(minutesDiff >= 90, "阶段1间隔应至少90分钟");
        assertTrue(minutesDiff <= 150, "阶段1间隔应最多150分钟");
    }

    @Test
    @DisplayName("生成下次抽查时间 - 阶段2（1次已抽）120-200分钟")
    void generateNextSpotCheckTime_Stage2() {
        // Given
        when(properties.getIntervalRange(1)).thenReturn(new int[]{120, 200});
        LocalDateTime from = LocalDateTime.now();

        // When
        LocalDateTime nextTime = handler.generateNextSpotCheckTime(from, 1);

        // Then
        assertNotNull(nextTime);
        long minutesDiff = java.time.Duration.between(from, nextTime).toMinutes();
        assertTrue(minutesDiff >= 120, "阶段2间隔应至少120分钟");
        assertTrue(minutesDiff <= 200, "阶段2间隔应最多200分钟");
    }

    @Test
    @DisplayName("生成下次抽查时间 - 阶段3（2次已抽）150-250分钟")
    void generateNextSpotCheckTime_Stage3() {
        // Given
        when(properties.getIntervalRange(2)).thenReturn(new int[]{150, 250});
        LocalDateTime from = LocalDateTime.now();

        // When
        LocalDateTime nextTime = handler.generateNextSpotCheckTime(from, 2);

        // Then
        assertNotNull(nextTime);
        long minutesDiff = java.time.Duration.between(from, nextTime).toMinutes();
        assertTrue(minutesDiff >= 150, "阶段3间隔应至少150分钟");
        assertTrue(minutesDiff <= 250, "阶段3间隔应最多250分钟");
    }

    @Test
    @DisplayName("生成下次抽查时间 - 已达上限返回null")
    void generateNextSpotCheckTime_MaxReached() {
        // Given
        when(properties.getIntervalRange(3)).thenReturn(null);
        LocalDateTime from = LocalDateTime.now();

        // When
        LocalDateTime nextTime = handler.generateNextSpotCheckTime(from, 3);

        // Then
        assertNull(nextTime, "已达抽查上限应返回null");
    }
}
