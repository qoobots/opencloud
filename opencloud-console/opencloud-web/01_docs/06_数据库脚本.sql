-- ============================================================
-- OpenCloud 控制台 — 完整数据库初始化脚本
-- 数据库：PostgreSQL 16
-- 字符集：UTF-8
-- 版本：V1.0.0
-- 最后更新：2026-03-31
-- ============================================================
--
-- 执行说明：
--   1. 以 DBA 账号登录，创建数据库和应用账号
--   2. 以应用账号执行本脚本（建议通过 Flyway 自动执行）
--   3. 脚本幂等设计（IF NOT EXISTS / ON CONFLICT DO NOTHING）
--
-- 表前缀规范：
--   auth_    认证授权模块
--   sys_     系统管理模块
--   cloud_   云平台对接模块
--   monitor_ 监控告警模块
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- 【一】认证授权模块（auth）
-- ─────────────────────────────────────────────────────────────

-- ------------------------------------------------------------
-- 1.1 用户凭证表（auth_user_credential）
-- 存储用户登录认证信息：密码哈希、账号状态、失败计数、锁定时间
-- 与 sys_user 一对一关联（user_id 唯一）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth_user_credential (
    id                    BIGINT          PRIMARY KEY,
    user_id               BIGINT          NOT NULL,
    tenant_id             BIGINT          NOT NULL,
    username              VARCHAR(64)     NOT NULL,
    password_hash         VARCHAR(128)    NOT NULL,
    status                VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    login_fail_count      SMALLINT        NOT NULL DEFAULT 0,
    lock_expire_time      TIMESTAMP,
    last_login_time       TIMESTAMP,
    password_update_time  TIMESTAMP,
    created_at            TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by            BIGINT,
    updated_by            BIGINT,
    deleted               SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  auth_user_credential                       IS '用户凭证表（登录认证信息）';
COMMENT ON COLUMN auth_user_credential.user_id              IS '关联 sys_user.id，一对一';
COMMENT ON COLUMN auth_user_credential.username             IS '登录账号（冗余存储，登录时免 JOIN sys_user）';
COMMENT ON COLUMN auth_user_credential.password_hash        IS 'BCrypt(cost=12) 加密密码';
COMMENT ON COLUMN auth_user_credential.status               IS 'ACTIVE=正常 | DISABLED=禁用 | LOCKED=锁定';
COMMENT ON COLUMN auth_user_credential.login_fail_count     IS '连续登录失败次数，成功登录后清零';
COMMENT ON COLUMN auth_user_credential.lock_expire_time     IS '账号锁定到期时间；NULL 表示未锁定';
COMMENT ON COLUMN auth_user_credential.last_login_time      IS '上次成功登录时间';
COMMENT ON COLUMN auth_user_credential.password_update_time IS '密码最近修改时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_auth_cred_user_id  ON auth_user_credential(user_id)            WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS idx_auth_cred_username ON auth_user_credential(username, tenant_id) WHERE deleted = 0;
CREATE        INDEX IF NOT EXISTS idx_auth_cred_status   ON auth_user_credential(status)              WHERE deleted = 0;

-- ------------------------------------------------------------
-- 1.2 登录日志表（auth_login_log）
-- 记录每次登录/登出行为，用于安全审计与异常登录检测
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth_login_log (
    id              BIGINT          PRIMARY KEY,
    user_id         BIGINT,
    tenant_id       BIGINT,
    username        VARCHAR(64)     NOT NULL,
    login_ip        VARCHAR(64),
    user_agent      VARCHAR(512),
    login_time      TIMESTAMP       NOT NULL DEFAULT NOW(),
    logout_time     TIMESTAMP,
    status          VARCHAR(16)     NOT NULL,
    fail_reason     VARCHAR(128),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  auth_login_log             IS '登录日志表';
COMMENT ON COLUMN auth_login_log.user_id     IS '登录用户 ID（登录失败时可能为 NULL）';
COMMENT ON COLUMN auth_login_log.username    IS '登录账号（失败时也需记录）';
COMMENT ON COLUMN auth_login_log.login_ip    IS '客户端 IP（优先取 X-Forwarded-For）';
COMMENT ON COLUMN auth_login_log.status      IS 'SUCCESS=成功 | FAILED=失败 | LOGOUT=登出 | FORCE_LOGOUT=强制登出';
COMMENT ON COLUMN auth_login_log.fail_reason IS 'PASSWORD_ERROR / ACCOUNT_LOCKED / ACCOUNT_DISABLED 等';

CREATE INDEX IF NOT EXISTS idx_auth_login_log_user_id    ON auth_login_log(user_id);
CREATE INDEX IF NOT EXISTS idx_auth_login_log_username   ON auth_login_log(username);
CREATE INDEX IF NOT EXISTS idx_auth_login_log_login_time ON auth_login_log(login_time DESC);
CREATE INDEX IF NOT EXISTS idx_auth_login_log_status     ON auth_login_log(status);

-- ------------------------------------------------------------
-- 1.3 操作审计日志表（auth_operation_log）
-- 记录密码修改、强制下线、账号解锁等敏感操作
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth_operation_log (
    id              BIGINT          PRIMARY KEY,
    operator_id     BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    target_user_id  BIGINT,
    operation_type  VARCHAR(32)     NOT NULL,
    operate_ip      VARCHAR(64),
    operate_time    TIMESTAMP       NOT NULL DEFAULT NOW(),
    result          VARCHAR(16)     NOT NULL DEFAULT 'SUCCESS',
    remark          VARCHAR(256),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  auth_operation_log                IS '认证授权操作审计日志';
COMMENT ON COLUMN auth_operation_log.operation_type IS 'CHANGE_PASSWORD / FORCE_LOGOUT / UNLOCK_ACCOUNT';
COMMENT ON COLUMN auth_operation_log.result         IS 'SUCCESS / FAILED';

CREATE INDEX IF NOT EXISTS idx_auth_op_log_operator_id  ON auth_operation_log(operator_id);
CREATE INDEX IF NOT EXISTS idx_auth_op_log_operate_time ON auth_operation_log(operate_time DESC);

-- ─────────────────────────────────────────────────────────────
-- 【二】系统管理模块（system）
-- ─────────────────────────────────────────────────────────────

-- ------------------------------------------------------------
-- 2.1 系统用户表（sys_user）
-- 存储用户基础信息；登录凭证由 auth_user_credential 单独管理
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT          PRIMARY KEY,
    tenant_id   BIGINT          NOT NULL,
    username    VARCHAR(64)     NOT NULL,
    nickname    VARCHAR(64),
    email       VARCHAR(128),
    phone       VARCHAR(20),
    avatar      VARCHAR(512),
    gender      SMALLINT        NOT NULL DEFAULT 0,
    status      SMALLINT        NOT NULL DEFAULT 0,
    remark      VARCHAR(512),
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted     SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  sys_user          IS '系统用户表（基础信息）';
COMMENT ON COLUMN sys_user.gender   IS '0=未知 | 1=男 | 2=女';
COMMENT ON COLUMN sys_user.status   IS '0=正常 | 1=禁用';
COMMENT ON COLUMN sys_user.deleted  IS '0=未删除 | 1=已删除（逻辑删除）';

CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_user_username  ON sys_user(username, tenant_id) WHERE deleted = 0;
CREATE        INDEX IF NOT EXISTS idx_sys_user_tenant_id ON sys_user(tenant_id)            WHERE deleted = 0;
CREATE        INDEX IF NOT EXISTS idx_sys_user_email     ON sys_user(email)                WHERE deleted = 0 AND email IS NOT NULL;

-- ------------------------------------------------------------
-- 2.2 系统角色表（sys_role）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT          PRIMARY KEY,
    tenant_id   BIGINT          NOT NULL,
    role_name   VARCHAR(64)     NOT NULL,
    role_code   VARCHAR(64)     NOT NULL,
    description VARCHAR(256),
    status      SMALLINT        NOT NULL DEFAULT 0,
    sort        INT             NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted     SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  sys_role           IS '系统角色表';
COMMENT ON COLUMN sys_role.role_code IS '角色编码，如 ROLE_ADMIN / ROLE_VIEWER（同一租户内唯一）';
COMMENT ON COLUMN sys_role.status    IS '0=启用 | 1=禁用';

CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_role_code ON sys_role(role_code, tenant_id) WHERE deleted = 0;

-- ------------------------------------------------------------
-- 2.3 用户角色关联表（sys_user_role）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE sys_user_role IS '用户角色多对多关联表';

-- ------------------------------------------------------------
-- 2.4 菜单权限表（sys_menu）— 树形结构
-- menu_type: M=目录（无页面）  C=菜单（有页面）  B=按钮（权限标识）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT          PRIMARY KEY,
    parent_id   BIGINT          NOT NULL DEFAULT 0,
    menu_name   VARCHAR(64)     NOT NULL,
    menu_type   CHAR(1)         NOT NULL,
    path        VARCHAR(256),
    component   VARCHAR(256),
    permission  VARCHAR(128),
    icon        VARCHAR(64),
    sort        INT             NOT NULL DEFAULT 0,
    hidden      SMALLINT        NOT NULL DEFAULT 0,
    status      SMALLINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted     SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  sys_menu            IS '菜单权限表（树形结构）';
COMMENT ON COLUMN sys_menu.parent_id  IS '父节点 ID；0 表示根节点';
COMMENT ON COLUMN sys_menu.menu_type  IS 'M=目录 | C=菜单 | B=按钮';
COMMENT ON COLUMN sys_menu.path       IS '前端路由路径';
COMMENT ON COLUMN sys_menu.component  IS '前端组件路径（如 system/user/index）';
COMMENT ON COLUMN sys_menu.permission IS '权限标识（如 system:user:list）';
COMMENT ON COLUMN sys_menu.hidden     IS '0=显示 | 1=隐藏';
COMMENT ON COLUMN sys_menu.status     IS '0=启用 | 1=禁用';

CREATE INDEX IF NOT EXISTS idx_sys_menu_parent_id ON sys_menu(parent_id) WHERE deleted = 0;

-- ------------------------------------------------------------
-- 2.5 角色菜单关联表（sys_role_menu）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

COMMENT ON TABLE sys_role_menu IS '角色菜单多对多关联表';

-- ------------------------------------------------------------
-- 2.6 操作日志表（sys_operation_log）
-- AOP 自动记录所有有 @Log 注解的接口调用
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT,
    module          VARCHAR(64),
    operation       VARCHAR(128),
    method          VARCHAR(256),
    request_method  VARCHAR(16),
    request_url     VARCHAR(512),
    request_params  TEXT,
    response_result TEXT,
    ip              VARCHAR(64),
    user_agent      VARCHAR(512),
    user_id         BIGINT,
    username        VARCHAR(64),
    status          SMALLINT        NOT NULL DEFAULT 0,
    error_msg       TEXT,
    cost_time       BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  sys_operation_log              IS '系统操作日志（AOP 自动记录）';
COMMENT ON COLUMN sys_operation_log.module       IS '模块名（如 用户管理）';
COMMENT ON COLUMN sys_operation_log.operation    IS '操作描述（如 新增用户）';
COMMENT ON COLUMN sys_operation_log.status       IS '0=成功 | 1=失败';
COMMENT ON COLUMN sys_operation_log.cost_time    IS '接口耗时（毫秒）';

CREATE INDEX IF NOT EXISTS idx_sys_op_log_user_id    ON sys_operation_log(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_op_log_tenant_id  ON sys_operation_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_op_log_created_at ON sys_operation_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sys_op_log_status     ON sys_operation_log(status);

-- ─────────────────────────────────────────────────────────────
-- 【三】云平台对接模块（cloud）
-- ─────────────────────────────────────────────────────────────

-- ------------------------------------------------------------
-- 3.1 集群配置表（cloud_cluster_config）
-- 存储 OpenStack / Ceph / Kubernetes 集群连接信息（AES-256-GCM 加密）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cloud_cluster_config (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    cluster_name        VARCHAR(64)     NOT NULL,
    cluster_type        VARCHAR(32)     NOT NULL,
    config_json         TEXT            NOT NULL,
    description         VARCHAR(256),
    status              SMALLINT        NOT NULL DEFAULT 0,
    last_check_time     TIMESTAMP,
    last_check_result   VARCHAR(16),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT,
    deleted             SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  cloud_cluster_config                   IS '云平台集群配置表';
COMMENT ON COLUMN cloud_cluster_config.cluster_type      IS 'OPENSTACK | CEPH | KUBERNETES';
COMMENT ON COLUMN cloud_cluster_config.config_json       IS '连接配置 JSON，AES-256-GCM 加密存储';
COMMENT ON COLUMN cloud_cluster_config.status            IS '0=启用 | 1=禁用 | 2=离线';
COMMENT ON COLUMN cloud_cluster_config.last_check_result IS 'SUCCESS | FAILED';

CREATE UNIQUE INDEX IF NOT EXISTS idx_cloud_cluster_name ON cloud_cluster_config(cluster_name, tenant_id) WHERE deleted = 0;
CREATE        INDEX IF NOT EXISTS idx_cloud_cluster_type ON cloud_cluster_config(cluster_type, tenant_id) WHERE deleted = 0;

-- ------------------------------------------------------------
-- 3.2 云平台异步任务表（cloud_task）
-- 记录云资源创建/删除/变更等异步操作的执行状态
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cloud_task (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    cluster_id      BIGINT          NOT NULL,
    task_type       VARCHAR(64)     NOT NULL,
    task_status     VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    resource_id     VARCHAR(128),
    resource_type   VARCHAR(32),
    request_params  TEXT,
    result_message  TEXT,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  cloud_task              IS '云平台异步任务记录表';
COMMENT ON COLUMN cloud_task.task_type    IS 'CREATE_SERVER / DELETE_SERVER / RESIZE_SERVER 等';
COMMENT ON COLUMN cloud_task.task_status  IS 'PENDING=待执行 | RUNNING=执行中 | SUCCESS=成功 | FAILED=失败';
COMMENT ON COLUMN cloud_task.resource_id  IS '被操作的云资源 ID（如 Nova 实例 UUID）';
COMMENT ON COLUMN cloud_task.resource_type IS 'SERVER / VOLUME / NETWORK / SNAPSHOT 等';

CREATE INDEX IF NOT EXISTS idx_cloud_task_cluster_id  ON cloud_task(cluster_id);
CREATE INDEX IF NOT EXISTS idx_cloud_task_status      ON cloud_task(task_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_cloud_task_created_at  ON cloud_task(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cloud_task_resource_id ON cloud_task(resource_id) WHERE resource_id IS NOT NULL;

-- ------------------------------------------------------------
-- 3.3 资源配额表（cloud_quota）
-- 记录租户在各集群上的资源配额上限与使用量
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cloud_quota (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    cluster_id      BIGINT          NOT NULL,
    resource_type   VARCHAR(32)     NOT NULL,
    quota_limit     BIGINT          NOT NULL,
    quota_used      BIGINT          NOT NULL DEFAULT 0,
    unit            VARCHAR(16),
    synced_at       TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  cloud_quota               IS '云平台资源配额表';
COMMENT ON COLUMN cloud_quota.resource_type IS 'VCPU / MEMORY_MB / DISK_GB / INSTANCE / FLOATING_IP';
COMMENT ON COLUMN cloud_quota.quota_limit   IS '配额上限（-1 表示无限制）';
COMMENT ON COLUMN cloud_quota.quota_used    IS '当前已使用量（定时同步更新）';
COMMENT ON COLUMN cloud_quota.unit          IS '单位：核 / MB / GB / 个';
COMMENT ON COLUMN cloud_quota.synced_at     IS '最近从云平台同步时间';

CREATE UNIQUE INDEX IF NOT EXISTS idx_cloud_quota_unique ON cloud_quota(tenant_id, cluster_id, resource_type) WHERE deleted = 0;

-- ------------------------------------------------------------
-- 3.4 资源同步日志表（cloud_sync_log）
-- 记录每次定时同步或手动同步的执行结果
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cloud_sync_log (
    id              BIGINT          PRIMARY KEY,
    cluster_id      BIGINT          NOT NULL,
    sync_type       VARCHAR(32)     NOT NULL,
    sync_status     VARCHAR(16)     NOT NULL,
    synced_count    INT,
    error_message   TEXT,
    started_at      TIMESTAMP       NOT NULL,
    finished_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  cloud_sync_log           IS '云平台资源同步日志';
COMMENT ON COLUMN cloud_sync_log.sync_type IS 'CLUSTER_STATUS / RESOURCE_USAGE / QUOTA';
COMMENT ON COLUMN cloud_sync_log.sync_status IS 'SUCCESS | FAILED';

CREATE INDEX IF NOT EXISTS idx_cloud_sync_log_cluster_id  ON cloud_sync_log(cluster_id);
CREATE INDEX IF NOT EXISTS idx_cloud_sync_log_created_at  ON cloud_sync_log(created_at DESC);

-- ─────────────────────────────────────────────────────────────
-- 【四】监控告警模块（monitor）
-- ─────────────────────────────────────────────────────────────

-- ------------------------------------------------------------
-- 4.1 告警规则表（monitor_alert_rule）
-- 定义 PromQL 告警触发条件，以及关联的通知渠道
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor_alert_rule (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    rule_name       VARCHAR(128)    NOT NULL,
    expr            TEXT            NOT NULL,
    duration        VARCHAR(32),
    severity        VARCHAR(16)     NOT NULL DEFAULT 'warning',
    summary         VARCHAR(256),
    description     TEXT,
    labels          JSONB,
    notify_channels TEXT,
    status          SMALLINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  monitor_alert_rule                 IS '告警规则表';
COMMENT ON COLUMN monitor_alert_rule.expr            IS 'PromQL 告警触发表达式';
COMMENT ON COLUMN monitor_alert_rule.duration        IS '持续时间阈值，如 5m（超过此时长才触发告警）';
COMMENT ON COLUMN monitor_alert_rule.severity        IS 'critical=严重 | warning=警告 | info=信息';
COMMENT ON COLUMN monitor_alert_rule.summary         IS '告警摘要模板（支持 Go 模板变量）';
COMMENT ON COLUMN monitor_alert_rule.labels          IS '自定义标签 JSON（JSONB 格式）';
COMMENT ON COLUMN monitor_alert_rule.notify_channels IS '通知渠道 ID 列表（JSON 数组，如 [1,2,3]）';
COMMENT ON COLUMN monitor_alert_rule.status          IS '0=启用 | 1=禁用';

CREATE INDEX IF NOT EXISTS idx_alert_rule_tenant_id ON monitor_alert_rule(tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_alert_rule_severity  ON monitor_alert_rule(severity)  WHERE deleted = 0;

-- ------------------------------------------------------------
-- 4.2 告警记录表（monitor_alert_record）
-- 记录每次告警事件，支持 GIN 标签索引进行多维查询
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor_alert_record (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    rule_id         BIGINT,
    alert_name      VARCHAR(128)    NOT NULL,
    severity        VARCHAR(16)     NOT NULL,
    summary         VARCHAR(512),
    description     TEXT,
    labels          JSONB           NOT NULL DEFAULT '{}',
    status          VARCHAR(16)     NOT NULL DEFAULT 'firing',
    fired_at        TIMESTAMP       NOT NULL,
    resolved_at     TIMESTAMP,
    acked_at        TIMESTAMP,
    acked_by        BIGINT,
    notify_count    INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  monitor_alert_record              IS '告警事件记录表';
COMMENT ON COLUMN monitor_alert_record.rule_id      IS '触发的告警规则 ID（AlertManager 直接推送时可能为 NULL）';
COMMENT ON COLUMN monitor_alert_record.labels       IS '告警标签 JSONB（支持 GIN 索引，用于多维查询与静默匹配）';
COMMENT ON COLUMN monitor_alert_record.status       IS 'firing=告警中 | resolved=已恢复 | acked=已确认';
COMMENT ON COLUMN monitor_alert_record.notify_count IS '已发送通知次数';

CREATE UNIQUE INDEX IF NOT EXISTS idx_alert_record_dedup    ON monitor_alert_record(alert_name, (labels->>'instance'), fired_at);
CREATE        INDEX IF NOT EXISTS idx_alert_record_status   ON monitor_alert_record(status);
CREATE        INDEX IF NOT EXISTS idx_alert_record_fired_at ON monitor_alert_record(fired_at DESC);
CREATE        INDEX IF NOT EXISTS idx_alert_record_tenant   ON monitor_alert_record(tenant_id);
CREATE        INDEX IF NOT EXISTS idx_alert_record_labels   ON monitor_alert_record USING GIN(labels);

-- ------------------------------------------------------------
-- 4.3 告警静默规则表（monitor_alert_silence）
-- 在指定时间段内抑制匹配特定标签的告警
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor_alert_silence (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    silence_name    VARCHAR(128)    NOT NULL,
    match_labels    JSONB           NOT NULL DEFAULT '{}',
    start_time      TIMESTAMP       NOT NULL,
    end_time        TIMESTAMP       NOT NULL,
    comment         VARCHAR(512),
    status          SMALLINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  monitor_alert_silence              IS '告警静默规则表';
COMMENT ON COLUMN monitor_alert_silence.match_labels IS '匹配标签条件（AND 关系，JSONB 格式）';
COMMENT ON COLUMN monitor_alert_silence.start_time   IS '静默开始时间';
COMMENT ON COLUMN monitor_alert_silence.end_time     IS '静默结束时间';
COMMENT ON COLUMN monitor_alert_silence.status       IS '0=活跃 | 1=过期（过期后由定时任务标记）';

CREATE INDEX IF NOT EXISTS idx_alert_silence_tenant_id  ON monitor_alert_silence(tenant_id)             WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_alert_silence_time_range ON monitor_alert_silence(start_time, end_time)  WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_alert_silence_match      ON monitor_alert_silence USING GIN(match_labels) WHERE deleted = 0;

-- ------------------------------------------------------------
-- 4.4 通知渠道表（monitor_notify_channel）
-- 告警通知的发送渠道，敏感配置 AES-256-GCM 加密存储
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor_notify_channel (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    channel_name    VARCHAR(64)     NOT NULL,
    channel_type    VARCHAR(32)     NOT NULL,
    config_json     TEXT            NOT NULL,
    status          SMALLINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  monitor_notify_channel              IS '告警通知渠道表';
COMMENT ON COLUMN monitor_notify_channel.channel_type IS 'EMAIL | DINGTALK | WECHAT_WORK | WEBHOOK';
COMMENT ON COLUMN monitor_notify_channel.config_json  IS '渠道配置 JSON，AES-256-GCM 加密存储';
COMMENT ON COLUMN monitor_notify_channel.status       IS '0=启用 | 1=禁用';

CREATE INDEX IF NOT EXISTS idx_notify_channel_tenant_id ON monitor_notify_channel(tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_notify_channel_type      ON monitor_notify_channel(channel_type) WHERE deleted = 0;

-- ------------------------------------------------------------
-- 4.5 通知发送日志表（monitor_notify_log）
-- 记录每次通知的发送结果，包含重试次数和失败原因
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor_notify_log (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    alert_record_id BIGINT          NOT NULL,
    channel_id      BIGINT          NOT NULL,
    channel_type    VARCHAR(32)     NOT NULL,
    status          VARCHAR(16)     NOT NULL,
    retry_count     SMALLINT        NOT NULL DEFAULT 0,
    error_message   TEXT,
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  monitor_notify_log              IS '通知发送日志';
COMMENT ON COLUMN monitor_notify_log.status       IS 'SUCCESS=成功 | FAILED=失败';
COMMENT ON COLUMN monitor_notify_log.retry_count  IS '重试次数（0~3，指数退避）';
COMMENT ON COLUMN monitor_notify_log.channel_type IS '渠道类型冗余（方便统计分析）';

CREATE INDEX IF NOT EXISTS idx_notify_log_alert_record_id ON monitor_notify_log(alert_record_id);
CREATE INDEX IF NOT EXISTS idx_notify_log_channel_id      ON monitor_notify_log(channel_id);
CREATE INDEX IF NOT EXISTS idx_notify_log_created_at      ON monitor_notify_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notify_log_status          ON monitor_notify_log(status);

-- ─────────────────────────────────────────────────────────────
-- 【五】初始化数据
-- ─────────────────────────────────────────────────────────────

-- ------------------------------------------------------------
-- 5.1 初始角色数据
-- ------------------------------------------------------------
INSERT INTO sys_role (id, tenant_id, role_name, role_code, description, status, sort, created_at, updated_at, deleted)
VALUES
    (1, 1, '超级管理员', 'ROLE_ADMIN',  '系统最高权限，不受 RBAC 约束',  0, 1, NOW(), NOW(), 0),
    (2, 1, '只读用户',   'ROLE_VIEWER', '仅查看权限，无任何写操作权限',    0, 2, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 5.2 默认管理员账号（密码：Admin@123，BCrypt cost=12 加密）
-- ------------------------------------------------------------
INSERT INTO sys_user (id, tenant_id, username, nickname, status, created_at, updated_at, deleted)
VALUES (1, 1, 'admin', 'Administrator', 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 对应凭证（BCrypt(Admin@123, cost=12)）
INSERT INTO auth_user_credential (
    id, user_id, tenant_id, username, password_hash,
    status, login_fail_count, created_at, updated_at, deleted
) VALUES (
    1, 1, 1, 'admin',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LuU5BL6N6zPzB8Gm2',
    'ACTIVE', 0, NOW(), NOW(), 0
) ON CONFLICT DO NOTHING;

-- 绑定超管角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 5.3 基础菜单树
-- menu_type: M=目录  C=菜单  B=按钮
-- ------------------------------------------------------------

-- 一级目录：系统管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, hidden, status, created_at, updated_at, deleted) VALUES
(100, 0, '系统管理', 'M', '/system', NULL, NULL, 'setting', 10, 0, 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 系统管理 - 子菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, hidden, status, created_at, updated_at, deleted) VALUES
(101, 100, '用户管理', 'C', '/system/user',   'system/user/index',   NULL,                  'user',   1, 0, 0, NOW(), NOW(), 0),
(102, 100, '角色管理', 'C', '/system/role',   'system/role/index',   NULL,                  'role',   2, 0, 0, NOW(), NOW(), 0),
(103, 100, '菜单管理', 'C', '/system/menu',   'system/menu/index',   NULL,                  'menu',   3, 0, 0, NOW(), NOW(), 0),
(104, 100, '操作日志', 'C', '/system/oplog',  'system/oplog/index',  NULL,                  'log',    4, 0, 0, NOW(), NOW(), 0),
(105, 100, '登录日志', 'C', '/system/loginlog','system/loginlog/index',NULL,                 'login',  5, 0, 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 用户管理 - 按钮权限
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, hidden, status, created_at, updated_at, deleted) VALUES
(1010, 101, '查询',   'B', NULL, NULL, 'system:user:list',   NULL, 1, 0, 0, NOW(), NOW(), 0),
(1011, 101, '新增',   'B', NULL, NULL, 'system:user:add',    NULL, 2, 0, 0, NOW(), NOW(), 0),
(1012, 101, '修改',   'B', NULL, NULL, 'system:user:edit',   NULL, 3, 0, 0, NOW(), NOW(), 0),
(1013, 101, '删除',   'B', NULL, NULL, 'system:user:delete', NULL, 4, 0, 0, NOW(), NOW(), 0),
(1014, 101, '重置密码','B', NULL, NULL, 'system:user:resetpwd',NULL,5, 0, 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 一级目录：云平台管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, hidden, status, created_at, updated_at, deleted) VALUES
(200, 0, '云平台管理', 'M', '/cloud', NULL, NULL, 'cloud', 20, 0, 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 云平台 - 子菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, hidden, status, created_at, updated_at, deleted) VALUES
(201, 200, '集群配置', 'C', '/cloud/cluster',   'cloud/cluster/index',   NULL, 'cluster',  1, 0, 0, NOW(), NOW(), 0),
(202, 200, '云主机',   'C', '/cloud/server',    'cloud/server/index',    NULL, 'server',   2, 0, 0, NOW(), NOW(), 0),
(203, 200, '网络管理', 'C', '/cloud/network',   'cloud/network/index',   NULL, 'network',  3, 0, 0, NOW(), NOW(), 0),
(204, 200, '存储管理', 'C', '/cloud/storage',   'cloud/storage/index',   NULL, 'storage',  4, 0, 0, NOW(), NOW(), 0),
(205, 200, 'K8s 管理', 'C', '/cloud/kubernetes','cloud/kubernetes/index', NULL, 'k8s',     5, 0, 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 一级目录：监控告警
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, hidden, status, created_at, updated_at, deleted) VALUES
(300, 0, '监控告警', 'M', '/monitor', NULL, NULL, 'monitor', 30, 0, 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 监控告警 - 子菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort, hidden, status, created_at, updated_at, deleted) VALUES
(301, 300, '指标查询', 'C', '/monitor/metrics',  'monitor/metrics/index',  NULL, 'metrics',  1, 0, 0, NOW(), NOW(), 0),
(302, 300, 'Grafana',  'C', '/monitor/grafana',  'monitor/grafana/index',  NULL, 'grafana',  2, 0, 0, NOW(), NOW(), 0),
(303, 300, '告警规则', 'C', '/monitor/rules',    'monitor/rules/index',    NULL, 'rule',     3, 0, 0, NOW(), NOW(), 0),
(304, 300, '告警记录', 'C', '/monitor/records',  'monitor/records/index',  NULL, 'alert',    4, 0, 0, NOW(), NOW(), 0),
(305, 300, '静默规则', 'C', '/monitor/silence',  'monitor/silence/index',  NULL, 'silence',  5, 0, 0, NOW(), NOW(), 0),
(306, 300, '通知渠道', 'C', '/monitor/channels', 'monitor/channels/index', NULL, 'channel',  6, 0, 0, NOW(), NOW(), 0),
(307, 300, '通知日志', 'C', '/monitor/notify-log','monitor/notify-log/index',NULL,'log',     7, 0, 0, NOW(), NOW(), 0)
ON CONFLICT DO NOTHING;

-- 绑定超管角色与所有菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE deleted = 0
ON CONFLICT DO NOTHING;

-- ============================================================
-- END OF SCRIPT
-- ============================================================
