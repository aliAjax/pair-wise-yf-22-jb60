-- H2(PostgreSQL 模式) 测试 schema：与 database/init.sql 保持一致

CREATE TABLE IF NOT EXISTS work_order (
  id BIGINT PRIMARY KEY, order_no TEXT, product_code TEXT, product_name TEXT,
  planned_qty TEXT, line_code TEXT, start_at TEXT, status TEXT
);
CREATE TABLE IF NOT EXISTS product_batch (
  id BIGINT PRIMARY KEY, batch_no TEXT, work_order_id TEXT, quantity TEXT,
  material_lot_no TEXT, produced_at TEXT, batch_status TEXT
);
CREATE TABLE IF NOT EXISTS quality_inspection (
  id BIGINT PRIMARY KEY, batch_id TEXT, inspector_id TEXT, inspection_type TEXT,
  standard_version TEXT, result_status TEXT, inspected_at TEXT
);
CREATE TABLE IF NOT EXISTS inspection_item_result (
  id BIGINT PRIMARY KEY, inspection_id TEXT, item_code TEXT, item_name TEXT,
  measured_value TEXT, limit_min TEXT, limit_max TEXT, item_status TEXT
);
CREATE TABLE IF NOT EXISTS defect_record (
  id BIGINT PRIMARY KEY, batch_id TEXT, defect_type TEXT, defect_qty TEXT,
  severity TEXT, root_cause TEXT, disposition_status TEXT
);
CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor TEXT, action TEXT, target_type TEXT, target_id TEXT, detail TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS inspector (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  employee_no TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'INSPECTOR',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS inspector_qualification (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  inspector_id BIGINT NOT NULL,
  qualification_no TEXT NOT NULL UNIQUE,
  inspection_type TEXT NOT NULL,
  valid_from DATE NOT NULL,
  valid_until DATE NOT NULL,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE SEQUENCE IF NOT EXISTS task_no_seq START WITH 100;

CREATE TABLE IF NOT EXISTS inspection_task (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  task_no TEXT NOT NULL UNIQUE,
  batch_no TEXT NOT NULL,
  inspection_type TEXT NOT NULL,
  standard_version TEXT,
  status TEXT NOT NULL DEFAULT 'PENDING_DISPATCH',
  assignee_id BIGINT,
  claimed_at TIMESTAMP WITH TIME ZONE,
  claimed_qualification_id BIGINT,
  claimed_qualification_no TEXT,
  claimed_qualification_valid_until DATE,
  dispatch_attempts INTEGER NOT NULL DEFAULT 0,
  last_dispatch_error TEXT,
  result_status TEXT,
  result_note TEXT,
  submitted_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
