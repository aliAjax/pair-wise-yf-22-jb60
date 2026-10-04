-- H2（PostgreSQL 兼容模式）测试 schema
CREATE TABLE IF NOT EXISTS work_order (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  order_no VARCHAR(100),
  product_code VARCHAR(100),
  product_name VARCHAR(200),
  planned_qty VARCHAR(50),
  line_code VARCHAR(50),
  start_at VARCHAR(50),
  status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS product_batch (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  batch_no VARCHAR(100),
  work_order_id BIGINT,
  quantity VARCHAR(50),
  material_lot_no VARCHAR(100),
  produced_at VARCHAR(50),
  batch_status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS quality_inspection (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  batch_id BIGINT,
  task_id BIGINT,
  inspector_id BIGINT,
  inspector_name VARCHAR(100),
  inspection_type VARCHAR(50),
  standard_version VARCHAR(50),
  result_status VARCHAR(50),
  qualification_id BIGINT,
  qualification_snapshot VARCHAR(500),
  inspected_at VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS inspection_item_result (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  inspection_id BIGINT,
  item_code VARCHAR(100),
  item_name VARCHAR(200),
  measured_value VARCHAR(50),
  limit_min VARCHAR(50),
  limit_max VARCHAR(50),
  item_status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS defect_record (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  batch_id BIGINT,
  defect_type VARCHAR(100),
  defect_qty VARCHAR(50),
  severity VARCHAR(50),
  root_cause VARCHAR(200),
  disposition_status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  display_name VARCHAR(100),
  role VARCHAR(50),
  status VARCHAR(50),
  created_at VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS inspector_qualification (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  inspector_id BIGINT NOT NULL,
  inspection_type VARCHAR(50) NOT NULL,
  qualified_from VARCHAR(50),
  qualified_until VARCHAR(50),
  status VARCHAR(20),
  version INTEGER DEFAULT 0,
  granted_by VARCHAR(100),
  created_at VARCHAR(50),
  updated_at VARCHAR(50),
  CONSTRAINT uq_inspector_type UNIQUE (inspector_id, inspection_type)
);

CREATE TABLE IF NOT EXISTS inspection_task (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  task_no VARCHAR(50) NOT NULL UNIQUE,
  batch_id BIGINT NOT NULL,
  inspection_type VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL,
  assignee_id BIGINT,
  dispatch_attempts INTEGER DEFAULT 0,
  last_dispatch_error VARCHAR(500),
  dispatched_at VARCHAR(50),
  claimed_at VARCHAR(50),
  submitted_at VARCHAR(50),
  version INTEGER DEFAULT 0,
  created_at VARCHAR(50),
  updated_at VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor VARCHAR(100),
  action VARCHAR(200),
  target_type VARCHAR(50),
  target_id VARCHAR(100),
  created_at VARCHAR(50)
);
