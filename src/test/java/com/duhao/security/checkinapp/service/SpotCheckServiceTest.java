package com.duhao.security.checkinapp.service;

import com.duhao.security.checkinapp.config.SpotCheckProperties;
import com.duhao.security.checkinapp.entity.*;
import com.duhao.security.checkinapp.impl.SpotCheckServiceImpl;
import com.duhao.security.checkinapp.repository.CheckinLocationRepository;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.repository.SpotCheckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SpotCheckService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SpotCheckServiceTest {

    @Mock
    private SpotCheckRepository spotCheckRepository;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private SecurityGuardRepository guardRepository;

    @Mock
    private DelayedTaskService delayedTaskService;

    @Mock
    private SpotCheckProperties properties;

    @Mock
    private CheckinLocationRepository locationRepository;

    @Mock
    private WechatNotificationService notificationService;

    @InjectMocks
    private SpotCheckServiceImpl spotCheckService;

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
        testSite.setAllowedRadiusMeters(500.0);

        // 创建测试保安
        testGuard = new SecurityGuard();
        setField(testGuard, "id", 1L);
        testGuard.setName("张三");
        testGuard.setOpenId("test-openid");
        testGuard.setSite(testSite);

        // 创建活跃的工作片段
        activeSession = new CheckinRecord(testGuard, testSite, 31.2304, 121.4737, null);
        setField(activeSession, "id", 100L);

        // 创建测试抽查
        testSpotCheck = new SpotCheck(activeSession, SpotCheckTriggerType.AUTOMATIC);
        setField(testSpotCheck, "id", 200L);
        setField(testSpotCheck, "version", 0L);
    }

    // 辅助方法：使用反射设置私有字段
    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            // ignore
        }
    }

    // ==================== 创建抽查测试 ====================

    @Test
    @DisplayName("创建抽查成功 - 保安在岗")
    void createSpotCheck_Success() {
        // Given
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE))
                .thenReturn(Optional.of(activeSession));
        when(spotCheckRepository.save(any(SpotCheck.class))).thenAnswer(i -> {
            SpotCheck check = i.getArgument(0);
            setField(check, "id", 200L);
            setField(check, "version", 0L);
            return check;
        });

        // When
        SpotCheck result = spotCheckService.createSpotCheck(testGuard, SpotCheckTriggerType.AUTOMATIC, null);

        // Then
        assertNotNull(result);
        assertEquals(SpotCheckStatus.PENDING, result.getStatus());
        verify(delayedTaskService).scheduleSpotCheckTimeout(any(), any(), any());
    }

    @Test
    @DisplayName("创建抽查失败 - 保安未在岗")
    void createSpotCheck_NotWorking() {
        // Given
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // When
        SpotCheck result = spotCheckService.createSpotCheck(testGuard, SpotCheckTriggerType.AUTOMATIC, null);

        // Then
        assertNull(result);
        verify(spotCheckRepository, never()).save(any());
    }

    // ==================== 完成抽查测试 ====================

    @Test
    @DisplayName("完成抽查成功 - 位置在范围内")
    void completeSpotCheck_Success() {
        // Given
        when(spotCheckRepository.findById(200L)).thenReturn(Optional.of(testSpotCheck));
        when(spotCheckRepository.save(any(SpotCheck.class))).thenAnswer(i -> i.getArgument(0));

        // When
        SpotCheckService.SpotCheckResult result = spotCheckService.completeSpotCheck(
                200L,
                31.2305,  // 距离站点约11米
                121.4738,
                "http://example.com/face.jpg"
        );

        // Then
        assertTrue(result.success());
        assertEquals("验证成功", result.message());
        assertNotNull(result.spotCheck());
        assertEquals(SpotCheckStatus.PASSED, result.spotCheck().getStatus());
    }

    @Test
    @DisplayName("完成抽查失败 - 抽查不存在")
    void completeSpotCheck_NotFound() {
        // Given
        when(spotCheckRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        SpotCheckService.SpotCheckResult result = spotCheckService.completeSpotCheck(
                999L, 31.2304, 121.4737, "http://example.com/face.jpg"
        );

        // Then
        assertFalse(result.success());
        assertEquals("抽查记录不存在", result.message());
    }

    @Test
    @DisplayName("完成抽查失败 - 抽查已处理")
    void completeSpotCheck_AlreadyProcessed() {
        // Given
        testSpotCheck.markPassed(31.2304, 121.4737, "http://example.com/face.jpg");
        when(spotCheckRepository.findById(200L)).thenReturn(Optional.of(testSpotCheck));

        // When
        SpotCheckService.SpotCheckResult result = spotCheckService.completeSpotCheck(
                200L, 31.2304, 121.4737, "http://example.com/face.jpg"
        );

        // Then
        assertFalse(result.success());
        assertTrue(result.message().contains("抽查已处理"));
    }

    @Test
    @DisplayName("完成抽查失败 - 抽查已超时")
    void completeSpotCheck_Expired() {
        // Given
        // 设置截止时间为过去
        setField(testSpotCheck, "deadline", LocalDateTime.now().minusMinutes(1));
        when(spotCheckRepository.findById(200L)).thenReturn(Optional.of(testSpotCheck));
        when(spotCheckRepository.save(any(SpotCheck.class))).thenAnswer(i -> i.getArgument(0));

        // When
        SpotCheckService.SpotCheckResult result = spotCheckService.completeSpotCheck(
                200L, 31.2304, 121.4737, "http://example.com/face.jpg"
        );

        // Then
        assertFalse(result.success());
        assertEquals("抽查已超时", result.message());
    }

    @Test
    @DisplayName("完成抽查失败 - 位置超出范围")
    void completeSpotCheck_OutOfRange() {
        // Given
        when(spotCheckRepository.findById(200L)).thenReturn(Optional.of(testSpotCheck));
        when(spotCheckRepository.save(any(SpotCheck.class))).thenAnswer(i -> i.getArgument(0));

        // When - 位置距离约1km
        SpotCheckService.SpotCheckResult result = spotCheckService.completeSpotCheck(
                200L, 31.2400, 121.4800, "http://example.com/face.jpg"
        );

        // Then
        assertFalse(result.success());
        assertTrue(result.message().contains("位置超出允许范围"));
    }

    // ==================== 获取待处理抽查测试 ====================

    @Test
    @DisplayName("获取待处理抽查 - 存在")
    void getActiveSpotCheckForGuard_Found() {
        // Given
        when(spotCheckRepository.findPendingByGuardId(1L)).thenReturn(Optional.of(testSpotCheck));

        // When
        Optional<SpotCheck> result = spotCheckService.getActiveSpotCheckForGuard(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(200L, result.get().getId());
    }

    @Test
    @DisplayName("获取待处理抽查 - 不存在")
    void getActiveSpotCheckForGuard_NotFound() {
        // Given
        when(spotCheckRepository.findPendingByGuardId(1L)).thenReturn(Optional.empty());

        // When
        Optional<SpotCheck> result = spotCheckService.getActiveSpotCheckForGuard(1L);

        // Then
        assertTrue(result.isEmpty());
    }

    // ==================== 手动触发抽查测试 ====================

    @Test
    @DisplayName("手动触发抽查成功")
    void triggerManualSpotChecks_Success() {
        // Given
        List<Long> guardIds = Arrays.asList(1L, 2L);
        SecurityGuard guard2 = new SecurityGuard();
        setField(guard2, "id", 2L);
        guard2.setName("李四");
        guard2.setSite(testSite);

        CheckinRecord session2 = new CheckinRecord(guard2, testSite, 31.2304, 121.4737, null);
        setField(session2, "id", 101L);

        when(guardRepository.findById(1L)).thenReturn(Optional.of(testGuard));
        when(guardRepository.findById(2L)).thenReturn(Optional.of(guard2));
        when(spotCheckRepository.findPendingByGuardId(any())).thenReturn(Optional.empty());
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE))
                .thenReturn(Optional.of(activeSession));
        when(checkinRepository.findByGuardAndStatus(guard2, WorkStatus.ACTIVE))
                .thenReturn(Optional.of(session2));
        when(spotCheckRepository.save(any(SpotCheck.class))).thenAnswer(i -> {
            SpotCheck check = i.getArgument(0);
            setField(check, "id", 300L);
            setField(check, "version", 0L);
            return check;
        });

        // When
        List<SpotCheck> results = spotCheckService.triggerManualSpotChecks(guardIds, null);

        // Then
        assertEquals(2, results.size());
        verify(spotCheckRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("手动触发抽查 - 跳过已有待处理抽查的保安")
    void triggerManualSpotChecks_SkipPending() {
        // Given
        List<Long> guardIds = Arrays.asList(1L);

        when(guardRepository.findById(1L)).thenReturn(Optional.of(testGuard));
        when(spotCheckRepository.findPendingByGuardId(1L)).thenReturn(Optional.of(testSpotCheck));

        // When
        List<SpotCheck> results = spotCheckService.triggerManualSpotChecks(guardIds, null);

        // Then
        assertTrue(results.isEmpty());
        verify(spotCheckRepository, never()).save(any());
    }

    // ==================== 取消抽查测试 ====================

    @Test
    @DisplayName("取消抽查成功")
    void cancelSpotCheck_Success() {
        // Given
        when(spotCheckRepository.findById(200L)).thenReturn(Optional.of(testSpotCheck));
        when(spotCheckRepository.save(any(SpotCheck.class))).thenAnswer(i -> i.getArgument(0));

        // When
        boolean result = spotCheckService.cancelSpotCheck(200L, "测试取消");

        // Then
        assertTrue(result);
        assertEquals(SpotCheckStatus.MISSED, testSpotCheck.getStatus());
        assertTrue(testSpotCheck.getFailReason().contains("管理员取消"));
        verify(delayedTaskService).cancelSpotCheckTimeout(200L);
    }

    @Test
    @DisplayName("取消抽查失败 - 抽查不存在")
    void cancelSpotCheck_NotFound() {
        // Given
        when(spotCheckRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        boolean result = spotCheckService.cancelSpotCheck(999L, "测试取消");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("取消抽查失败 - 抽查已处理")
    void cancelSpotCheck_AlreadyProcessed() {
        // Given
        testSpotCheck.markPassed(31.2304, 121.4737, null);
        when(spotCheckRepository.findById(200L)).thenReturn(Optional.of(testSpotCheck));

        // When
        boolean result = spotCheckService.cancelSpotCheck(200L, "测试取消");

        // Then
        assertFalse(result);
        verify(spotCheckRepository, never()).save(any());
    }

    // ==================== 统计测试 ====================

    @Test
    @DisplayName("获取抽查统计")
    void getStatistics() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        when(spotCheckRepository.countByStatusWithFilters(start, end, SpotCheckStatus.PASSED, null, null))
                .thenReturn(80L);
        when(spotCheckRepository.countByStatusWithFilters(start, end, SpotCheckStatus.MISSED, null, null))
                .thenReturn(15L);
        when(spotCheckRepository.countByStatusWithFilters(start, end, SpotCheckStatus.PENDING, null, null))
                .thenReturn(5L);

        // When
        SpotCheckService.SpotCheckStatistics stats = spotCheckService.getStatistics(start, end, null, null);

        // Then
        assertEquals(100L, stats.totalCount());
        assertEquals(80L, stats.completedCount());  // PASSED
        assertEquals(15L, stats.missedCount());
        assertEquals(5L, stats.pendingCount());
        assertEquals(84, stats.completionRate());  // 80 / (80+15) * 100 = 84
    }
}
