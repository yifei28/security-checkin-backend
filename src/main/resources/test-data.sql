-- 测试数据插入脚本
-- 用于测试CheckinRecord表与前端的兼容性

-- 首先确保有一些测试用的保安和站点数据
-- 插入测试站点（如果不存在）
INSERT INTO work_site (id, name, latitude, longitude, allowed_radius_meters) VALUES 
(1, '办公大楼A座', 39.9088, 116.3974, 100.0),
(2, '办公大楼B座', 39.9120, 116.4010, 150.0),
(3, '科技园C区', 39.9150, 116.4050, 200.0)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 插入测试保安（如果不存在）
INSERT INTO security_guard (id, name, phone_number, employee_id, site_id, open_id) VALUES 
(1, '张三', '13800138001', '20250101-1234567-ABC001', 1, 'wx_openid_001'),
(2, '李四', '13800138002', '20250102-1234567-ABC002', 1, 'wx_openid_002'),
(3, '王五', '13800138003', '20250103-1234567-ABC003', 2, 'wx_openid_003'),
(4, '赵六', '13800138004', '20250104-1234567-ABC004', 2, 'wx_openid_004'),
(5, '钱七', '13800138005', '20250105-1234567-ABC005', 3, 'wx_openid_005')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 插入多样化的签到记录
-- 1. 成功的签到记录（今天）
INSERT INTO checkin_record (guard_id, site_id, timestamp, latitude, longitude, face_image_url, status, reason) VALUES 
(1, 1, NOW(), 39.9088, 116.3974, 'https://example.com/faces/zhang_success.jpg', 'SUCCESS', NULL),
(2, 1, NOW() - INTERVAL 2 HOUR, 39.9089, 116.3975, 'https://example.com/faces/li_success.jpg', 'SUCCESS', NULL),
(3, 2, NOW() - INTERVAL 3 HOUR, 39.9121, 116.4011, NULL, 'SUCCESS', NULL);

-- 2. 失败的签到记录（位置超出范围）
INSERT INTO checkin_record (guard_id, site_id, timestamp, latitude, longitude, face_image_url, status, reason) VALUES 
(4, 2, NOW() - INTERVAL 1 HOUR, 39.9200, 116.4100, 'https://example.com/faces/zhao_failed.jpg', 'FAILED', '签到位置超出允许范围（实际距离：850米）'),
(5, 3, NOW() - INTERVAL 30 MINUTE, 39.9300, 116.4200, NULL, 'FAILED', '签到位置超出允许范围（实际距离：1500米）');

-- 3. 待处理的签到记录
INSERT INTO checkin_record (guard_id, site_id, timestamp, latitude, longitude, face_image_url, status, reason) VALUES 
(1, 1, NOW() - INTERVAL 4 HOUR, 39.9088, 116.3974, 'https://example.com/faces/zhang_pending.jpg', 'PENDING', '人脸识别中，请稍候');

-- 4. 昨天的签到记录
INSERT INTO checkin_record (guard_id, site_id, timestamp, latitude, longitude, face_image_url, status, reason) VALUES 
(1, 1, NOW() - INTERVAL 1 DAY, 39.9087, 116.3973, 'https://example.com/faces/zhang_yesterday.jpg', 'SUCCESS', NULL),
(2, 1, NOW() - INTERVAL 1 DAY - INTERVAL 2 HOUR, 39.9090, 116.3976, NULL, 'SUCCESS', NULL),
(3, 2, NOW() - INTERVAL 1 DAY - INTERVAL 3 HOUR, 39.9119, 116.4009, 'https://example.com/faces/wang_yesterday.jpg', 'SUCCESS', NULL);

-- 5. 前天的签到记录
INSERT INTO checkin_record (guard_id, site_id, timestamp, latitude, longitude, face_image_url, status, reason) VALUES 
(1, 1, NOW() - INTERVAL 2 DAY, 39.9086, 116.3972, NULL, 'SUCCESS', NULL),
(2, 1, NOW() - INTERVAL 2 DAY - INTERVAL 1 HOUR, 39.9091, 116.3977, 'https://example.com/faces/li_2days_ago.jpg', 'SUCCESS', NULL);

-- 6. 一周内的各种记录
INSERT INTO checkin_record (guard_id, site_id, timestamp, latitude, longitude, face_image_url, status, reason) VALUES 
(1, 1, NOW() - INTERVAL 3 DAY, 39.9088, 116.3974, NULL, 'SUCCESS', NULL),
(1, 1, NOW() - INTERVAL 4 DAY, 39.9087, 116.3973, 'https://example.com/faces/zhang_4days.jpg', 'SUCCESS', NULL),
(1, 1, NOW() - INTERVAL 5 DAY, 39.9089, 116.3975, NULL, 'FAILED', '当前时间不在签到时间段内'),
(2, 1, NOW() - INTERVAL 3 DAY, 39.9090, 116.3976, NULL, 'SUCCESS', NULL),
(2, 1, NOW() - INTERVAL 4 DAY, 39.9088, 116.3974, 'https://example.com/faces/li_4days.jpg', 'SUCCESS', NULL),
(3, 2, NOW() - INTERVAL 3 DAY, 39.9120, 116.4010, NULL, 'SUCCESS', NULL),
(4, 2, NOW() - INTERVAL 4 DAY, 39.9121, 116.4011, NULL, 'SUCCESS', NULL),
(5, 3, NOW() - INTERVAL 5 DAY, 39.9150, 116.4050, 'https://example.com/faces/qian_5days.jpg', 'SUCCESS', NULL);

-- 查询插入的数据以验证
SELECT 
    cr.id,
    sg.name as guard_name,
    sg.employee_id,
    ws.name as site_name,
    cr.timestamp,
    cr.latitude,
    cr.longitude,
    cr.status,
    cr.reason,
    cr.face_image_url
FROM checkin_record cr
JOIN security_guard sg ON cr.guard_id = sg.id
JOIN work_site ws ON cr.site_id = ws.id
ORDER BY cr.timestamp DESC
LIMIT 20;