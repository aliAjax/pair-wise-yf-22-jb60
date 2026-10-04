-- H2 测试种子数据（不写 id，由 identity 生成）
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

-- 张质检：首检+终检有效；李质检：巡检已过期
INSERT INTO inspector_qualification
  (inspector_id, inspection_type, qualified_from, qualified_until, status, version, granted_by, created_at, updated_at)
SELECT u.id, 'FIRST_INSPECTION', '2026-01-01', '2099-12-31', 'VALID', 0, 'manager.zhao', '2026-01-01 09:00:00', '2026-01-01 09:00:00'
FROM app_user u WHERE u.username = 'inspector.zhang'
  AND NOT EXISTS (SELECT 1 FROM inspector_qualification q WHERE q.inspector_id = u.id AND q.inspection_type = 'FIRST_INSPECTION');
INSERT INTO inspector_qualification
  (inspector_id, inspection_type, qualified_from, qualified_until, status, version, granted_by, created_at, updated_at)
SELECT u.id, 'FINAL_INSPECTION', '2026-01-01', '2099-12-31', 'VALID', 0, 'manager.zhao', '2026-01-01 09:00:00', '2026-01-01 09:00:00'
FROM app_user u WHERE u.username = 'inspector.zhang'
  AND NOT EXISTS (SELECT 1 FROM inspector_qualification q WHERE q.inspector_id = u.id AND q.inspection_type = 'FINAL_INSPECTION');
INSERT INTO inspector_qualification
  (inspector_id, inspection_type, qualified_from, qualified_until, status, version, granted_by, created_at, updated_at)
SELECT u.id, 'PATROL_INSPECTION', '2025-01-01', '2026-01-01', 'VALID', 0, 'manager.zhao', '2025-01-01 09:00:00', '2025-01-01 09:00:00'
FROM app_user u WHERE u.username = 'inspector.li'
  AND NOT EXISTS (SELECT 1 FROM inspector_qualification q WHERE q.inspector_id = u.id AND q.inspection_type = 'PATROL_INSPECTION');

INSERT INTO work_order (order_no, product_code, product_name, planned_qty, line_code, start_at, status)
SELECT 'WO-2026-0001', 'P-1001', '传动轴', '1000', 'L-01', '2026-10-01 08:00:00', 'RUNNING'
WHERE NOT EXISTS (SELECT 1 FROM work_order WHERE order_no = 'WO-2026-0001');
INSERT INTO product_batch (batch_no, work_order_id, quantity, material_lot_no, produced_at, batch_status)
SELECT 'BATCH-20261004-001', w.id, '100', 'LOT-20260928', '2026-10-04 10:00:00', 'PRODUCED'
FROM work_order w WHERE w.order_no = 'WO-2026-0001'
  AND NOT EXISTS (SELECT 1 FROM product_batch WHERE batch_no = 'BATCH-20261004-001');
