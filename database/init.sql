-- ============================================================
-- quality-trace 数据库初始化脚本（PostgreSQL 15）
-- 由 docker-entrypoint 在首次启动时执行；幂等，可重复执行。
-- ============================================================

-- ---------- 基础业务表 ----------
CREATE TABLE IF NOT EXISTS work_order (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  order_no TEXT,
  product_code TEXT,
  product_name TEXT,
  planned_qty TEXT,
  line_code TEXT,
  start_at TEXT,
  status TEXT
);

CREATE TABLE IF NOT EXISTS product_batch (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  batch_no TEXT,
  work_order_id BIGINT,
  quantity TEXT,
  material_lot_no TEXT,
  produced_at TEXT,
  batch_status TEXT
);

CREATE TABLE IF NOT EXISTS quality_inspection (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  batch_id BIGINT,
  task_id BIGINT,
  inspector_id BIGINT,
  inspector_name TEXT,
  inspection_type TEXT,
  standard_version TEXT,
  result_status TEXT,
  qualification_id BIGINT,
  qualification_snapshot TEXT,
  inspected_at TEXT
);

CREATE TABLE IF NOT EXISTS inspection_item_result (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  inspection_id BIGINT,
  item_code TEXT,
  item_name TEXT,
  measured_value TEXT,
  limit_min TEXT,
  limit_max TEXT,
  item_status TEXT
);

CREATE TABLE IF NOT EXISTS defect_record (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  batch_id BIGINT,
  defect_type TEXT,
  defect_qty TEXT,
  severity TEXT,
  root_cause TEXT,
  disposition_status TEXT
);

-- ---------- 用户 / 检验员 ----------
CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  username TEXT NOT NULL UNIQUE,
  display_name TEXT,
  role TEXT,
  status TEXT,
  created_at TEXT
);

-- ---------- 检验员资质 ----------
CREATE TABLE IF NOT EXISTS inspector_qualification (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  inspector_id BIGINT NOT NULL,
  inspection_type TEXT NOT NULL,
  qualified_from TEXT,
  qualified_until TEXT,
  status TEXT,
  version INTEGER DEFAULT 0,
  granted_by TEXT,
  created_at TEXT,
  updated_at TEXT,
  CONSTRAINT uq_inspector_type UNIQUE (inspector_id, inspection_type)
);

-- ---------- 检验任务 ----------
CREATE TABLE IF NOT EXISTS inspection_task (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  task_no TEXT NOT NULL UNIQUE,
  batch_id BIGINT NOT NULL,
  inspection_type TEXT NOT NULL,
  status TEXT NOT NULL,
  assignee_id BIGINT,
  dispatch_attempts INTEGER DEFAULT 0,
  last_dispatch_error TEXT,
  dispatched_at TEXT,
  claimed_at TEXT,
  submitted_at TEXT,
  version INTEGER DEFAULT 0,
  created_at TEXT,
  updated_at TEXT
);

-- ---------- 审计日志 ----------
CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor TEXT,
  action TEXT,
  target_type TEXT,
  target_id TEXT,
  created_at TEXT
);

-- ============================================================
-- 种子数据（幂等）
-- ============================================================

-- 用户：2 名检验员（1 名资质有效、1 名资质过期）、主管、经理、审计员
INSERT INTO app_user (username, display_name, role, status, created_at)
SELECT 'inspector.zhang', '张质检', 'INSPECTOR', 'ACTIVE', '2026-01-01 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'inspector.zhang');
INSERT INTO app_user (username, display_name, role, status, created_at)
SELECT 'inspector.li', '李质检', 'INSPECTOR', 'ACTIVE', '2026-01-01 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'inspector.li');
INSERT INTO app_user (username, display_name, role, status, created_at)
SELECT 'supervisor.wang', '王主管', 'SUPERVISOR', 'ACTIVE', '2026-01-01 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'supervisor.wang');
INSERT INTO app_user (username, display_name, role, status, created_at)
SELECT 'manager.zhao', '赵经理', 'MANAGER', 'ACTIVE', '2026-01-01 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'manager.zhao');
INSERT INTO app_user (username, display_name, role, status, created_at)
SELECT 'auditor.sun', '孙审计', 'AUDITOR', 'ACTIVE', '2026-01-01 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'auditor.sun');

-- 资质：张质检 首检+终检有效；李质检 巡检已过期
INSERT INTO inspector_qualification
  (inspector_id, inspection_type, qualified_from, qualified_until, status, version, granted_by, created_at, updated_at)
SELECT u.id, 'FIRST_INSPECTION', '2026-01-01', '2026-12-31', 'VALID', 0, 'manager.zhao', '2026-01-01 09:00:00', '2026-01-01 09:00:00'
FROM app_user u WHERE u.username = 'inspector.zhang'
  AND NOT EXISTS (SELECT 1 FROM inspector_qualification q WHERE q.inspector_id = u.id AND q.inspection_type = 'FIRST_INSPECTION');
INSERT INTO inspector_qualification
  (inspector_id, inspection_type, qualified_from, qualified_until, status, version, granted_by, created_at, updated_at)
SELECT u.id, 'FINAL_INSPECTION', '2026-01-01', '2026-12-31', 'VALID', 0, 'manager.zhao', '2026-01-01 09:00:00', '2026-01-01 09:00:00'
FROM app_user u WHERE u.username = 'inspector.zhang'
  AND NOT EXISTS (SELECT 1 FROM inspector_qualification q WHERE q.inspector_id = u.id AND q.inspection_type = 'FINAL_INSPECTION');
INSERT INTO inspector_qualification
  (inspector_id, inspection_type, qualified_from, qualified_until, status, version, granted_by, created_at, updated_at)
SELECT u.id, 'PATROL_INSPECTION', '2025-01-01', '2026-01-01', 'VALID', 0, 'manager.zhao', '2025-01-01 09:00:00', '2025-01-01 09:00:00'
FROM app_user u WHERE u.username = 'inspector.li'
  AND NOT EXISTS (SELECT 1 FROM inspector_qualification q WHERE q.inspector_id = u.id AND q.inspection_type = 'PATROL_INSPECTION');

-- 工单 + 批次
INSERT INTO work_order (order_no, product_code, product_name, planned_qty, line_code, start_at, status)
SELECT 'WO-2026-0001', 'P-1001', '传动轴', '1000', 'L-01', '2026-10-01 08:00:00', 'RUNNING'
WHERE NOT EXISTS (SELECT 1 FROM work_order WHERE order_no = 'WO-2026-0001');
INSERT INTO product_batch (batch_no, work_order_id, quantity, material_lot_no, produced_at, batch_status)
SELECT 'BATCH-20261004-001', w.id, '100', 'LOT-20260928', '2026-10-04 10:00:00', 'PRODUCED'
FROM work_order w WHERE w.order_no = 'WO-2026-0001'
  AND NOT EXISTS (SELECT 1 FROM product_batch WHERE batch_no = 'BATCH-20261004-001');

-- 一张待派的首检任务
INSERT INTO inspection_task
  (task_no, batch_id, inspection_type, status, assignee_id, dispatch_attempts, created_at, updated_at)
SELECT 'TASK-20261004-0001', b.id, 'FIRST_INSPECTION', 'PENDING', NULL, 0, '2026-10-04 10:05:00', '2026-10-04 10:05:00'
FROM product_batch b WHERE b.batch_no = 'BATCH-20261004-001'
  AND NOT EXISTS (SELECT 1 FROM inspection_task WHERE task_no = 'TASK-20261004-0001');

-- 重置标识列（identity）到当前最大值，避免后续手工插入冲突
SELECT setval(pg_get_serial_sequence('app_user', 'id'), (SELECT COALESCE(MAX(id), 1) FROM app_user));
SELECT setval(pg_get_serial_sequence('inspector_qualification', 'id'), (SELECT COALESCE(MAX(id), 1) FROM inspector_qualification));
SELECT setval(pg_get_serial_sequence('inspection_task', 'id'), (SELECT COALESCE(MAX(id), 1) FROM inspection_task));
SELECT setval(pg_get_serial_sequence('quality_inspection', 'id'), (SELECT COALESCE(MAX(id), 1) FROM quality_inspection));
SELECT setval(pg_get_serial_sequence('product_batch', 'id'), (SELECT COALESCE(MAX(id), 1) FROM product_batch));
SELECT setval(pg_get_serial_sequence('work_order', 'id'), (SELECT COALESCE(MAX(id), 1) FROM work_order));
