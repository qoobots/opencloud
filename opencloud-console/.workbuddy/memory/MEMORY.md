# MEMORY.md — opencloud-console 项目长期记忆

## 项目概况

- **项目名称**：opencloud-console
- **技术栈**：Spring Boot 3.2.x，MyBatis-Plus，PostgreSQL，Spring WebFlux（WebClient），Spring WebSocket，JWT
- **模块结构**：opencloud-common / opencloud-auth / opencloud-system / opencloud-monitor / opencloud-cloud / opencloud-web / opencloud-ui
- **包名根路径**：`com.qoobot.opencloud`
- **数据库**：PostgreSQL，所有表在 public schema，使用 BIGSERIAL 主键

## opencloud-monitor 模块

### 状态：✅ Java 代码全部完成（2026-03-31）

### 已完成内容
1. **指标查询**：PrometheusClient（WebClient）+ MetricsController/Service
2. **Grafana 集成**：GrafanaClient + GrafanaController/Service
3. **告警规则**：AlertRule CRUD + PromQL 语法校验 + AlertRuleMapper（XML）
4. **告警记录**：AlertRecord CRUD + ACK 状态流转 + AlertRecordMapper（XML，含 PostgreSQL FILTER 统计聚合）
5. **AlertManager Webhook**：幂等写入 + 静默检查 + 异步通知分发
6. **WebSocket 推送**：TextWebSocketHandler + HandshakeInterceptor JWT 认证 + 心跳任务
7. **通知渠道**：多类型（EMAIL/DINGTALK/WECHAT_WORK/WEBHOOK）+ AES-256-GCM 加密存储 + 脱敏返回
8. **通知发送器**：Strategy Pattern，4 种实现（Email/DingTalk/WeChatWork/Webhook）
9. **异步分发**：NotifyDispatchService，@Async + 指数退避重试 3 次 + 写 monitor_notify_log
10. **告警静默**：AlertSilence CRUD + isAlertSilenced(labels) 多条件 AND 匹配
11. **通知日志**：NotifyLogController 分页查询
12. **MyBatis XML**：AlertRuleMapper.xml / AlertRecordMapper.xml / AlertSilenceMapper.xml / NotifyChannelMapper.xml / NotifyLogMapper.xml

### 关键设计决策
- WebSocket JWT：使用 `MonitorJwtUtil`（封装 common JwtUtil），tenantId 约定存在 JWT claim "tenantId"
- AlertSilenceMapper.expireOutdated：@Select 注解写 UPDATE，无需 XML
- 通知重试：手写指数退避循环（1s/2s/4s），不引入 Spring Retry
- AlertRuleMapper.countByChannelId：LIKE '%channelId%' 模糊匹配，待优化为 PostgreSQL JSONB `@>` 操作符

### 待办（低优先级）
- AlertManager 规则同步（创建/修改规则后同步推送至 AlertManager API）
- AlertRecordDetailVO.notifyLogs 联查实现
- 多节点 WebSocket 迁移至 Redis Pub/Sub
- AlertRuleMapper.countByChannelId 改 JSONB 操作符

## opencloud-cloud 模块

### 状态：✅ v1.1.0 完善功能版（2026-03-31）

### 已完成内容
1. **集群管理**：CRUD + 连接测试（OpenStack/Ceph/K8s）
2. **OpenStack Nova**：云主机/规格/密钥对管理（14 个接口）
3. **OpenStack Neutron**：网络/子网/安全组/浮动IP管理（4 个接口）
4. **OpenStack Glance**：镜像管理（3 个接口，含上传）
5. **OpenStack Cinder**：卷/快照管理（5 个接口）
6. **Ceph RGW**：Bucket/Object CRUD（S3 兼容，6 个接口）
7. **Ceph MGR**：集群总览/存储池/OSD 状态（4 个接口）
8. **Kubernetes**：Deployment/Pod/Service/ConfigMap/Secret/Node 管理（13 个接口，含日志流）
9. **配额管理**：配额查询/设置/检查
10. **异步任务**：任务提交/状态查询/缓存
11. **定时同步**：集群状态同步 + 资源使用量同步

### 新增文件（2026-03-31）
- `KubernetesServiceImpl.java`：KubernetesService 实现类
- `KubernetesController.java`：Kubernetes 资源管理控制器
- `CloudSyncJob.java`：定时同步任务

### 关键设计决策
- 客户端工厂：`CloudClientFactory` 按 clusterId 缓存客户端实例（ConcurrentHashMap）
- OpenStack：使用 openstack4j SDK，Token 自动刷新
- Ceph RGW：使用 AWS SDK（S3 兼容）
- Kubernetes：使用 fabric8 kubernetes-client
- 配额检查：实时查询 OpenStack 计算资源使用量
- 定时任务：Spring @Scheduled 每小时同步集群状态

### 接口统计
- OpenStack：15/15（全部完成）
- Ceph：8/8（全部完成）
- Kubernetes：9/9（全部完成）
- 配额/任务：3/3
- **总计**：64/64 接口完成

## opencloud-auth 模块
- 已完成，作为参考模块（含完整文档 02~07）

## 文档目录约定
每个模块的 `01_docs/` 下有：
- `01_开发进度.md`
- `02_业务设计.md`
- `03_应用设计.md`
- `04_数据设计.md`
- `05_技术设计.md`
- `06_数据库脚本.sql`
- `07_API接口.yml`

## 通用工具类（opencloud-common）
- `TenantContext`：ThreadLocal 租户 ID
- `SecurityUtils.getCurrentUserId()`：获取当前登录用户 ID（字符串）
- `JwtUtil`：JWT 生成/解析/校验（@Component，依赖 @Value opencloud.jwt.secret）
- `R<T>`：统一响应封装

## 待完成的大任务

- `opencloud-console/01_docs` 整体设计文档（02_业务设计.md ~ 07_API接口.yml）✅ 已完成
- `opencloud-cloud/01_docs` 模块设计文档 ✅ 已完成（2026-03-31）
- `opencloud-cloud` 基础代码开发 ✅ 已完成（2026-03-31，64 个接口全部完成）
- `opencloud-web` 模块设计文档 + 代码开发 ✅ **全部完成（2026-03-31）**
  - 设计文档：02~07 全部完成
  - Java 源码：启动类 + 6 个配置类 + TraceIdFilter
  - 配置文件：application.yml（dev/prod/test 三环境）+ logback-spring.xml
  - Flyway 迁移脚本：V1.0.0 / V1.1.0 / V1.2.0 三个版本
  - 部署文件：Dockerfile（多阶段 Maven + JRE-Alpine）+ .env.example
- `opencloud-console/01_docs` 总体设计文档现状：已于 2026-04-01 完成与各模块文档的差异分析；其中 `01_总体开发进度.md` 状态落后于实际完成度，`02~05` 对 auth/system/cloud/monitor/web 的业务流程、应用组件、数据结构、技术实现仍需按模块文档补齐。


## 项目整体完成状态（截止 2026-03-31）

| 模块 | 设计文档 | Java 代码 | 状态 |
|------|---------|-----------|------|
| opencloud-common | ✅ | ✅ | 完成 |
| opencloud-auth | ✅ | ✅ | 完成 |
| opencloud-system | ✅ | ✅ | 完成 |
| opencloud-monitor | ✅ | ✅ | 完成 |
| opencloud-cloud | ✅ | ✅ | 完成 |
| opencloud-web | ✅ | ✅ | 完成 |
| opencloud-ui | ✅ | — | 设计文档完成，代码开发中 |

## 总体设计文档（2026-04-01 新增）

- `01_docs/06_总体数据库脚本.sql` - 合并了 auth/system/cloud/monitor 四个模块的数据库表，包含完整的 Schema、枚举类型、表结构、索引、触发器、初始化数据和运维视图
- `01_docs/07_总体API接口.yml` - 合并了五个模块的 API 接口，以 OpenAPI 3.0.3 格式呈现，包含所有标签分类和接口定义

## opencloud-ui 前端开发进度（2026-04-01 更新）

### 状态：✅ **100% 全部完成**

### 全部已完成（2026-04-01 两轮开发）
- **P0 基础层**：API 层（4 模块，含 AlertSilence 接口）、StatusBadge/MetricCard/PermissionButton、v-permission 指令、useTable/useWebSocket Hook、alert/user Pinia Store
- **动态路由**：router/index.ts 完整重写，`menuToRoutes()` 将后端菜单树转路由，user.ts 的 `loadDynamicRoutes()` 登录后动态注册，失败降级静态路由
- **P1 系统管理**：UsersView / RolesView（权限树）/ MenusView（TreeSelect）/ OpLogsView / **LoginLogsView** / **ProfileView** / **PasswordView**（密码强度条）
- **P2 OpenStack**：InstancesView（3步向导+卡片/表格切换）/ NetworksView（4 Tab）/ ImagesView（上传）/ VolumesView（挂载/快照）
- **P3 Ceph**：CephOverviewView（OSD矩阵+ECharts）/ PoolsView（ECharts柱图）/ VolumesView（Bucket浏览器）
- **P4 K8s**：WorkloadsView（Deployment+Pod+日志）/ ServicesView / ConfigsView / NodesView
- **P5 监控告警**：GrafanaDashboard（iframe）/ AlertListView（WebSocket）/ AlertRulesView / NotifyChannelView / **MetricExploreView**（PromQL+12模板+ECharts）/ **AlertSilenceView**（标签编辑器+时间快捷选项）
- **Dashboard**：接入 Prometheus range_query（CPU/内存趋势面积图）+ 告警统计接口 + 组件在线状态（up{} 指标），60s 自动刷新，mock 数据降级兜底
- **错误页**：NotFoundView（404）/ **ForbiddenView**（403）

### 完成度
**56 / 56 功能，100% 完成**

---

## 前端设计文档完善（2026-04-01）

- `opencloud-ui/01_docs/02_业务设计.md` - 补充了与总体业务设计的对应关系（前端在各业务领域的定位、业务流程中的前端角色、业务规则补充）
- `opencloud-ui/01_docs/03_应用设计.md` - 补充了前后端集成方案（API 对接规范、请求/响应拦截器、Vite 代理配置、Docker 部署配置、与总体应用设计对应关系）
- `opencloud-ui/01_docs/04_数据设计.md` - 补充了与总体数据设计的对应关系（前端数据流转架构、后端数据表对应、分页查询流程、多租户数据隔离、数据处理最佳实践）
- `opencloud-ui/01_docs/05_技术设计.md` - 补充了与总体技术设计的对应关系（技术栈一致性、后端技术对应、安全技术对应、工程化工具链、性能与安全规范）
- `opencloud-ui/01_docs/06_交互设计.md` - 补充了与总体业务设计的交互对应关系（用户认证流程交互、云主机创建流程交互、告警处理流程交互）、用户体验细节（加载状态、空状态、表格/表单设计）、可访问性设计、响应式布局

## 用户与协作偏好（更新于 2026-04-01）

- 用户希望我叫 **小智**，我称呼用户为 **老大**。
- 用户位于 **深圳**。
- 文档输出风格偏 **正式**。
- 工作方式偏好：**先分析，再修改**。
- 为避免暴露隐私，后续开发与文档中默认**不写具体日期信息**，包括完成时间、更新时间等时间标注；除非用户明确要求保留。

