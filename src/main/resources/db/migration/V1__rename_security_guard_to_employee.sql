-- 数据库迁移脚本：将 security_guard 表重构为 employee 表
-- 执行时机：部署新代码前执行

-- 1. 重命名表
ALTER TABLE security_guard RENAME TO employee;

-- 2. 添加员工类型列（discriminator）
ALTER TABLE employee ADD COLUMN employee_type VARCHAR(31) NOT NULL DEFAULT 'GUARD';

-- 3. 更新现有数据（确保所有现有记录标记为 GUARD）
UPDATE employee SET employee_type = 'GUARD' WHERE employee_type IS NULL OR employee_type = '';

-- 4. 添加索引以优化按类型查询
CREATE INDEX idx_employee_type ON employee(employee_type);

-- 5. 添加身份证号列
ALTER TABLE employee ADD COLUMN id_card_number VARCHAR(18) UNIQUE;

-- 6. 添加在职状态列（默认为 ACTIVE）
ALTER TABLE employee ADD COLUMN employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- 7. 添加性别列
ALTER TABLE employee ADD COLUMN gender VARCHAR(10);

-- 8. 添加入职/离职日期列
ALTER TABLE employee ADD COLUMN original_hire_date DATE;
ALTER TABLE employee ADD COLUMN latest_hire_date DATE;
ALTER TABLE employee ADD COLUMN resign_date DATE;

-- 注意：checkin_record 表的 guard_id 外键会自动跟随表名变化
-- 如果遇到外键问题，可能需要先删除再重建外键约束