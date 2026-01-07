-- ============================================================
-- V3: 添加签到地点表（多地点支持）
-- 每个单位可配置多个签到地点，各有独立坐标和半径
-- ============================================================

-- 1. 创建签到地点表
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS checkin_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '地点名称（如：东门、西门、停车场）',
    latitude DOUBLE NOT NULL COMMENT '纬度',
    longitude DOUBLE NOT NULL COMMENT '经度',
    allowed_radius DOUBLE NOT NULL DEFAULT 100 COMMENT '允许签到半径（米）',
    site_id BIGINT NOT NULL COMMENT '所属单位ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_location_site FOREIGN KEY (site_id) REFERENCES work_site(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到地点表';

-- 2. 添加索引
-- --------------------------------------------------------

CREATE INDEX idx_checkin_location_site ON checkin_location(site_id);

-- 3. 迁移现有数据
-- 为每个现有单位创建一个默认签到地点（使用 WorkSite 原有坐标）
-- --------------------------------------------------------

INSERT INTO checkin_location (name, latitude, longitude, allowed_radius, site_id)
SELECT
    '默认签到点' as name,
    latitude,
    longitude,
    allowed_radius_meters as allowed_radius,
    id as site_id
FROM work_site
WHERE latitude IS NOT NULL
  AND longitude IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM checkin_location cl WHERE cl.site_id = work_site.id
  );
