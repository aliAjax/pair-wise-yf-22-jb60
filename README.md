# 制造业质量追溯 API 服务

面向小型制造工厂的批次质量追溯后端服务，覆盖工单、批次、检验项、不良记录和追溯查询；
**检验任务按检验类型派发给资质有效的检验员，支持并发先到先得、资质过期复检、派发失败重试、越权拒绝，并可按批号追溯检验员及当时资质。**

## 快速启动

```bash
cp .env.example .env && docker compose up -d
```

启动后健康检查：<http://localhost:21114/health>

## 检验任务派发（核心业务）

把三类数据接起来：**检验任务（InspectionTask）→ 检验员资质（InspectorQualification）→ 批次检验结论（提交后不可变）**。

规则：

1. 任务有检验类型（首检 `FIRST_INSPECTION` / 巡检 `PATROL_INSPECTION` / 终检 `FINAL_INSPECTION`），只能派给持同类型**有效资质**（`status=ACTIVE` 且今天在 `valid_from~valid_until` 内）且在岗的检验员。
2. 两名检验员同时领取同一任务时，领取是**数据库行级条件更新**（`WHERE status='PENDING_DISPATCH' AND assignee_id IS NULL`），只有先提交的一人成功，后到者收到 `409 TASK_ALREADY_CLAIMED`。
3. 资质过期/吊销后，对在途任务做复检（也可由每日 02:17 的定时巡检自动执行）：
   - 已领取但**还没提交**的任务 → 清空领取人与资质快照，**退回待派**，重新确认接单人；
   - **已提交的结论照旧有效**，数据不动，只在追溯时标明该资质当前状态为 `EXPIRED/REVOKED`。
4. 派发时无人具备有效资质 → 任务落 `DISPATCH_FAILED`（记录尝试次数与原因，HTTP 422），事后补资质可重试。
5. 越权直接拒绝：无对应类型资质/资质过期/已停用的人领取 → `403 ILLEGAL_CLAIM`；非领取人提交 → `403 TASK_ASSIGNED_TO_OTHER`。
6. 领取瞬间把资质号、有效期**快照**到任务行；`GET /api/trace/{batchNo}` 按批号返回每条检验的检验员和“当时的资质”，以及该资质现在是否仍有效。

任务状态机：

```text
PENDING_DISPATCH ──派发/领取成功──▶ CLAIMED ──提交结论──▶ SUBMITTED（结论不可变）
       │                              │
       │无合格人选                    │资质过期（复检/提交时发现）
       ▼                              ▼
 DISPATCH_FAILED ──重试成功──▶ CLAIMED   （退回）PENDING_DISPATCH
```

### CLI 示例

所有写接口需带检验员身份头 `X-Inspector-Id`（种子检验员工号 Q001~Q004）。

```bash
# 查看检验员与资质（Q004 的首检资质已过期，用于演示拒绝）
curl -s http://localhost:21114/api/dispatch/inspectors | head

# 创建检验任务（默认创建后自动按检验类型派发）
curl -s -X POST http://localhost:21114/api/dispatch/tasks \
  -H 'Content-Type: application/json' -H 'X-Inspector-Id: Q001' \
  -d '{"batchNo":"B2026100401","inspectionType":"FIRST_INSPECTION","standardVersion":"STD-v3.1"}'

# 待派任务：检验员自行领取（并发时先到先得）
curl -s -X POST http://localhost:21114/api/dispatch/tasks/TASK-xxxx/claim -H 'X-Inspector-Id: Q001'

# 越权演示：Q003 只有终检资质，领取首检任务会被直接拒绝（403 ILLEGAL_CLAIM）
curl -s -X POST http://localhost:21114/api/dispatch/tasks/TASK-xxxx/claim -H 'X-Inspector-Id: Q003'

# 领取人提交检验结论
curl -s -X POST http://localhost:21114/api/dispatch/tasks/TASK-xxxx/submit \
  -H 'Content-Type: application/json' -H 'X-Inspector-Id: Q001' \
  -d '{"resultStatus":"PASS","resultNote":"合格"}'

# 派发失败后重试
curl -s -X POST http://localhost:21114/api/dispatch/tasks/TASK-xxxx/retry-dispatch -H 'X-Inspector-Id: Q001'

# 资质过期后按批号重新确认接单人（未提交退回待派，已提交保持有效）
curl -s -X POST http://localhost:21114/api/dispatch/batches/B2026100401/recheck-qualifications -H 'X-Inspector-Id: Q001'

# 按批号追溯：返回检验员和当时的资质
curl -s http://localhost:21114/api/trace/B2026100401
```

### 派发域接口清单

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/dispatch/tasks` | 创建检验任务（默认自动派发） |
| GET | `/api/dispatch/tasks?status=` | 任务列表，可按状态过滤（如 `DISPATCH_FAILED`） |
| GET | `/api/dispatch/tasks/{taskNo}` | 任务详情（领取人 + 资质快照） |
| POST | `/api/dispatch/tasks/{taskNo}/dispatch` | 系统派发 |
| POST | `/api/dispatch/tasks/{taskNo}/retry-dispatch` | 派发失败重试 |
| POST | `/api/dispatch/tasks/{taskNo}/claim` | 检验员领取（先到先得，需 `X-Inspector-Id`） |
| POST | `/api/dispatch/tasks/{taskNo}/submit` | 领取人提交结论（PASS/FAIL/CONDITIONAL_PASS/RECHECK） |
| POST | `/api/dispatch/batches/{batchNo}/recheck-qualifications` | 资质复检：未提交退回、已提交保持 |
| GET | `/api/dispatch/inspectors` | 检验员及其资质有效性 |
| GET | `/api/trace/{batchNo}` | 按批号追溯检验员与当时资质 |

## 访问地址或 CLI 示例

- 后端健康检查：<http://localhost:21114/health>
- 接口统一挂在 `/api` 下，派发写接口需 `X-Inspector-Id` 请求头。

## 本地开发方式

- 需要 JDK 17、Maven 3.9+、PostgreSQL 15。
- 运行测试（使用 H2 内存库，无需外部数据库）：`mvn -f backend/pom.xml test`，含并发领取、越权拒绝、资质过期退回、追溯等 11 个用例。
- 本地起服务：先建库并执行 `database/init.sql`，再 `mvn -f backend/pom.xml spring-boot:run`，用环境变量 `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD` 覆盖连接。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | - |
| 后端 | Spring Boot 3 + Java 17 + Spring Data JPA |
| 数据库 | PostgreSQL 15（测试用 H2 PostgreSQL 兼容模式） |
| 部署 | Docker Compose |

## 项目目录结构

```text
backend/src/main/java/com/generated/qualityTrace/
├── dispatch/                     # 检验任务派发域（新增）
│   ├── constants/                # InspectionTaskStatus / InspectionType / QualificationStatus
│   │                             # DispatchErrorCodes / DispatchErrorMessages / DispatchLogTemplates
│   ├── controllers/              # InspectionTaskController / BatchTraceController / InspectorController
│   ├── services/                 # InspectionTaskService / InspectorQualificationService
│   │                             # QualificationRecheckService / BatchTraceService / DispatchAuditLogService
│   ├── models/                   # InspectionTask / Inspector / InspectorQualification / DispatchAuditLog
│   ├── repositories/             # JPA Repository：条件 UPDATE 保证原子领取/派发/退回
│   ├── middlewares/              # CallerInterceptor(认证) / ErrorHandlerDispatchMiddleware(异常)
│   │                             # QualificationSweepMiddleware(资质定时巡检) / CallerContext
│   ├── routes/                   # DispatchRoutes 路由常量
│   ├── constructors/             # InspectionTaskViewFactory / InspectorViewFactory / BatchTraceViewFactory
│   ├── validators/               # DispatchValidator 入参与枚举校验
│   ├── types/                    # 请求/响应 Payload 与追溯视图
│   ├── exceptions/               # DispatchBusinessException（携带错误码与 HTTP 状态）
│   └── config/                   # DispatchWebConfig 注册拦截器
├── routes, controllers, services, models, repositories/  # 脚手架既有实体（内存桩）
├── middlewares, constants, constructors, utils, types, config
backend/src/test/                 # H2 集成测试 + HTTP 冒烟测试
database/init.sql                 # 建表 + 索引 + 种子检验员/资质/任务
```

## 环境变量说明

- `COMPOSE_PROJECT_NAME`: Compose 项目名，默认 `quality-trace`
- `BACKEND_PORT`: 后端宿主机端口，默认 `21114`
- `DB_PORT`: 数据库宿主机端口，默认 `54320`
- `DB_USER/DB_PASSWORD/DB_NAME`: 数据库凭据
- 容器内另有 `DB_HOST=db`、`PORT=8080`、`JWT_SECRET`。

## Docker 部署说明

- 根 Compose 文件不写 `version`，顶层 `name: quality-trace`。
- 容器名均使用 `${COMPOSE_PROJECT_NAME:-quality-trace}` 前缀。
- 数据库使用命名卷 `db_data`，不绑定挂载中文路径。
- 数据库配置 healthcheck，后端通过 `depends_on: condition: service_healthy` 等待数据库就绪。
- 常见问题：端口占用时修改 `.env` 中端口后重启；需要重置数据时执行 `docker compose down -v`。

## 枚举/常量出现位置清单

- **WorkOrderStatus**（PLANNED/RUNNING/PAUSED/FINISHED/CANCELLED）：`constants/WorkOrderStatus.java`、`models/WorkOrder.java`、`constructors/WorkOrderDtoFactory.java`、`constants/LogTemplates.java`、`constants/ErrorMessages.java`、`services/WorkOrderService.java`、`controllers/WorkOrderController.java`、`types/WorkOrderPayload.java`。
- **InspectionResultStatus**（PASS/FAIL/CONDITIONAL_PASS/RECHECK）：`constants/InspectionResultStatus.java`、`dispatch/validators/DispatchValidator.java`（提交结论校验）、`dispatch/types/SubmitInspectionPayload.java`（`@Pattern`）、`dispatch/models/InspectionTask.java`（`result_status` 列）、`dispatch/constants/DispatchErrorMessages.java`、`dispatch/constants/DispatchLogTemplates.java`（提交日志）、`dispatch/constructors/BatchTraceViewFactory.java`（追溯展示）、`database/init.sql`（列注释/种子）。
- **DefectSeverity**（MINOR/MAJOR/CRITICAL）：`constants/DefectSeverity.java`、`models/DefectRecord.java`、`constructors/DefectRecordDtoFactory.java`、`constants/LogTemplates.java`、`constants/ErrorMessages.java`、`services/DefectRecordService.java`、`controllers/DefectRecordController.java`、`types/DefectRecordPayload.java`。
- **InspectionTaskStatus**（PENDING_DISPATCH/DISPATCH_FAILED/CLAIMED/SUBMITTED，派发域新增）：`dispatch/constants/InspectionTaskStatus.java`、`dispatch/models/InspectionTask.java`、`dispatch/repositories/InspectionTaskRepository.java`（全部条件 UPDATE）、`dispatch/services/*.java`、`dispatch/controllers/InspectionTaskController.java`、`dispatch/types/InspectionTaskView.java`、`dispatch/middlewares/QualificationSweepMiddleware.java`、`database/init.sql`。
- **InspectionType / QualificationStatus**（派发域新增）：`dispatch/constants/`、资质/任务 model、`InspectorQualificationRepository` 查询条件、`DispatchValidator`、`CreateInspectionTaskPayload`、`init.sql` 种子数据。

## 为什么会牵一发动全身

- 任务、资质、检验员、审计日志被拆成独立 model/repository/service/controller/route，改一条状态规则至少跨 3~5 个文件。
- 错误码与错误消息分文件（`DispatchErrorCodes` / `DispatchErrorMessages`），service 与 controller 各自包装异常，最终由 `ErrorHandlerDispatchMiddleware` 统一映射 HTTP 状态。
- 日志模板集中在 `DispatchLogTemplates`，所有写操作（创建/派发/领取/并发落败/越权拒绝/退回/提交/复检/追溯）都要同步写应用日志与 `audit_log` 表。
- 响应结构只能由 `constructors` 下的 Factory 构造，字段调整会同时波及视图类型、工厂、追溯树与 README。
- 领取/派发/退回的正确性依赖 `repositories` 里的条件 UPDATE SQL，改状态枚举必须同步改 SQL 字面量、init.sql 与测试。

## License

MIT
