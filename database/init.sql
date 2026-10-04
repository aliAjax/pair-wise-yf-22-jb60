-- ============================================================
-- 制造业质量追溯 API 服务 - 数据库初始化
-- ============================================================

-- ---------- 脚手架既有表（保持兼容） ----------

CREATE TABLE IF NOT EXISTS work_order (
  id BIGINT PRIMARY KEY,
  order_no TEXT,
  product_code TEXT,
  product_name TEXT,
  planned_qty TEXT,
  line_code TEXT,
  start_at TEXT,
  status TEXT
);

CREATE TABLE IF NOT EXISTS product_batch (
  id BIGINT PRIMARY KEY,
  batch_no TEXT,
  work_order_id TEXT,
  quantity TEXT,
  material_lot_no TEXT,
  produced_at TEXT,
  batch_status TEXT
);

CREATE TABLE IF NOT EXISTS quality_inspection (
  id BIGINT PRIMARY KEY,
  batch_id TEXT,
  inspector_id TEXT,
  inspection_type TEXT,
  standard_version TEXT,
  result_status TEXT,
  inspected_at TEXT
);

CREATE TABLE IF NOT EXISTS inspection_item_result (
  id BIGINT PRIMARY KEY,
  inspection_id TEXT,
  item_code TEXT,
  item_name TEXT,
  measured_value TEXT,
  limit_min TEXT,
  limit_max TEXT,
  item_status TEXT
);

CREATE TABLE IF NOT EXISTS defect_record (
  id BIGINT PRIMARY KEY,
  batch_id TEXT,
  defect_type TEXT,
  defect_qty TEXT,
  severity TEXT,
  root_cause TEXT,
  disposition_status TEXT
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor TEXT,
  action TEXT,
  target_type TEXT,
  target_id TEXT,
  detail TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- 检验任务派发域 ----------

-- 检验员
CREATE TABLE IF NOT EXISTS inspector (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  employee_no TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'INSPECTOR',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 检验员资质：按检验类型持有，valid_from/valid_until 决定是否有效
CREATE TABLE IF NOT EXISTS inspector_qualification (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  inspector_id BIGINT NOT NULL REFERENCES inspector(id),
  qualification_no TEXT NOT NULL UNIQUE,
  inspection_type TEXT NOT NULL,
  valid_from DATE NOT NULL,
  valid_until DATE NOT NULL,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_qualification_inspector ON inspector_qualification(inspector_id);
CREATE INDEX IF NOT EXISTS idx_qualification_match
  ON inspector_qualification(inspection_type, status, valid_until);

-- 检验任务
CREATE TABLE IF NOT EXISTS inspection_task (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  task_no TEXT NOT NULL UNIQUE,
  batch_no TEXT NOT NULL,
  inspection_type TEXT NOT NULL,
  standard_version TEXT,
  -- PENDING_DISPATCH / DISPATCH_FAILED / CLAIMED / SUBMITTED
  status TEXT NOT NULL DEFAULT 'PENDING_DISPATCH',
  assignee_id BIGINT,
  claimed_at TIMESTAMPTZ,
  -- 领取时刻的资质快照：按批号追溯时即使资质后来过期/吊销，结论仍可还原“当时的资质”
  claimed_qualification_id BIGINT,
  claimed_qualification_no TEXT,
  claimed_qualification_valid_until DATE,
  dispatch_attempts INTEGER NOT NULL DEFAULT 0,
  last_dispatch_error TEXT,
  result_status TEXT,
  result_note TEXT,
  submitted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- 任务单号序列
CREATE SEQUENCE IF NOT EXISTS task_no_seq START 100;

CREATE INDEX IF NOT EXISTS idx_task_status ON inspection_task(status);
CREATE INDEX IF NOT EXISTS idx_task_batch ON inspection_task(batch_no);
CREATE INDEX IF NOT EXISTS idx_task_assignee ON inspection_task(assignee_id);

-- ---------- 种子数据 ----------

INSERT INTO inspector (employee_no, name, role) VALUES
  ('Q001', '张首检', 'INSPECTOR'),
  ('Q002', '李巡检', 'INSPECTOR'),
  ('Q003', '王终检', 'INSPECTOR'),
  ('Q004', '赵过期', 'INSPECTOR')
ON CONFLICT (employee_no) DO NOTHING;

-- Q001：首检资质有效；Q002：巡检资质有效；Q003：终检资质有效；Q004：资质已过期
INSERT INTO inspector_qualification
  (inspector_id, qualification_no, inspection_type, valid_from, valid_until, status)
VALUES
  ((SELECT id FROM inspector WHERE employee_no='Q001'), 'CERT-FIRST-001', 'FIRST_INSPECTION', DATE '2025-01-01', DATE '2027-12-31', 'ACTIVE'),
  ((SELECT id FROM inspector WHERE employee_no='Q002'), 'CERT-PATROL-002', 'PATROL_INSPECTION', DATE '2025-01-01', DATE '2027-12-31', 'ACTIVE'),
  ((SELECT id FROM inspector WHERE employee_no='Q003'), 'CERT-FINAL-003', 'FINAL_INSPECTION', DATE '2025-01-01', DATE '2027-12-31', 'ACTIVE'),
  ((SELECT id FROM inspector WHERE employee_no='Q004'), 'CERT-FIRST-004', 'FIRST_INSPECTION', DATE '2024-01-01', DATE '2025-06-30', 'ACTIVE')
ON CONFLICT (qualification_no) DO NOTHING;

INSERT INTO inspection_task
  (task_no, batch_no, inspection_type, standard_version, status)
VALUES
  ('TASK-20260901-01', 'B2026090101', 'FIRST_INSPECTION', 'STD-v3.1', 'PENDING_DISPATCH'),
  ('TASK-20260901-02', 'B2026090101', 'PATROL_INSPECTION', 'STD-v3.1', 'PENDING_DISPATCH'),
  ('TASK-20260901-03', 'B2026090101', 'FINAL_INSPECTION', 'STD-v3.1', 'PENDING_DISPATCH')
ON CONFLICT (task_no) DO NOTHING;

INSERT INTO product_batch (id, batch_no, work_order_id, quantity, material_lot_no, produced_at, batch_status)
SELECT 9001, 'B2026090101', 'WO-001', '1000', 'MAT-LOT-001', '2026-09-01', 'IN_PRODUCTION'
WHERE NOT EXISTS (SELECT 1 FROM product_batch WHERE batch_no='B2026090101');
