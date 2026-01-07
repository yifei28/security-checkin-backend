-- ============================================================
-- V2: 重构签到记录为工作片段模型
-- 支持上岗-下岗周期，抽查关联工作片段
-- ============================================================

-- 1. CheckinRecord 表重构
-- --------------------------------------------------------

-- 1.1 添加 version 字段（乐观锁）
ALTER TABLE checkin_record ADD COLUMN version BIGINT DEFAULT 0;

-- 1.2 重命名现有字段
ALTER TABLE checkin_record RENAME COLUMN timestamp TO start_time;
ALTER TABLE checkin_record RENAME COLUMN latitude TO start_latitude;
ALTER TABLE checkin_record RENAME COLUMN longitude TO start_longitude;
ALTER TABLE checkin_record RENAME COLUMN face_image_url TO start_face_image_url;

-- 1.3 添加下岗相关字段
ALTER TABLE checkin_record ADD COLUMN end_time DATETIME NULL;
ALTER TABLE checkin_record ADD COLUMN end_latitude DOUBLE NULL;
ALTER TABLE checkin_record ADD COLUMN end_longitude DOUBLE NULL;
ALTER TABLE checkin_record ADD COLUMN end_face_image_url VARCHAR(500) NULL;

-- 1.4 添加冗余统计字段
ALTER TABLE checkin_record ADD COLUMN duration_minutes BIGINT NULL;
ALTER TABLE checkin_record ADD COLUMN spot_check_total INT DEFAULT 0;
ALTER TABLE checkin_record ADD COLUMN spot_check_passed INT DEFAULT 0;

-- 1.5 迁移旧状态到 LEGACY
-- 旧状态: SUCCESS, FAILED, PENDING -> 新状态: LEGACY
UPDATE checkin_record SET status = 'LEGACY' WHERE status IN ('SUCCESS', 'FAILED', 'PENDING');


-- 2. SpotCheck 表重构
-- --------------------------------------------------------

-- 2.1 添加 version 字段
ALTER TABLE spot_check ADD COLUMN version BIGINT DEFAULT 0;

-- 2.2 添加 checkin_record_id 外键（暂时允许 NULL，后续填充）
ALTER TABLE spot_check ADD COLUMN checkin_record_id BIGINT NULL;

-- 2.3 重命名 reason 为 fail_reason
ALTER TABLE spot_check RENAME COLUMN reason TO fail_reason;

-- 2.4 删除不再需要的字段
ALTER TABLE spot_check DROP COLUMN notification_sent;
ALTER TABLE spot_check DROP COLUMN scheduled_time;
ALTER TABLE spot_check DROP COLUMN triggered_by_admin_id;

-- 2.5 迁移旧状态
-- COMPLETED -> PASSED, FAILED/CANCELLED -> 删除或保留为 MISSED
UPDATE spot_check SET status = 'PASSED' WHERE status = 'COMPLETED';
UPDATE spot_check SET status = 'MISSED' WHERE status IN ('FAILED', 'CANCELLED');

-- 2.6 添加外键约束（注意：需要先填充 checkin_record_id）
-- 暂时注释掉，等数据迁移完成后再添加
-- ALTER TABLE spot_check ADD CONSTRAINT fk_spot_check_checkin_record
--     FOREIGN KEY (checkin_record_id) REFERENCES checkin_record(id);


-- 3. 索引优化
-- --------------------------------------------------------

-- 查询在岗保安
CREATE INDEX idx_checkin_record_status ON checkin_record(status);
CREATE INDEX idx_checkin_record_guard_status ON checkin_record(guard_id, status);

-- 按时间范围查询
CREATE INDEX idx_checkin_record_start_time ON checkin_record(start_time);

-- 抽查关联查询
CREATE INDEX idx_spot_check_checkin_record ON spot_check(checkin_record_id);
CREATE INDEX idx_spot_check_status ON spot_check(status);
