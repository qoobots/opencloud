-- ============================================================
-- OpenCloud 控制台 — 总体数据库初始化脚本
-- 数据库：PostgreSQL 16
-- Schema：public（所有模块表）
-- 版本：V1.0.0
-- 最后更新：2026-04-01
-- ============================================================
--
-- 执行说明：
--   1. 以 DBA 账号登录，创建数据库和应用账号
--   2. 以应用账号执行本脚本（建议通过 Flyway 自动执行）
--   3. 脚本幂等设计（IF NOT EXISTS / ON CONFLICT DO NOTHING）
--
-- 表前缀规范：
--   auth_     认证授权模块
--   sys_      系统管理模块
--   cloud_    云平台对接模块
--   monitor_  监控告警模块
--
-- Schema 规划：
--   auth     - 认证授权模块（用户凭证、登录日志、操作日志）
--   system   - 系统管理模块（租户、部门、用户、角色、菜单、字典）
--   cloud    - 云平台对接模块（集群配置、任务、配额、同步日志）
--   monitor  - 监控告警模块（告警规则、告警记录、静默、通知渠道、通知日志）
-- ============================================================

-- ------------------------------------------------------------
-- 【0】扩展（uuid 生成）
-- ------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- 【一】认证授权模块（auth）
-- ============================================================

-- ------------------------------------------------------------
-- 1.1 创建 Schema
-- ------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS auth;

-- ------------------------------------------------------------
-- 1.2 枚举类型
-- ------------------------------------------------------------
-- 账号状态
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_status') THEN
        CREATE TYPE auth.account_status AS ENUM ('ACTIVE', 'DISABLED', 'LOCKED');
    END IF;
END$$;

-- 登录/登出事件状态
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'login_status') THEN
        CREATE TYPE auth.login_status AS ENUM ('SUCCESS', 'FAILED', 'LOGOUT', 'FORCE_LOGOUT');
    END IF;
END$$;

-- 操作日志类型
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'operation_type') THEN
        CREATE TYPE auth.operation_type AS ENUM ('CHANGE_PASSWORD', 'FORCE_LOGOUT', 'UNLOCK_ACCOUNT');
    END IF;
END$$;

-- ------------------------------------------------------------
-- 1.3 用户凭证表 auth_user_credential
-- 存储用户认证凭证，与 system 模块 sys_user 通过 user_id 关联但解耦。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth.auth_user_credential (
    id                   BIGINT          PRIMARY KEY,
    user_id              VARCHAR(36)     NOT NULL,
    tenant_id            VARCHAR(36)     NOT NULL,
    username             VARCHAR(64)     NOT NULL,
    password_hash        VARCHAR(128)    NOT NULL,
    status               VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    login_fail_count     SMALLINT        NOT NULL DEFAULT 0,
    lock_expire_time     TIMESTAMPTZ,
    last_login_time      TIMESTAMPTZ,
    password_update_time TIMESTAMPTZ,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT chk_auc_status            CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED')),
    CONSTRAINT chk_auc_login_fail_count  CHECK (login_fail_count >= 0)
);

-- 索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_auc_username    ON auth.auth_user_credential (username) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_auc_user_id      ON auth.auth_user_credential (user_id) WHERE deleted_at IS NULL;
CREATE INDEX        IF NOT EXISTS idx_auc_tenant      ON auth.auth_user_credential (tenant_id);
CREATE INDEX        IF NOT EXISTS idx_auc_status       ON auth.auth_user_credential (status) WHERE deleted_at IS NULL;

COMMENT ON TABLE  auth.auth_user_credential                IS '用户认证凭证表 - 存储登录账号、密码哈希、账号状态及安全相关字段';
COMMENT ON COLUMN auth.auth_user_credential.id              IS '自增主键';
COMMENT ON COLUMN auth.auth_user_credential.user_id         IS '关联 sys_user.id（UUID 格式），租户内唯一';
COMMENT ON COLUMN auth.auth_user_credential.tenant_id       IS '所属租户 ID';
COMMENT ON COLUMN auth.auth_user_credential.username         IS '登录账号，同一租户内唯一（逻辑删除不计入唯一约束）';
COMMENT ON COLUMN auth.auth_user_credential.password_hash    IS 'BCrypt 加密密码（cost=12），禁止明文存储';
COMMENT ON COLUMN auth.auth_user_credential.status          IS '账号状态: ACTIVE=正常 | DISABLED=禁用 | LOCKED=锁定';
COMMENT ON COLUMN auth.auth_user_credential.login_fail_count IS '连续登录失败次数；≥5 次触发锁定；登录成功 / 管理员解锁后清零';
COMMENT ON COLUMN auth.auth_user_credential.lock_expire_time   IS '锁定自动解除时间；下次登录时若 lock_expire_time <= now() 则自动解锁';
COMMENT ON COLUMN auth.auth_user_credential.last_login_time    IS '最近一次登录成功时间';
COMMENT ON COLUMN auth.auth_user_credential.password_update_time IS '最近一次修改密码时间';

-- ------------------------------------------------------------
-- 1.4 登录日志表 auth_login_log
-- 记录每次登录 / 登出行为，只追加写入。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth.auth_login_log (
    id          BIGINT          PRIMARY KEY,
    user_id     VARCHAR(36),
    tenant_id   VARCHAR(36),
    username    VARCHAR(64)     NOT NULL,
    login_ip    VARCHAR(64),
    user_agent  VARCHAR(512),
    login_time  TIMESTAMPTZ,
    logout_time TIMESTAMPTZ,
    status      VARCHAR(16)     NOT NULL,
    fail_reason VARCHAR(256),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_all_status CHECK (status IN ('SUCCESS', 'FAILED', 'LOGOUT', 'FORCE_LOGOUT'))
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_all_user_time   ON auth.auth_login_log (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_all_tenant_time ON auth.auth_login_log (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_all_status      ON auth.auth_login_log (status);
CREATE INDEX IF NOT EXISTS idx_all_login_time  ON auth.auth_login_log (login_time DESC);

COMMENT ON TABLE  auth.auth_login_log           IS '登录/登出日志表 - 只追加写入，用于审计与异常分析';
COMMENT ON COLUMN auth.auth_login_log.user_id   IS '登录用户 ID';
COMMENT ON COLUMN auth.auth_login_log.tenant_id IS '所属租户 ID';
COMMENT ON COLUMN auth.auth_login_log.username  IS '登录账号';
COMMENT ON COLUMN auth.auth_login_log.login_ip  IS '客户端 IP，优先取 X-Forwarded-For';
COMMENT ON COLUMN auth.auth_login_log.status   IS 'SUCCESS=登录成功 | FAILED=登录失败 | LOGOUT=主动登出 | FORCE_LOGOUT=被强制下线';

-- ------------------------------------------------------------
-- 1.5 操作日志表 auth_operation_log
-- 记录修改密码、强制下线、解锁账号等关键管理操作
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth.auth_operation_log (
    id              BIGINT          PRIMARY KEY,
    operator_id     VARCHAR(36)     NOT NULL,
    tenant_id       VARCHAR(36)     NOT NULL,
    target_user_id  VARCHAR(36),
    operation_type  VARCHAR(32)     NOT NULL,
    operate_time    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    operate_ip      VARCHAR(64),
    remark          VARCHAR(256),
    CONSTRAINT chk_aol_operation_type CHECK (operation_type IN ('CHANGE_PASSWORD', 'FORCE_LOGOUT', 'UNLOCK_ACCOUNT'))
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_aol_operator_time ON auth.auth_operation_log (operator_id, operate_time DESC);
CREATE INDEX IF NOT EXISTS idx_aol_target_time   ON auth.auth_operation_log (target_user_id, operate_time DESC);
CREATE INDEX IF NOT EXISTS idx_aol_type           ON auth.auth_operation_log (operation_type);
CREATE INDEX IF NOT EXISTS idx_aol_tenant_time    ON auth.auth_operation_log (tenant_id, operate_time DESC);

COMMENT ON TABLE  auth.auth_operation_log              IS '关键管理操作审计日志表';
COMMENT ON COLUMN auth.auth_operation_log.operation_type IS '操作类型: CHANGE_PASSWORD=修改密码 | FORCE_LOGOUT=强制下线 | UNLOCK_ACCOUNT=解锁账号';

-- ============================================================
-- 【二】系统管理模块（system）
-- ============================================================

-- ------------------------------------------------------------
-- 2.1 创建 Schema
-- ------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS system;

-- ------------------------------------------------------------
-- 2.2 租户表 sys_tenant
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system.sys_tenant (
    id             VARCHAR(36)   PRIMARY KEY,
    tenant_code    VARCHAR(32)   NOT NULL,
    tenant_name    VARCHAR(64)   NOT NULL,
    contact_name   VARCHAR(32),
    contact_email  VARCHAR(128),
    status         VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    is_default     BOOLEAN       NOT NULL DEFAULT FALSE,
    expire_time    TIMESTAMPTZ,
    remark         VARCHAR(256),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted        SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT chk_st_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX idx_st_code   ON system.sys_tenant(tenant_code) WHERE deleted = 0;
CREATE INDEX        idx_st_status ON system.sys_tenant(status);

COMMENT ON TABLE  system.sys_tenant             IS '租户信息表';
COMMENT ON COLUMN system.sys_tenant.tenant_code IS '租户编码，全局唯一';
COMMENT ON COLUMN system.sys_tenant.is_default  IS '是否默认租户，默认租户不可删除';

-- ------------------------------------------------------------
-- 2.3 部门表 sys_dept
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system.sys_dept (
    id          BIGSERIAL     PRIMARY KEY,
    tenant_id   VARCHAR(36)   NOT NULL,
    parent_id   BIGINT        NOT NULL DEFAULT 0,
    dept_name   VARCHAR(64)   NOT NULL,
    leader      VARCHAR(32),
    phone       VARCHAR(16),
    email       VARCHAR(128),
    sort        INTEGER       NOT NULL DEFAULT 0,
    status      VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT chk_sd_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_sd_parent ON system.sys_dept(parent_id);
CREATE INDEX idx_sd_tenant ON system.sys_dept(tenant_id);

COMMENT ON TABLE  system.sys_dept           IS '部门/组织架构树';
COMMENT ON COLUMN system.sys_dept.parent_id IS '父部门 ID，0 表示根节点';

-- ------------------------------------------------------------
-- 2.4 用户表 sys_user
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system.sys_user (
    id          VARCHAR(36)   PRIMARY KEY,
    tenant_id   VARCHAR(36)   NOT NULL,
    dept_id     BIGINT,
    username    VARCHAR(64)   NOT NULL,
    nickname    VARCHAR(64)   NOT NULL,
    avatar      VARCHAR(512),
    email       VARCHAR(128),
    phone       VARCHAR(16),
    status      VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    is_builtin  BOOLEAN       NOT NULL DEFAULT FALSE,
    sort        INTEGER       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT chk_su_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX idx_su_username_tenant ON system.sys_user(username, tenant_id) WHERE deleted = 0;
CREATE INDEX        idx_su_tenant          ON system.sys_user(tenant_id);
CREATE INDEX        idx_su_dept            ON system.sys_user(dept_id);
CREATE INDEX        idx_su_status          ON system.sys_user(status);

COMMENT ON TABLE  system.sys_user            IS '用户基础信息表（不含认证凭证）';
COMMENT ON COLUMN system.sys_user.id         IS '用户 ID（UUID），与 auth_user_credential.user_id 关联';
COMMENT ON COLUMN system.sys_user.is_builtin IS '是否内置账号，内置账号不允许删除/禁用';

-- ------------------------------------------------------------
-- 2.5 角色表 sys_role
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system.sys_role (
    id          BIGSERIAL     PRIMARY KEY,
    tenant_id   VARCHAR(36)   NOT NULL,
    role_name   VARCHAR(64)   NOT NULL,
    role_code   VARCHAR(64)   NOT NULL,
    description VARCHAR(256),
    is_builtin  BOOLEAN       NOT NULL DEFAULT FALSE,
    status      VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    sort        INTEGER       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT chk_sr_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX idx_sr_code_tenant ON system.sys_role(role_code, tenant_id) WHERE deleted = 0;
CREATE INDEX        idx_sr_tenant      ON system.sys_role(tenant_id);

COMMENT ON TABLE  system.sys_role            IS '角色定义表';
COMMENT ON COLUMN system.sys_role.role_code  IS '角色编码，如 SUPER_ADMIN / ROLE_ADMIN，同租户内唯一';

-- ------------------------------------------------------------
-- 2.6 用户-角色关联表 sys_user_role
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system.sys_user_role (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    VARCHAR(36) NOT NULL,
    role_id    BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_sur_user_role ON system.sys_user_role(user_id, role_id);
CREATE INDEX        idx_sur_role      ON system.sys_user_role(role_id);

COMMENT ON TABLE system.sys_user_role IS '用户-角色多对多关联表';

-- ------------------------------------------------------------
-- 2.7 菜单/权限表 sys_menu
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system.sys_menu (
    id          BIGSERIAL     PRIMARY KEY,
    parent_id   BIGINT        NOT NULL DEFAULT 0,
    menu_name   VARCHAR(64)   NOT NULL,
    menu_type   SMALLINT      NOT NULL,
    path        VARCHAR(256),
    component   VARCHAR(256),
    icon        VARCHAR(64),
    permission  VARCHAR(128),
    visible     BOOLEAN       NOT NULL DEFAULT TRUE,
    sort        INTEGER       NOT NULL DEFAULT 0,
    remark      VARCHAR(256),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT chk_sm_type CHECK (menu_type IN (0, 1, 2))
);

CREATE INDEX        idx_sm_parent     ON system.sys_menu(parent_id);
CREATE INDEX        idx_sm_type       ON system.sys_menu(menu_type);
CREATE UNIQUE INDEX idx_sm_permission ON system.sys_menu(permission) WHERE deleted = 0 AND permission IS NOT NULL;

COMMENT ON TABLE  system.sys_menu            IS '菜单/权限树';
COMMENT ON COLUMN system.sys_menu.menu_type IS '类型: 0=目录 1=菜单 2=按钮/权限';
COMMENT ON COLUMN system.sys_menu.permission IS '权限标识，格式 module:resource:action';

-- ------------------------------------------------------------
-- 2.8 角色-菜单关联表 sys_role_menu
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system.sys_role_menu (
    id         BIGSERIAL   PRIMARY KEY,
    role_id    BIGINT      NOT NULL,
    menu_id    BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_srm_role_menu ON system.sys_role_menu(role_id, menu_id);
CREATE INDEX        idx_srm_menu      ON system.sys_role_menu(menu_id);

COMMENT ON TABLE system.sys_role_menu IS '角色-菜单多对多关联表';

-- ------------------------------------------------------------
-- 2.9 字典类型表 sys_dict_type
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system.sys_dict_type (
    id          BIGSERIAL     PRIMARY KEY,
    dict_type   VARCHAR(64)   NOT NULL,
    dict_name   VARCHAR(64)   NOT NULL,
    is_system   BOOLEAN       NOT NULL DEFAULT FALSE,
    status      VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    remark      VARCHAR(256),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted     SMALLINT      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_sdt_type ON system.sys_dict_type(dict_type) WHERE deleted = 0;

COMMENT ON TABLE  system.sys_dict_type           IS '字典类型表';
COMMENT ON COLUMN system.sys_dict_type.is_system IS '是否系统内置字典，系统字典不允许删除';

-- ------------------------------------------------------------
-- 2.10 字典数据表 sys_dict_data
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system.sys_dict_data (
    id          BIGSERIAL     PRIMARY KEY,
    dict_type   VARCHAR(64)   NOT NULL,
    dict_label  VARCHAR(64)   NOT NULL,
    dict_value  VARCHAR(64)   NOT NULL,
    sort        INTEGER       NOT NULL DEFAULT 0,
    is_default  BOOLEAN       NOT NULL DEFAULT FALSE,
    status      VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    remark      VARCHAR(256),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted     SMALLINT      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_sdd_type_value ON system.sys_dict_data(dict_type, dict_value) WHERE deleted = 0;
CREATE INDEX        idx_sdd_type       ON system.sys_dict_data(dict_type);

COMMENT ON TABLE  system.sys_dict_data            IS '字典数据表';
COMMENT ON COLUMN system.sys_dict_data.dict_type  IS '所属字典类型编码';

-- ------------------------------------------------------------
-- 2.11 操作日志表 sys_operation_log
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system.sys_operation_log (
    id              BIGSERIAL     PRIMARY KEY,
    user_id         VARCHAR(36),
    tenant_id       VARCHAR(36),
    username        VARCHAR(64),
    module_name     VARCHAR(64)   NOT NULL,
    operation_type  VARCHAR(64)   NOT NULL,
    method          VARCHAR(8)    NOT NULL,
    request_url     VARCHAR(512)  NOT NULL,
    request_param   TEXT,
    response_result TEXT,
    exception_msg   TEXT,
    status          VARCHAR(16)   NOT NULL,
    cost_time       INTEGER,
    operate_ip      VARCHAR(64),
    operate_time    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sol_status CHECK (status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX idx_sol_user   ON system.sys_operation_log(user_id, operate_time DESC);
CREATE INDEX idx_sol_tenant ON system.sys_operation_log(tenant_id, operate_time DESC);
CREATE INDEX idx_sol_module ON system.sys_operation_log(module_name);
CREATE INDEX idx_sol_status ON system.sys_operation_log(status);
CREATE INDEX idx_sol_time   ON system.sys_operation_log(operate_time DESC);

COMMENT ON TABLE  system.sys_operation_log                IS '操作日志表，只追加写入，不修改删除';
COMMENT ON COLUMN system.sys_operation_log.request_param IS '请求参数（JSON，已脱敏）';
COMMENT ON COLUMN system.sys_operation_log.cost_time      IS '接口耗时（毫秒）';

-- ============================================================
-- 【三】云平台对接模块（cloud）
-- ============================================================

-- ------------------------------------------------------------
-- 3.1 创建 Schema
-- ------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS cloud;

-- ------------------------------------------------------------
-- 3.2 集群配置表 cloud_cluster_config
-- 存储 OpenStack、Ceph、Kubernetes 的连接配置信息
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cloud.cloud_cluster_config (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(36)     NOT NULL,
    name                VARCHAR(64)     NOT NULL,
    type                VARCHAR(16)     NOT NULL,
    endpoint            VARCHAR(256)    NOT NULL,
    config_json         TEXT            NOT NULL,
    status              VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    last_check_time     TIMESTAMPTZ,
    error_msg           VARCHAR(512),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    deleted             SMALLINT        NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_ccc_name    ON cloud.cloud_cluster_config(tenant_id, name) WHERE deleted = 0;
CREATE INDEX        idx_ccc_tenant  ON cloud.cloud_cluster_config(tenant_id);
CREATE INDEX        idx_ccc_type    ON cloud.cloud_cluster_config(type);
CREATE INDEX        idx_ccc_status  ON cloud.cloud_cluster_config(status);

COMMENT ON TABLE  cloud.cloud_cluster_config              IS '云平台集群配置表';
COMMENT ON COLUMN cloud.cloud_cluster_config.type        IS '集群类型: OPENSTACK / CEPH / KUBERNETES';
COMMENT ON COLUMN cloud.cloud_cluster_config.config_json IS '连接配置 JSON（AES-256-GCM 加密）';
COMMENT ON COLUMN cloud.cloud_cluster_config.status      IS 'ACTIVE / ERROR / PENDING';

-- ------------------------------------------------------------
-- 3.3 异步任务表 cloud_task
-- 记录云资源异步操作的任务状态
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cloud.cloud_task (
    id              BIGSERIAL       PRIMARY KEY,
    task_id         VARCHAR(36)     NOT NULL UNIQUE,
    tenant_id       VARCHAR(36)     NOT NULL,
    type            VARCHAR(32)     NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    resource_type   VARCHAR(32)     NOT NULL,
    resource_id     VARCHAR(64),
    cluster_id      VARCHAR(36)     NOT NULL,
    progress        SMALLINT        NOT NULL DEFAULT 0,
    result_json     TEXT,
    error_msg       VARCHAR(512),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_by      VARCHAR(36)     NOT NULL
);

CREATE UNIQUE INDEX idx_ct_task_id   ON cloud.cloud_task(task_id);
CREATE INDEX        idx_ct_tenant    ON cloud.cloud_task(tenant_id);
CREATE INDEX        idx_ct_status    ON cloud.cloud_task(status);
CREATE INDEX        idx_ct_type      ON cloud.cloud_task(type);
CREATE INDEX        idx_ct_cluster   ON cloud.cloud_task(cluster_id);
CREATE INDEX        idx_ct_resource  ON cloud.cloud_task(resource_type, resource_id);
CREATE INDEX        idx_ct_created   ON cloud.cloud_task(created_at DESC);

COMMENT ON TABLE  cloud.cloud_task              IS '异步任务表，跟踪云资源操作';
COMMENT ON COLUMN cloud.cloud_task.task_id      IS '任务唯一标识（UUID）';
COMMENT ON COLUMN cloud.cloud_task.type         IS '任务类型: INSTANCE_CREATE / INSTANCE_DELETE / VOLUME_CREATE / ...';
COMMENT ON COLUMN cloud.cloud_task.status       IS 'PENDING / RUNNING / SUCCESS / FAILED / CANCELLED';

-- ------------------------------------------------------------
-- 3.4 配额配置表 cloud_quota
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cloud.cloud_quota (
    id                              BIGSERIAL       PRIMARY KEY,
    tenant_id                       VARCHAR(36)     NOT NULL UNIQUE,
    openstack_vcpu_limit            INTEGER         NOT NULL DEFAULT -1,
    openstack_memory_limit          BIGINT          NOT NULL DEFAULT -1,
    openstack_instance_limit        INTEGER         NOT NULL DEFAULT -1,
    openstack_volume_count_limit    INTEGER         NOT NULL DEFAULT -1,
    openstack_volume_storage_limit  BIGINT          NOT NULL DEFAULT -1,
    ceph_storage_limit              BIGINT          NOT NULL DEFAULT -1,
    k8s_cpu_limit                   INTEGER         NOT NULL DEFAULT -1,
    k8s_memory_limit                BIGINT          NOT NULL DEFAULT -1,
    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_cq_tenant ON cloud.cloud_quota(tenant_id);

COMMENT ON TABLE  cloud.cloud_quota                           IS '租户资源配额表';
COMMENT ON COLUMN cloud.cloud_quota.openstack_vcpu_limit      IS 'OpenStack vCPU 限制（-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.openstack_memory_limit    IS 'OpenStack 内存限制（MB，-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.openstack_instance_limit  IS 'OpenStack 实例数限制（-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.ceph_storage_limit        IS 'Ceph 存储限制（GB，-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.k8s_cpu_limit            IS 'K8s CPU 限制（核，-1=无限制）';
COMMENT ON COLUMN cloud.cloud_quota.k8s_memory_limit        IS 'K8s 内存限制（MB，-1=无限制）';

-- ------------------------------------------------------------
-- 3.5 资源同步日志表 cloud_sync_log
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cloud.cloud_sync_log (
    id                BIGSERIAL       PRIMARY KEY,
    cluster_id        VARCHAR(36)     NOT NULL,
    tenant_id         VARCHAR(36)     NOT NULL,
    sync_type         VARCHAR(16)     NOT NULL,
    status            VARCHAR(16)     NOT NULL,
    resource_counts   JSONB,
    error_msg         VARCHAR(512),
    started_at        TIMESTAMPTZ     NOT NULL,
    completed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_csl_cluster  ON cloud.cloud_sync_log(cluster_id);
CREATE INDEX idx_csl_tenant   ON cloud.cloud_sync_log(tenant_id);
CREATE INDEX idx_csl_status   ON cloud.cloud_sync_log(status);
CREATE INDEX idx_csl_created  ON cloud.cloud_sync_log(created_at DESC);

COMMENT ON TABLE  cloud.cloud_sync_log            IS '资源同步日志表';
COMMENT ON COLUMN cloud.cloud_sync_log.sync_type   IS '同步类型: AUTO / MANUAL';
COMMENT ON COLUMN cloud.cloud_sync_log.status      IS '状态: SUCCESS / FAILED';

-- ============================================================
-- 【四】监控告警模块（monitor）
-- ============================================================

-- ------------------------------------------------------------
-- 4.1 创建 Schema
-- ------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS monitor;

-- ------------------------------------------------------------
-- 4.2 枚举类型
-- ------------------------------------------------------------
-- 通知渠道类型
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notify_channel_type') THEN
        CREATE TYPE monitor.notify_channel_type AS ENUM ('EMAIL', 'DINGTALK', 'WECHAT_WORK', 'WEBHOOK');
    END IF;
END$$;

-- ------------------------------------------------------------
-- 4.3 告警规则表 monitor_alert_rule
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor.monitor_alert_rule (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(36)     NOT NULL,
    rule_name           VARCHAR(100)    NOT NULL,
    description         VARCHAR(500)    DEFAULT '',
    promql_expr         TEXT            NOT NULL,
    severity            VARCHAR(20)     NOT NULL,
    duration            VARCHAR(10)     NOT NULL DEFAULT '5m',
    notify_channel_ids  TEXT            DEFAULT '[]',
    notify_on_resolve   BOOLEAN         NOT NULL DEFAULT FALSE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ENABLED',
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    CONSTRAINT chk_mar_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_mar_status   CHECK (status IN ('ENABLED', 'DISABLED'))
);

CREATE UNIQUE INDEX idx_mar_name_tenant ON monitor.monitor_alert_rule(tenant_id, rule_name) WHERE deleted = 0;
CREATE INDEX        idx_mar_tenant_status ON monitor.monitor_alert_rule(tenant_id, status);

COMMENT ON TABLE  monitor.monitor_alert_rule               IS '告警规则表';
COMMENT ON COLUMN monitor.monitor_alert_rule.rule_name    IS '规则名称，同租户下唯一';
COMMENT ON COLUMN monitor.monitor_alert_rule.promql_expr  IS 'PromQL 表达式';
COMMENT ON COLUMN monitor.monitor_alert_rule.severity     IS 'INFO / WARNING / CRITICAL';
COMMENT ON COLUMN monitor.monitor_alert_rule.duration    IS '持续时间：告警条件持续满足此时长才触发';

-- ------------------------------------------------------------
-- 4.4 告警记录表 monitor_alert_record
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor.monitor_alert_record (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(36)     NOT NULL,
    rule_id         BIGINT,
    rule_name       VARCHAR(100)    NOT NULL,
    alert_name      VARCHAR(200)    NOT NULL,
    severity        VARCHAR(20)     NOT NULL,
    instance        VARCHAR(200)    DEFAULT '',
    labels          TEXT            NOT NULL DEFAULT '{}',
    annotations     TEXT            NOT NULL DEFAULT '{}',
    summary         VARCHAR(500)    DEFAULT '',
    status          VARCHAR(20)     NOT NULL DEFAULT 'FIRING',
    fired_at        TIMESTAMPTZ     NOT NULL,
    resolved_at     TIMESTAMPTZ,
    ack_by          VARCHAR(36),
    ack_at          TIMESTAMPTZ,
    ack_note        VARCHAR(500),
    notified        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_malr_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_malr_status   CHECK (status IN ('FIRING', 'ACKNOWLEDGED', 'RESOLVED'))
);

CREATE UNIQUE INDEX idx_malr_dedup ON monitor.monitor_alert_record(alert_name, instance, fired_at);
CREATE INDEX        idx_malr_tenant_status ON monitor.monitor_alert_record(tenant_id, status);
CREATE INDEX        idx_malr_tenant_fired ON monitor.monitor_alert_record(tenant_id, fired_at DESC);
CREATE INDEX        idx_malr_rule_id ON monitor.monitor_alert_record(rule_id);

COMMENT ON TABLE  monitor.monitor_alert_record              IS '告警记录表，仅追加，不做逻辑删除';
COMMENT ON COLUMN monitor.monitor_alert_record.status       IS 'FIRING=触发中，ACKNOWLEDGED=已确认，RESOLVED=已解决';

-- ------------------------------------------------------------
-- 4.5 通知渠道表 monitor_notify_channel
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor.monitor_notify_channel (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(36)     NOT NULL,
    channel_name    VARCHAR(100)    NOT NULL,
    channel_type    VARCHAR(20)     NOT NULL,
    config          TEXT            NOT NULL DEFAULT '{}',
    description     VARCHAR(300)    DEFAULT '',
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(36),
    updated_by      VARCHAR(36)
);

CREATE INDEX idx_mnc_tenant ON monitor.monitor_notify_channel(tenant_id) WHERE deleted = 0;

COMMENT ON TABLE  monitor.monitor_notify_channel           IS '通知渠道配置表';
COMMENT ON COLUMN monitor.monitor_notify_channel.channel_type IS 'EMAIL / DINGTALK / WECHAT_WORK / WEBHOOK';
COMMENT ON COLUMN monitor.monitor_notify_channel.config    IS '渠道配置 JSON（AES-256-GCM 加密）';

-- ------------------------------------------------------------
-- 4.6 通知发送日志表 monitor_notify_log
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor.monitor_notify_log (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(36)     NOT NULL,
    alert_record_id BIGINT          NOT NULL,
    channel_id      BIGINT          NOT NULL,
    channel_name    VARCHAR(100)    NOT NULL,
    channel_type    VARCHAR(20)     NOT NULL,
    is_test         BOOLEAN         NOT NULL DEFAULT FALSE,
    status          VARCHAR(20)     NOT NULL,
    error_msg       TEXT,
    retry_count     SMALLINT        NOT NULL DEFAULT 0,
    sent_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_mnl_status CHECK (status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX idx_mnl_record   ON monitor.monitor_notify_log(alert_record_id);
CREATE INDEX idx_mnl_tenant   ON monitor.monitor_notify_log(tenant_id, sent_at DESC);
CREATE INDEX idx_mnl_channel   ON monitor.monitor_notify_log(channel_id, sent_at DESC);

COMMENT ON TABLE  monitor.monitor_notify_log              IS '通知发送日志，只追加，不做逻辑删除';
COMMENT ON COLUMN monitor.monitor_notify_log.retry_count  IS '重试次数，最大 3 次';

-- ------------------------------------------------------------
-- 4.7 告警静默表 monitor_alert_silence
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor.monitor_alert_silence (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(36)     NOT NULL,
    silence_name    VARCHAR(100)    NOT NULL,
    description     VARCHAR(300)    DEFAULT '',
    match_labels    TEXT            NOT NULL DEFAULT '[]',
    start_at        TIMESTAMPTZ     NOT NULL,
    end_at          TIMESTAMPTZ     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(36),
    updated_by      VARCHAR(36),
    CONSTRAINT chk_mas_status CHECK (status IN ('ACTIVE', 'EXPIRED'))
);

CREATE INDEX idx_mas_tenant_status ON monitor.monitor_alert_silence(tenant_id, status);
CREATE INDEX idx_mas_time         ON monitor.monitor_alert_silence(start_at, end_at);

COMMENT ON TABLE  monitor.monitor_alert_silence            IS '告警静默规则表';
COMMENT ON COLUMN monitor.monitor_alert_silence.match_labels IS '标签匹配规则（JSON 数组），所有条件 AND 匹配';
COMMENT ON COLUMN monitor.monitor_alert_silence.status     IS 'ACTIVE=生效中，EXPIRED=已停止或自动过期';

-- ============================================================
-- 【五】触发器：updated_at 自动更新
-- ============================================================

-- auth 模块触发器
CREATE OR REPLACE FUNCTION auth.fn_update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auc_updated_at ON auth.auth_user_credential;
CREATE TRIGGER trg_auc_updated_at
    BEFORE UPDATE ON auth.auth_user_credential
    FOR EACH ROW EXECUTE FUNCTION auth.fn_update_updated_at();

-- system 模块触发器
CREATE OR REPLACE FUNCTION system.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_st_updated_at  BEFORE UPDATE ON system.sys_tenant     FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_sd_updated_at  BEFORE UPDATE ON system.sys_dept       FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_su_updated_at  BEFORE UPDATE ON system.sys_user       FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_sr_updated_at  BEFORE UPDATE ON system.sys_role       FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_sm_updated_at  BEFORE UPDATE ON system.sys_menu       FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_sdt_updated_at BEFORE UPDATE ON system.sys_dict_type  FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();
CREATE TRIGGER trg_sdd_updated_at BEFORE UPDATE ON system.sys_dict_data  FOR EACH ROW EXECUTE FUNCTION system.update_updated_at();

-- cloud 模块触发器
CREATE OR REPLACE FUNCTION cloud.fn_update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ccc_updated_at ON cloud.cloud_cluster_config;
CREATE TRIGGER trg_ccc_updated_at
    BEFORE UPDATE ON cloud.cloud_cluster_config
    FOR EACH ROW EXECUTE FUNCTION cloud.fn_update_updated_at();

DROP TRIGGER IF EXISTS trg_ct_updated_at ON cloud.cloud_task;
CREATE TRIGGER trg_ct_updated_at
    BEFORE UPDATE ON cloud.cloud_task
    FOR EACH ROW EXECUTE FUNCTION cloud.fn_update_updated_at();

DROP TRIGGER IF EXISTS trg_cq_updated_at ON cloud.cloud_quota;
CREATE TRIGGER trg_cq_updated_at
    BEFORE UPDATE ON cloud.cloud_quota
    FOR EACH ROW EXECUTE FUNCTION cloud.fn_update_updated_at();

-- monitor 模块触发器
CREATE OR REPLACE FUNCTION monitor.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_mar_updated_at ON monitor.monitor_alert_rule      FOR EACH ROW EXECUTE FUNCTION monitor.update_updated_at();
CREATE TRIGGER trg_mnc_updated_at ON monitor.monitor_notify_channel  FOR EACH ROW EXECUTE FUNCTION monitor.update_updated_at();
CREATE TRIGGER trg_mas_updated_at ON monitor.monitor_alert_silence    FOR EACH ROW EXECUTE FUNCTION monitor.update_updated_at();
CREATE TRIGGER trg_malr_updated_at ON monitor.monitor_alert_record   FOR EACH ROW EXECUTE FUNCTION monitor.update_updated_at();

-- ============================================================
-- 【六】初始化数据
-- ============================================================

-- ------------------------------------------------------------
-- 6.1 初始化默认租户
-- ------------------------------------------------------------
INSERT INTO system.sys_tenant (id, tenant_code, tenant_name, is_default, status)
VALUES ('00000000-0000-0000-0000-000000000001', 'default', '默认租户', TRUE, 'ACTIVE')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 6.2 初始化超级管理员用户
-- ------------------------------------------------------------
INSERT INTO system.sys_user (id, tenant_id, username, nickname, status, is_builtin)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'superadmin', '超级管理员', 'ACTIVE', TRUE)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 6.3 初始化超级管理员凭证
-- ------------------------------------------------------------
INSERT INTO auth.auth_user_credential
    (user_id, tenant_id, username, password_hash, status, login_fail_count, created_at, updated_at)
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'superadmin',
        '$2a$12$eImiTXuWVxfM37uY4JANjQ.G0X35x5JMHpRVkC2RzXkQUKxpAzqIC',
        'ACTIVE',
        0,
        NOW(),
        NOW()
    )
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 6.4 初始化超级管理员角色
-- ------------------------------------------------------------
INSERT INTO system.sys_role (tenant_id, role_name, role_code, is_builtin, status, sort)
VALUES ('00000000-0000-0000-0000-000000000001', '超级管理员', 'SUPER_ADMIN', TRUE, 'ACTIVE', 0)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 6.5 绑定超级管理员用户与角色
-- ------------------------------------------------------------
INSERT INTO system.sys_user_role (user_id, role_id)
VALUES ('00000000-0000-0000-0000-000000000001', 1)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 6.6 初始化系统字典类型
-- ------------------------------------------------------------
INSERT INTO system.sys_dict_type (dict_type, dict_name, is_system, status) VALUES
    ('sys_user_status',   '用户状态',   TRUE, 'ACTIVE'),
    ('sys_login_status',  '登录状态',   TRUE, 'ACTIVE'),
    ('sys_yes_no',        '是否',       TRUE, 'ACTIVE'),
    ('sys_menu_type',     '菜单类型',   TRUE, 'ACTIVE'),
    ('sys_op_status',     '操作状态',   TRUE, 'ACTIVE')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 6.7 初始化字典数据
-- ------------------------------------------------------------
INSERT INTO system.sys_dict_data (dict_type, dict_label, dict_value, sort) VALUES
    ('sys_user_status',  '正常',    'ACTIVE',        1),
    ('sys_user_status',  '禁用',    'DISABLED',      2),
    ('sys_login_status', '登录成功',  'SUCCESS',       1),
    ('sys_login_status', '登录失败',  'FAILED',        2),
    ('sys_login_status', '主动登出',  'LOGOUT',        3),
    ('sys_login_status', '强制下线',  'FORCE_LOGOUT', 4),
    ('sys_yes_no',       '是',       '1',             1),
    ('sys_yes_no',       '否',       '0',             2),
    ('sys_menu_type',    '目录',     '0',             1),
    ('sys_menu_type',    '菜单',     '1',             2),
    ('sys_menu_type',    '按钮',     '2',             3),
    ('sys_op_status',    '成功',    'SUCCESS',        1),
    ('sys_op_status',    '失败',    'FAILED',         2)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 6.8 初始化基础菜单
-- ------------------------------------------------------------
-- 一级目录
INSERT INTO system.sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort) VALUES
    (100, 0, '系统管理', 0, '/system', NULL, 'Setting', 100),
    (200, 0, '云平台管理', 0, '/cloud', NULL, 'Cloud', 200),
    (300, 0, '监控告警', 0, '/monitor', NULL, 'Monitor', 300)
ON CONFLICT DO NOTHING;

-- 系统管理子菜单
INSERT INTO system.sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort) VALUES
    (101, 100, '用户管理', 1, '/system/user', 'system/user/index', 'User', 1),
    (102, 100, '角色管理', 1, '/system/role', 'system/role/index', 'Role', 2),
    (103, 100, '菜单管理', 1, '/system/menu', 'system/menu/index', 'Menu', 3),
    (104, 100, '部门管理', 1, '/system/dept', 'system/dept/index', 'Dept', 4),
    (105, 100, '租户管理', 1, '/system/tenant', 'system/tenant/index', 'Tenant', 5),
    (106, 100, '字典管理', 1, '/system/dict', 'system/dict/index', 'Dict', 6),
    (107, 100, '操作日志', 1, '/system/oplog', 'system/oplog/index', 'Log', 7),
    (108, 100, '登录日志', 1, '/system/loginlog', 'system/loginlog/index', 'LoginLog', 8)
ON CONFLICT DO NOTHING;

-- 云平台管理子菜单
INSERT INTO system.sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort) VALUES
    (201, 200, '集群配置', 1, '/cloud/cluster', 'cloud/cluster/index', 'Cluster', 1),
    (202, 200, '云主机', 1, '/cloud/instance', 'cloud/instance/index', 'Instance', 2),
    (203, 200, '网络管理', 1, '/cloud/network', 'cloud/network/index', 'Network', 3),
    (204, 200, '存储管理', 1, '/cloud/storage', 'cloud/storage/index', 'Storage', 4),
    (205, 200, 'K8s 管理', 1, '/cloud/kubernetes', 'cloud/kubernetes/index', 'Kubernetes', 5)
ON CONFLICT DO NOTHING;

-- 监控告警子菜单
INSERT INTO system.sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort) VALUES
    (301, 300, '指标查询', 1, '/monitor/metrics', 'monitor/metrics/index', 'Metrics', 1),
    (302, 300, 'Grafana', 1, '/monitor/grafana', 'monitor/grafana/index', 'Grafana', 2),
    (303, 300, '告警规则', 1, '/monitor/rules', 'monitor/rules/index', 'AlertRule', 3),
    (304, 300, '告警记录', 1, '/monitor/records', 'monitor/records/index', 'AlertRecord', 4),
    (305, 300, '静默规则', 1, '/monitor/silence', 'monitor/silence/index', 'Silence', 5),
    (306, 300, '通知渠道', 1, '/monitor/channels', 'monitor/channels/index', 'Channel', 6),
    (307, 300, '通知日志', 1, '/monitor/notify-log', 'monitor/notify-log/index', 'NotifyLog', 7)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 6.9 绑定超管角色与所有菜单
-- ------------------------------------------------------------
INSERT INTO system.sys_role_menu (role_id, menu_id)
SELECT 1, id FROM system.sys_menu
ON CONFLICT DO NOTHING;

-- ============================================================
-- 【七】运维视图（可选）
-- ============================================================

-- 近 7 天登录失败统计（auth 模块）
CREATE OR REPLACE VIEW auth.v_recent_login_failures AS
SELECT
    user_id,
    username,
    tenant_id,
    COUNT(*)                         AS fail_count,
    MAX(login_time)                  AS last_fail_time,
    COUNT(DISTINCT login_ip)         AS distinct_ip_count
FROM auth.auth_login_log
WHERE status = 'FAILED'
  AND created_at >= now() - INTERVAL '7 days'
GROUP BY user_id, username, tenant_id;

COMMENT ON VIEW auth.v_recent_login_failures IS '近 7 天登录失败统计，按用户聚合';

-- 当前被锁定账号（auth 模块）
CREATE OR REPLACE VIEW auth.v_locked_accounts AS
SELECT
    user_id,
    tenant_id,
    username,
    login_fail_count,
    lock_expire_time,
    CASE
        WHEN lock_expire_time IS NULL OR lock_expire_time > now() THEN '锁定中'
        ELSE '锁定已到期（待自动解锁）'
    END AS lock_state,
    created_at
FROM auth.auth_user_credential
WHERE status = 'LOCKED'
  AND deleted_at IS NULL;

COMMENT ON VIEW auth.v_locked_accounts IS '当前处于锁定状态的账号列表';

-- 最近 7 天各模块操作统计（system 模块）
CREATE OR REPLACE VIEW system.v_operation_stat_7d AS
SELECT
    module_name,
    operation_type,
    status,
    COUNT(*) AS cnt,
    AVG(cost_time) AS avg_cost_ms
FROM system.sys_operation_log
WHERE operate_time >= now() - INTERVAL '7 days'
GROUP BY module_name, operation_type, status
ORDER BY cnt DESC;

-- 各租户用户数量统计（system 模块）
CREATE OR REPLACE VIEW system.v_tenant_user_count AS
SELECT
    t.id              AS tenant_id,
    t.tenant_name,
    t.status,
    COUNT(u.id)       AS user_count
FROM system.sys_tenant t
LEFT JOIN system.sys_user u
    ON u.tenant_id = t.id AND u.deleted = 0
WHERE t.deleted = 0
GROUP BY t.id, t.tenant_name, t.status
ORDER BY user_count DESC;

-- ============================================================
-- END
-- ============================================================
-- 验证建表结果：
--   SELECT table_name FROM information_schema.tables
--   WHERE table_schema IN ('auth', 'system', 'cloud', 'monitor')
--   ORDER BY table_schema, table_name;
-- ============================================================
