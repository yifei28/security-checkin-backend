package com.duhao.security.checkinapp.service;

import com.duhao.security.checkinapp.config.SpotCheckProperties;
import com.duhao.security.checkinapp.dto.WorkEndRequest;
import com.duhao.security.checkinapp.dto.WorkResponse;
import com.duhao.security.checkinapp.dto.WorkStartRequest;
import com.duhao.security.checkinapp.dto.WorkStatusResponse;
import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.entity.WorkStatus;
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
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WorkService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class WorkServiceTest {

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private SecurityGuardRepository guardRepository;

    @Mock
    private SpotCheckRepository spotCheckRepository;

    @Mock
    private CheckinLocationRepository locationRepository;

    @Mock
    private DelayedTaskProcessor delayedTaskProcessor;

    @Mock
    private SpotCheckProperties properties;

    @InjectMocks
    private WorkService workService;

    private SecurityGuard testGuard;
    private WorkSite testSite;

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
    }

    // 辅助方法：使用反射设置私有字段
    private void setField(Object obj, String fieldName, Object value) {
        try {
            Class<?> clazz = obj.getClass();
            while (clazz != null) {
                try {
                    var field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(obj, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    // ==================== 上岗测试 ====================

    @Test
    @DisplayName("上岗成功 - 位置在允许范围内")
    void startWork_Success() {
        // Given
        WorkStartRequest request = new WorkStartRequest();
        request.setLatitude(31.2305);  // 距离测试站点约11米
        request.setLongitude(121.4738);
        request.setFaceImageUrl("http://example.com/face.jpg");

        when(guardRepository.findById(1L)).thenReturn(Optional.of(testGuard));
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE)).thenReturn(Optional.empty());
        when(checkinRepository.save(any(CheckinRecord.class))).thenAnswer(invocation -> {
            CheckinRecord record = invocation.getArgument(0);
            // 模拟设置ID
            try {
                var idField = CheckinRecord.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(record, 100L);
            } catch (Exception e) {
                // ignore
            }
            return record;
        });

        // When
        WorkResponse response = workService.startWork(1L, request);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("上岗成功", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("测试站点", response.getData().getSiteName());

        verify(delayedTaskProcessor).scheduleForNewSession(any(CheckinRecord.class));
    }

    @Test
    @DisplayName("上岗失败 - 保安不存在")
    void startWork_GuardNotFound() {
        // Given
        WorkStartRequest request = new WorkStartRequest();
        request.setLatitude(31.2304);
        request.setLongitude(121.4737);

        when(guardRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        WorkResponse response = workService.startWork(999L, request);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("保安不存在", response.getMessage());
    }

    @Test
    @DisplayName("上岗失败 - 已在岗中")
    void startWork_AlreadyWorking() {
        // Given
        WorkStartRequest request = new WorkStartRequest();
        request.setLatitude(31.2304);
        request.setLongitude(121.4737);

        CheckinRecord activeRecord = new CheckinRecord(testGuard, testSite, 31.2304, 121.4737, null);

        when(guardRepository.findById(1L)).thenReturn(Optional.of(testGuard));
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE)).thenReturn(Optional.of(activeRecord));

        // When
        WorkResponse response = workService.startWork(1L, request);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("您已在岗中，请先下岗", response.getMessage());
    }

    @Test
    @DisplayName("上岗失败 - 未分配工作地点")
    void startWork_NoSiteAssigned() {
        // Given
        testGuard.setSite(null);  // 未分配站点

        WorkStartRequest request = new WorkStartRequest();
        request.setLatitude(31.2304);
        request.setLongitude(121.4737);

        when(guardRepository.findById(1L)).thenReturn(Optional.of(testGuard));
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE)).thenReturn(Optional.empty());

        // When
        WorkResponse response = workService.startWork(1L, request);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("您未分配工作地点，请联系管理员", response.getMessage());
    }

    @Test
    @DisplayName("上岗失败 - 位置超出允许范围")
    void startWork_OutOfRange() {
        // Given
        WorkStartRequest request = new WorkStartRequest();
        request.setLatitude(31.2400);  // 距离测试站点约1km
        request.setLongitude(121.4800);
        request.setFaceImageUrl("http://example.com/face.jpg");

        when(guardRepository.findById(1L)).thenReturn(Optional.of(testGuard));
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE)).thenReturn(Optional.empty());

        // When
        WorkResponse response = workService.startWork(1L, request);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("位置超出允许范围"));
    }

    // ==================== 下岗测试 ====================

    @Test
    @DisplayName("下岗成功 - 工作时长满足要求")
    void endWork_Success() {
        // Given
        WorkEndRequest request = new WorkEndRequest();
        request.setLatitude(31.2305);
        request.setLongitude(121.4738);
        request.setFaceImageUrl("http://example.com/face.jpg");

        // 创建一个2小时前开始的工作片段
        CheckinRecord activeRecord = new CheckinRecord(testGuard, testSite, 31.2304, 121.4737, null);
        // 使用反射设置开始时间为2小时前
        try {
            var startTimeField = CheckinRecord.class.getDeclaredField("startTime");
            startTimeField.setAccessible(true);
            startTimeField.set(activeRecord, LocalDateTime.now().minusHours(2));
        } catch (Exception e) {
            // ignore
        }

        when(guardRepository.findById(1L)).thenReturn(Optional.of(testGuard));
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE)).thenReturn(Optional.of(activeRecord));
        when(properties.getMinWorkHours()).thenReturn(1);
        when(spotCheckRepository.countByCheckinRecordId(any())).thenReturn(2L);
        when(spotCheckRepository.countPassedByCheckinRecordId(any())).thenReturn(2L);
        when(spotCheckRepository.findByCheckinRecordIdAndStatus(any(), any())).thenReturn(Collections.emptyList());
        when(checkinRepository.save(any(CheckinRecord.class))).thenAnswer(i -> i.getArgument(0));

        // When
        WorkResponse response = workService.endWork(1L, request);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("下岗成功", response.getMessage());
        assertNotNull(response.getData());

        verify(delayedTaskProcessor).cancelSessionSchedules(any());
    }

    @Test
    @DisplayName("下岗失败 - 未在岗")
    void endWork_NotWorking() {
        // Given
        WorkEndRequest request = new WorkEndRequest();
        request.setLatitude(31.2304);
        request.setLongitude(121.4737);

        when(guardRepository.findById(1L)).thenReturn(Optional.of(testGuard));
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE)).thenReturn(Optional.empty());

        // When
        WorkResponse response = workService.endWork(1L, request);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("您未在岗，无法下岗", response.getMessage());
    }

    @Test
    @DisplayName("下岗失败 - 工作时长不足")
    void endWork_InsufficientWorkTime() {
        // Given
        WorkEndRequest request = new WorkEndRequest();
        request.setLatitude(31.2305);
        request.setLongitude(121.4738);

        // 创建一个刚开始的工作片段（工作时长不足1小时）
        CheckinRecord activeRecord = new CheckinRecord(testGuard, testSite, 31.2304, 121.4737, null);

        when(guardRepository.findById(1L)).thenReturn(Optional.of(testGuard));
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE)).thenReturn(Optional.of(activeRecord));
        when(properties.getMinWorkHours()).thenReturn(1);

        // When
        WorkResponse response = workService.endWork(1L, request);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("工作时长不足"));
    }

    // ==================== 工作状态测试 ====================

    @Test
    @DisplayName("获取工作状态 - 在岗中")
    void getWorkStatus_Working() {
        // Given
        CheckinRecord activeRecord = new CheckinRecord(testGuard, testSite, 31.2304, 121.4737, null);
        try {
            var idField = CheckinRecord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(activeRecord, 100L);
        } catch (Exception e) {
            // ignore
        }

        when(guardRepository.findById(1L)).thenReturn(Optional.of(testGuard));
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE)).thenReturn(Optional.of(activeRecord));
        when(spotCheckRepository.findPendingByGuardId(1L)).thenReturn(Optional.empty());

        // When
        WorkStatusResponse response = workService.getWorkStatus(1L);

        // Then
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertTrue(response.getData().isWorking());
        assertEquals("测试站点", response.getData().getSiteName());
    }

    @Test
    @DisplayName("获取工作状态 - 未在岗")
    void getWorkStatus_NotWorking() {
        // Given
        when(guardRepository.findById(1L)).thenReturn(Optional.of(testGuard));
        when(checkinRepository.findByGuardAndStatus(testGuard, WorkStatus.ACTIVE)).thenReturn(Optional.empty());

        // When
        WorkStatusResponse response = workService.getWorkStatus(1L);

        // Then
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertFalse(response.getData().isWorking());
    }

    @Test
    @DisplayName("获取工作状态 - 保安不存在")
    void getWorkStatus_GuardNotFound() {
        // Given
        when(guardRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        WorkStatusResponse response = workService.getWorkStatus(999L);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("保安不存在", response.getMessage());
    }
}
